package zhiguang.nauy.search.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchResult {
    private     Long postId;

    private String title;

    private String snippet;
    private long likeCount;
    private long favCount;
}
