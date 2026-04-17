package zhiguang.nauy.common;

import lombok.Data;

@Data
public class PageRequest {
    /**
     * 当前页号，默认值为 1
     */
    private int current = 1;

    /**
     * 页面大小，默认值为 10
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
