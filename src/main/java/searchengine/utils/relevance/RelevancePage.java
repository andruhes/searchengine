package searchengine.utils.relevance;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import searchengine.model.Page;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class RelevancePage {
    private final Page page;
    private final Map<String, Float> rankWords = new HashMap<>();
    private Float relevance;

    public float getAbsRelevance() {
        return rankWords.values().stream().reduce(0.0f, Float::sum);
    }

    public void putRankWord(String word, Float rank) {
        rankWords.put(word, rank);
    }

    public void setRelevance(Float rel) {
        relevance = rel;
    }
}