package zhiguang.nauy.llm.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * 知文摘要生成服务。
 */
@Service
public class KnowPostDescriptionServiceImpl implements KnowPostDescriptionService{

    @Resource
    private ChatClient chatClient;

    /**
     * 根据帖子内容生成 ≤50 字的中文摘要。
     *
     * @param content 帖子正文内容
     * @return 摘要文本
     */
    @Override
    public String generateDescription(String content) {
        String result =chatClient.prompt()
            .system("你是中文文案编辑，负责为知识帖文生成简洁的摘要。需要遵循 NFKC 规范化（全角→半角，组合字符→预组合）")
            .user("请为以下内容生成一个不超过50个字的中文摘要：\n\n" + content)
            .options(DeepSeekChatOptions.builder()
                .model("deepseek-chat")
                .temperature(0.8)
                .maxTokens(120)
                .build())
            .call()
            .content();
        return postProcess(result);
    }
    /**
     * 后处理：规范化、清理、截断。
     */
    private String postProcess(String text) {
        if (text == null) {
            return "";
        }
        text = Normalizer.normalize(text, Normalizer.Form.NFKC);
        // 折叠连续空白
        text = text.replaceAll("\\s+", " ").trim();
        // 移除常见引号和标点
        text = text.replaceAll("[\"\"「」『』【】《》]", "");
        // 截断至 50 字（按 code point 计数）
        if (text.codePointCount(0, text.length()) > 50){
            //String text = "Hello😀World"; // 😀是emoji，占2个char
            // text.length() = 12 (char数量)
            // 但实际只有11个Unicode字符

            // 错误方式：substring(0, 6) 可能截断emoji
            // 正确方式：substring(0, text.offsetByCodePoints(0, 6))

            text = text.substring(0, text.offsetByCodePoints(0, 50));
        }
        return text;

    }
}
