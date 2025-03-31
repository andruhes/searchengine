package searchengine.utils.morphology;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.Map;

@RequiredArgsConstructor
public class LemmaIndexer {
    private final LemmaRepository repositoryLemma;
    private final IndexRepository repositoryIndex;
    private final Site site;
    private final Page page;
    private static volatile boolean stopped = false;

    public static void stopIndexing() {
        stopped = true;
    }

    public static boolean isStopped() {
        return stopped;
    }

    public void indexing() {
        if (stopped) {
            return;
        }

        Map<String, Integer> lemmas = getLemmasOfPage();
        lemmas.forEach(this::saveLemmaAndIndex);
    }

    private Map<String, Integer> getLemmasOfPage() {
        LemmaFinder finder = new LemmaFinder();
        String content = page.getContent();

        Document document = Jsoup.parse(content);
        String titleTagText = document.title();
        String bodyTagText = document.body().text();
        String text = titleTagText + " " + bodyTagText;

        return finder.collectLemmas(text);
    }

    private void saveLemmaAndIndex(String lemma, Integer count) {
        synchronized (site) {
            if (stopped) return;

            Lemma lemmaDB = repositoryLemma.findByLemmaAndSite(lemma, site);
            if (lemmaDB == null) {
                Lemma lemmaNew = new Lemma();
                lemmaNew.setSite(site);
                lemmaNew.setLemma(lemma);
                lemmaNew.setFrequency(1);
                lemmaDB = repositoryLemma.save(lemmaNew);
            } else {
                lemmaDB.setFrequency(lemmaDB.getFrequency() + 1);
                repositoryLemma.save(lemmaDB);
            }

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemmaDB);
            index.setRank(count.floatValue());
            repositoryIndex.save(index);
        }
    }
}