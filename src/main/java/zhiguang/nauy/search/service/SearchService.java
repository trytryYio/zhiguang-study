package zhiguang.nauy.search.service;

import zhiguang.nauy.search.service.dto.SearchResult;

import java.util.List;

public interface SearchService {
    /**
     * 执行全文搜索
     * @param query 搜索关键词
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 搜索结果列表，失败时返回空列表
     */
    List<SearchResult> search(String query, int page, int size);

    /**
     * 获取补全建议
     * @param prefix 用户输入的前缀
     * @param size 返回数量
     * @return 补全建议列表，失败时返回空列表
     */
    List<String> suggest(String prefix, int size);

}
