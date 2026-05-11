package zhiguang.nauy.llm.service;

/**
 * 知文摘要生成服务。
 */
public interface KnowPostDescriptionService {



    /**
     * 根据帖子内容生成 ≤50 字的中文摘要。
     *
     * @param content 帖子正文内容
     * @return 摘要文本
     */
    String generateDescription(String content);
}
