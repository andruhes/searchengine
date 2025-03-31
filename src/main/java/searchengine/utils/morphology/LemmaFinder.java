package searchengine.utils.morphology;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LemmaFinder {
    private static final Set<String> PARTICLES = Set.of(
            "межд", "предл", "союз", "част", "article", "conj", "prep"
    );

    private final SnowballStemmer russianStemmer;
    private final PorterStemmer englishStemmer;
    private final SimpleTokenizer tokenizer;
    private final Map<String, String> lemmaCache = new ConcurrentHashMap<>();

    public LemmaFinder() {
        this.russianStemmer = new SnowballStemmer(SnowballStemmer.ALGORITHM.RUSSIAN);
        this.englishStemmer = new PorterStemmer();
        this.tokenizer = SimpleTokenizer.INSTANCE;
    }

    public Map<String, Integer> collectLemmas(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyMap();
        }

        String[] tokens = tokenizeText(text);
        Map<String, Integer> lemmas = new HashMap<>();

        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }

            String stem = getCachedStem(token);
            if (stem == null || isParticle(stem)) {
                continue;
            }

            lemmas.merge(stem, 1, Integer::sum);
        }

        return lemmas;
    }

    public List<String> getNormalForms(String word) {
        if (word == null || word.isEmpty()) {
            return Collections.emptyList();
        }

        String stem = getCachedStem(word.toLowerCase());
        return stem != null ? Collections.singletonList(stem) : Collections.emptyList();
    }

    private String[] tokenizeText(String text) {
        return tokenizer.tokenize(text.toLowerCase()
                .replaceAll("[^a-zа-яё\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim());
    }

    private String getCachedStem(String word) {
        return lemmaCache.computeIfAbsent(word, this::getStem);
    }

    private String getStem(String word) {
        try {
            if (word.matches("[а-яё]+")) {
                return russianStemmer.stem(word).toString();
            } else if (word.matches("[a-z]+")) {
                return englishStemmer.stem(word);
            }
        } catch (Exception e) {
            log.warn("Stemming error for word: {}", word, e);
        }
        return null;
    }

    private boolean isParticle(String stem) {
        return PARTICLES.contains(stem);
    }
}