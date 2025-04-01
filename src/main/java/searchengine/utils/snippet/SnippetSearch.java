package searchengine.utils.snippet;

import searchengine.utils.morphology.LemmaFinder;
import java.util.*;
import java.util.regex.Pattern;

public class SnippetSearch {
    private static final int MAX_SNIPPETS = 3;
    private static final int MAX_SNIPPET_LENGTH = 160;
    private static final int WORDS_AROUND = 5;

    public static String find(String text, Set<String> lemmas) {
        if (text == null || text.isEmpty() || lemmas == null || lemmas.isEmpty()) {
            return "";
        }

        LemmaFinder finder = new LemmaFinder();
        String cleanText = text.replaceAll("\\s+", " ");
        String[] sentences = cleanText.split("(?<=[.!?])\\s+");
        List<String> snippets = new ArrayList<>();

        for (String sentence : sentences) {
            if (snippets.size() >= MAX_SNIPPETS) {
                break;
            }

            String highlighted = highlightMatches(sentence, lemmas, finder);
            if (!highlighted.equals(sentence)) {
                snippets.add(truncateSnippet(highlighted));
            }
        }

        return snippets.isEmpty()
                ? truncateSnippet(highlightQueryInText(cleanText, lemmas, finder))
                : String.join("<br/><br/>", snippets);
    }

    private static String highlightMatches(String text, Set<String> lemmas, LemmaFinder finder) {
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++) {
            String cleanWord = words[i].replaceAll("[^\\p{L}]", "").toLowerCase();
            if (!cleanWord.isEmpty()) {
                List<String> wordLemmas = finder.getNormalForms(cleanWord);
                for (String lemma : lemmas) {
                    if (wordLemmas.contains(lemma)) {
                        // Выделяем полное слово, а не только лемму
                        words[i] = words[i].replaceAll(
                                "(?i)(" + Pattern.quote(cleanWord) + ")",
                                "<b>$1</b>"
                        );
                        break;
                    }
                }
            }
        }
        return String.join(" ", words);
    }

    private static String highlightQueryInText(String text, Set<String> lemmas, LemmaFinder finder) {
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++) {
            String cleanWord = words[i].replaceAll("[^\\p{L}]", "").toLowerCase();
            if (!cleanWord.isEmpty() && lemmas.contains(cleanWord)) {
                words[i] = words[i].replaceAll(
                        "(?i)(" + Pattern.quote(cleanWord) + ")",
                        "<b>$1</b>"
                );
            }
        }
        return String.join(" ", words);
    }

    private static String truncateSnippet(String text) {
        return text.length() <= MAX_SNIPPET_LENGTH
                ? text
                : text.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }
}