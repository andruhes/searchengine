package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.DataSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.morphology.LemmaFinder;
import searchengine.utils.relevance.RelevancePage;
import searchengine.utils.snippet.SnippetSearch;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository repositorySite;
    private final LemmaRepository repositoryLemma;
    private final IndexRepository repositoryIndex;
    private static String error = "";
    private static String lastQuery;
    private static List<DataSearchItem> data;

    @Override
    public SearchResponse getSearch(String query, String siteUrl, Integer offset, Integer limit) {
        try {
            if (query == null || query.trim().isEmpty()) {
                error = "Запрос не введен";
                return errorSearch(error);
            }

            query = query.trim().toLowerCase();
            if (query.equals(lastQuery) && data != null) {
                return buildResponse(offset != null ? offset : 0,
                        limit != null ? limit : 20);
            }

            log.info("** START SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            log.info(" - QUERY: {}", query);

            LemmaFinder finder = new LemmaFinder();
            Map<String, Integer> queryLemmasMap = finder.collectLemmas(query);
            Set<String> queryLemmas = queryLemmasMap.keySet();

            if (queryLemmas.isEmpty()) {
                error = "Не удалось выделить леммы из запроса";
                return errorSearch(error);
            }

            log.debug("Found lemmas: {}", queryLemmas);
            List<Index> indexes = foundIndexes(queryLemmas, siteUrl);

            if (!error.isEmpty()) {
                return errorSearch(error);
            }

            lastQuery = query;
            data = getDataList(indexes, queryLemmas);
            endSearchPrint(data.size());

            return buildResponse(offset != null ? offset : 0,
                    limit != null ? limit : 20);
        } catch (Exception e) {
            log.error("Search error", e);
            return new SearchResponse("Ошибка при выполнении поиска: " + e.getMessage());
        }
    }

    private void endSearchPrint(int countPages) {
        log.info(" RESULT SEARCH: found {} pages", countPages);
        log.info("** END SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
    }

    private SearchResponse errorSearch(String error) {
        log.error(" - ERROR: {}", error);
        log.info("** END SEARCH OF QUERY ** {}", LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
        SearchServiceImpl.error = "";
        return new SearchResponse(error);
    }

    private List<Index> foundIndexes(Set<String> queryLemmas, String siteUrl) {
        List<Index> indexList;
        if (siteUrl == null) {
            log.info(" - SITE: ALL SITES");
            indexList = searchByAll(queryLemmas);
        } else {
            log.info(" - SITE: {}", siteUrl);
            Site site = repositorySite.findSiteByUrl(siteUrl);
            if (site == null || !site.getStatus().equals(SiteStatus.INDEXED)) {
                error = "Выбранный сайт ещё не проиндексирован";
                return new ArrayList<>();
            }
            indexList = searchBySite(queryLemmas, site);
        }
        if (indexList.isEmpty() && error.isEmpty()) {
            error = "Ничего не найдено";
        }
        return indexList;
    }

    private List<Index> searchByAll(Set<String> queryLemmas) {
        List<Index> indexList = new ArrayList<>();
        List<Site> allSites = (List<Site>) repositorySite.findAll();
        for (Site site : allSites) {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                error = "Дождитесь окончания индексации всех сайтов";
                return new ArrayList<>();
            }
            indexList.addAll(searchBySite(queryLemmas, site));
        }
        return indexList;
    }

    private List<Index> searchBySite(Set<String> queryLemmas, Site site) {
        List<Lemma> lemmas = repositoryLemma.selectLemmasBySite(queryLemmas, site);
        if (queryLemmas.size() != lemmas.size()) {
            return new ArrayList<>();
        }

        if (lemmas.size() == 1) {
            return repositoryIndex.findByLemma(lemmas.get(0));
        }

        List<Page> allPages = repositoryIndex.findPagesByLemma(lemmas.get(0));
        for (int i = 1; i < lemmas.size(); i++) {
            if (allPages.isEmpty()) {
                return new ArrayList<>();
            }
            List<Page> pagesOfLemma = repositoryIndex.findPagesByLemma(lemmas.get(i));
            allPages.removeIf(page -> !pagesOfLemma.contains(page));
        }
        return repositoryIndex.findByLemmasAndPages(lemmas, allPages);
    }

    private List<DataSearchItem> getDataList(List<Index> indexes, Set<String> queryLemmas) {
        List<RelevancePage> relevancePages = getRelevantList(indexes);
        List<DataSearchItem> result = new ArrayList<>();

        for (RelevancePage page : relevancePages) {
            DataSearchItem item = new DataSearchItem();
            item.setSite(page.getPage().getSite().getUrl());
            item.setSiteName(page.getPage().getSite().getName());
            item.setUri(page.getPage().getPath());

            String title = Jsoup.parse(page.getPage().getContent()).title();
            if (title.length() > 50) {
                title = title.substring(0, 50).concat("...");
            }
            item.setTitle(title);
            item.setRelevance(page.getRelevance());

            String content = Jsoup.parse(page.getPage().getContent()).body().text();
            item.setSnippet(SnippetSearch.find(content, queryLemmas));

            result.add(item);
        }

        return result.stream()
                .sorted(Comparator.comparingDouble(DataSearchItem::getRelevance).reversed())
                .collect(Collectors.toList());
    }

    private List<RelevancePage> getRelevantList(List<Index> indexes) {
        List<RelevancePage> pageSet = new ArrayList<>();

        for (Index index : indexes) {
            RelevancePage existingPage = pageSet.stream()
                    .filter(temp -> temp.getPage().equals(index.getPage()))
                    .findFirst()
                    .orElse(null);
            if (existingPage != null) {
                existingPage.putRankWord(index.getLemma().getLemma(), index.getRank());
                continue;
            }

            RelevancePage page = new RelevancePage(index.getPage());
            page.putRankWord(index.getLemma().getLemma(), index.getRank());
            pageSet.add(page);
        }

        float maxRelevance = 0.0f;

        for (RelevancePage page : pageSet) {
            float absRelevance = page.getAbsRelevance();
            if (absRelevance > maxRelevance) {
                maxRelevance = absRelevance;
            }
        }

        for (RelevancePage page : pageSet) {
            page.setRelevance(page.getAbsRelevance() / maxRelevance);
        }

        pageSet.sort(Comparator.comparingDouble(RelevancePage::getRelevance).reversed());
        return pageSet;
    }

    private SearchResponse buildResponse(Integer offset, Integer limit) {
        if (data == null || data.isEmpty()) {
            return new SearchResponse(0, Collections.emptyList());
        }

        offset = Math.max(offset, 0);
        limit = Math.max(limit, 0);

        int fromIndex = Math.min(offset, data.size());
        int toIndex = Math.min(offset + limit, data.size());

        if (fromIndex >= toIndex) {
            return new SearchResponse(data.size(), Collections.emptyList());
        }

        return new SearchResponse(data.size(), data.subList(fromIndex, toIndex));
    }
}