package searchengine.utils.morphology;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
public class LemmaFinder {
    private final LuceneMorphology luceneMorphologyRu;
    private final LuceneMorphology luceneMorphologyEn;
    private static final String[] particlesNames = {"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ", "ARTICLE", "CONJ", "PREP"};

    public LemmaFinder() {
        try {
            this.luceneMorphologyRu = new RussianLuceneMorphology();
            this.luceneMorphologyEn = new EnglishLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize morphology analyzers", e);
        }
    }

    public Map<String, Integer> collectLemmas(String text) {
        String[] words = arrayContainsWords(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        for (String word : words) {
            if (word.isBlank()) continue;

            List<String> wordBaseForms = getMorphInfo(word);
            if (wordBaseForms.isEmpty() || anyWordBaseBelongToParticle(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            lemmas.merge(normalWord, 1, Integer::sum);
        }
        return lemmas;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::isParticle);
    }

    private boolean isParticle(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.contains(property)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getMorphInfo(String word) {
        if (word.matches("[а-я]+")) {
            return luceneMorphologyRu.getMorphInfo(word);
        }
        else if (word.matches("[a-z]+")) {
            return luceneMorphologyEn.getMorphInfo(word);
        }
        return Collections.emptyList();
    }

    public List<String> getNormalForms(String word) {
        if (word.matches("[а-я]+")) {
            return luceneMorphologyRu.getNormalForms(word);
        }
        else if (word.matches("[a-z]+")) {
            return luceneMorphologyEn.getNormalForms(word);
        }
        return Collections.emptyList();
    }

    private String[] arrayContainsWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-яa-z\\s])", " ")
                .trim()
                .split("\\s+");
    }
}