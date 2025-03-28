package searchengine.utils.index;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.UserAgents;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.morphology.LemmaIndexer;
import searchengine.utils.parse.LinkParser;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteIndexer extends RecursiveAction {
    private final ForkJoinPool pool;
    private final PageRepository repositoryPage;
    private final SiteRepository repositorySite;
    private final LemmaRepository repositoryLemma;
    private final IndexRepository repositoryIndex;
    private final String url;
    private final Site site;
    private final Set<String> allLinks;
    private final UserAgents userAgent;
    private static volatile boolean stopIndexing;

    public SiteIndexer(ForkJoinPool pool, SiteRepository repositorySite, PageRepository repositoryPage,
                       LemmaRepository repositoryLemma, IndexRepository repositoryIndex,
                       Site site, String url, Set<String> allLinks, UserAgents userAgent) {
        this.pool = pool;
        this.repositorySite = repositorySite;
        this.repositoryPage = repositoryPage;
        this.repositoryLemma = repositoryLemma;
        this.repositoryIndex = repositoryIndex;
        this.site = site;
        this.url = url.endsWith("/") ? url : url + "/";
        this.allLinks = allLinks;
        this.userAgent = userAgent;
        stopIndexing = false;
    }

    public static void stopIndexing() {
        stopIndexing = true;
        log.info("Indexing stop requested");
    }

    @Override
    protected void compute() {
        if (stopIndexing) {
            log.debug("Indexing stopped for URL: {}", url);
            return;
        }

        allLinks.add(url);
        LinkParser parser = new LinkParser(site, url, userAgent);
        try {
            parser.parse();
        } catch (Exception e) {
            exceptionOfParse(e);
            return;
        }

        Page page = savePage(parser);

        if (!stopIndexing) {
            site.setStatusTime(new Date());
            repositorySite.save(site);
        }

        try {
            LemmaIndexer lemmaIndexer = new LemmaIndexer(repositoryLemma, repositoryIndex, site, page);
            lemmaIndexer.indexing();
        } catch (Exception e) {
            log.error("Lemma indexing failed for page: {}", url, e);
            return;
        }

        List<SiteIndexer> taskList = new ArrayList<>();
        Set<String> allLinksOfPage = parser.getAllLinksOfPage();
        allLinksOfPage.forEach(link -> {
            if (!allLinks.contains(link) && !stopIndexing) {
                SiteIndexer task = new SiteIndexer(pool, repositorySite, repositoryPage, repositoryLemma,
                        repositoryIndex, site, link, allLinks, userAgent);
                task.fork();
                taskList.add(task);
            }
        });

        if (pool.getActiveThreadCount() == 1) {
            endIndexing();
        }
    }

    private Page savePage(LinkParser parser) {
        Page page = new Page();
        page.setSite(site);
        page.setPath(parser.getPath());
        page.setCode(parser.getCode());
        page.setContent(parser.getContent());
        repositoryPage.save(page);
        log.debug("Page saved: {}", parser.getPath());
        return page;
    }

    private void endIndexing() {
        String siteStatus = repositorySite.getStatusBySiteId(site.getId());
        if (!siteStatus.equals("FAILED")) {
            log.info("** SITE {} IS INDEXED ** {}", site.getUrl(),
                    LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
            site.setStatus(SiteStatus.INDEXED);
            site.setStatusTime(new Date());
            repositorySite.save(site);
        }
    }

    private void exceptionOfParse(Exception e) {
        String mainSiteUrl = site.getUrl().replace("www.", "");
        mainSiteUrl = mainSiteUrl.endsWith("/") ? mainSiteUrl : mainSiteUrl + "/";
        if (url.equals(mainSiteUrl)) {
            site.setStatus(SiteStatus.FAILED);
            site.setStatusTime(new Date());
            site.setLastError(e + ": " + e.getMessage());
            repositorySite.save(site);
            log.error("Main site parsing failed: {}", url, e);
        } else {
            log.warn("Link parsing failed: {}", url, e);
        }
    }
}