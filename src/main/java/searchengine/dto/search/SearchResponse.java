package searchengine.dto.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private boolean result = true;
    private Integer count;
    private List<DataSearchItem> data;
    private String error;

    public SearchResponse(String error) {
        this.result = false;
        this.error = error;
    }

    public SearchResponse(Integer count, List<DataSearchItem> data) {
        this.count = count;
        this.data = data;
        this.result = true;
    }
}