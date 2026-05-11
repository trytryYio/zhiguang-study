package zhiguang.nauy.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring AI 提供了 `ChatClient`
 * 抽象来统一调用各种 LLM 模型（OpenAI、DeepSeek、Ollama 等）。
 * 我们需要配置一个 `ChatClient` Bean，使用 DeepSeek 模型。
 */
@Configuration
public class LlmConfig {
    /**
     * Spring AI 配置类。
     * 创建 ChatClient Bean，使用 DeepSeek 模型。
     */
    @Bean
    public ChatClient chatClient(@Qualifier("deepSeekChatModel") DeepSeekChatModel chatModel){
        return ChatClient.create(chatModel);
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
