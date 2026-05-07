package zhiguang.nauy.knowpost.api.dto;

/**
 * 创建草稿响应DTO
 * <p>返回新建的帖子ID（字符串类型避免前端精度丢失）</p>
 *
 * @param id 新创建的草稿知文ID
 */
public record KnowPostDraftCreateResponse(String id) {

}