package searchengine.utils.snippet;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import searchengine.utils.morphology.LemmaFinder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
public class SnippetSearch {
    private static final int LENGTH_SNIPPET = 53;
    private static final int NUMBER_SNIPPETS = 3;

    public static String find(String text, Set<String> lemmas) {
        Set<String> requiredLemmas = new HashSet<>(lemmas);
        List<String> snippets = new ArrayList<>();
        LemmaFinder finder = new LemmaFinder();

        for (String word : text.split("([^а-яА-ЯA-Za-z])+")) {
            if (snippets.size() >= NUMBER_SNIPPETS) break;
            
            List<String> normalFormsWord = finder.getNormalForms(word.toLowerCase());
            if (normalFormsWord.isEmpty()) continue;

            for (String lemma : requiredLemmas) {
                if (normalFormsWord.contains(lemma)) {
                    requiredLemmas.remove(lemma);
                    snippets.add(generateSnippet(text, word));
                    break;
                }
            }
        }
        return String.join("<br />", snippets);
    }

    private static String generateSnippet(String text, String word) {
        int start = text.indexOf(word);
        int end = start + word.length();
        int remainingLength = LENGTH_SNIPPET - word.length();

        end = adjustEndPosition(text, end, remainingLength / 2);
        start = adjustStartPosition(text, start, remainingLength - (end - start - word.length()));

        return text.substring(start, end).replace(word, "<b>" + word + "</b>");
    }

    private static int adjustEndPosition(String text, int end, int maxExtension) {
        // ... implementation ...
    }

    private static int adjustStartPosition(String text, int start, int maxExtension) {
        // ... implementation ...
    }
}