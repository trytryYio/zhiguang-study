package zhiguang.nauy.search.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.search.service.SearchService;
import zhiguang.nauy.search.service.dto.SearchResult;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "搜索接口", description = "全文搜索与联想建议")
public class SearchController {
    @Resource
    private SearchService searchService;
    /**
     * 全文搜索
     *
     * @param query 搜索关键词
     * @param page  页码（从1开始）
     * @param size  每页数量
     * @return 搜索结果列表
     */
    @GetMapping
    @Operation(summary = "全文搜索")
    public BaseResponse<List<SearchResult>> search  ( @RequestParam("q") String query,
    @RequestParam(value = "page", defaultValue = "1") int page,
    @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResultUtils.success( searchService.search(query, page, size));
    }
    /**
     * 联想建议
     *
     * @param prefix 用户输入的前缀
     * @param size   联想数量
     * @return 联想建议列表
     */

    @Operation(summary = "联想建议")
    @GetMapping("/suggest")
    public List<String> suggest(
        @RequestParam("q") String prefix,
        @RequestParam(value = "size", defaultValue = "10") int size) {
        return searchService.suggest(prefix, size);
    }
}
