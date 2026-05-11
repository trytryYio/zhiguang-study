package zhiguang.nauy.llm.rag.service;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;


/**
 * RAG 查询服务。
 *
 * <p>基于 VectorStore 语义检索 + ChatClient 流式生成，
 * 为知文提供知识问答能力。</p>
 */
@Service
public class RagQueryService {



    @Resource
    private VectorStore vectorStore;

    @Resource
    private RagIndexService ragIndexService;

    @Resource
    private ChatClient chatClient;
    @Value("${spring.ai.deepseek.chat.options.model:}")
    private String modelName;

    @Value("${spring.ai.deepseek.chat.options.temperature:deepseek-chat}")
    private Double temperature;



    /**
     * 用Elasticsearch搜索上下文。
     *
     * <p>基于 VectorStore 语义检索，返回与查询最相关的上下文片段。</p>
     *
     * @param postId 帖子 ID
     * @param query  查询内容
     * @param topK   返回的片段数量
     * @return 上下文片段列表
     */
    private List<String> searchContexts(String postId,String query,int topK){
        //1.宽召回 ()先多搜，再筛选。
        int max = Math.max(topK * 3, 20);

        //2.去ES语义搜索
        List<Document> doc = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(max).build());


        //Document {
        //    text: "这是实际的文本内容",
        //    metadata: {
        //        postId: "123456",
        //        chunkId: "123456_chunk_0",
        //        position: 0,
        //        contentSha256: "...",
        //        title: "Redis 基础"
        //    }
        //}
        //3.过滤掉不是这个帖子的
        List<String> list = new ArrayList<>();
        for (Document document : doc) {
            Object pid = document.getMetadata().get("postId");
            if(pid!=null && postId.equals(pid.toString())){
                //找到
                String text = document.getText();
                if (text != null&& text.length() > 0)
                {
                    list.add(text);
                    if (list.size() >= topK)break; //够了就停止了
                }

            }


        }
        return list;
    }

    //1.确保索引是新的

    /**
     * 流式生成答案。
     *
     * <p>基于 ChatClient 流式生成答案。</p>
     *
     * @param postId     帖子 ID
     * @param question   问题
     * @param topK       上下文片段数量
     * @param maxTokens  最大生成 tokens
     * @return 答案流
     */
    public Flux< String> StreamAnswerFlux(long postId, String question, int topK, int maxTokens) {
        //1.确保索引是新的
        ragIndexService.ensureIndexed(postId);

        //2.搜索上下文 宽搜索 后筛选
        List<String> contexts = searchContexts(String.valueOf(postId), question, Math.max(1, topK));

        //3.如果没搜索到，就返回友好信息
        if (contexts.isEmpty()){
            return Flux.just("抱歉，未能找到与该问题相关的内容。");
        }

        //4.拼接上下文字符串
        String context = String.join("\n\n---\n\n", contexts);

        //5.构造提示词
        String system = "你是中文知识助手。只能依据提供的知文上下文回答；无法确定的请说明不确定。";
        String user = "问题：" + question + "\n\n上下文如下：\n" + context + "\n\n请基于以上上下文作答。";

        //6.流式调用llm
        return chatClient.prompt()
            .system(system)
            .user( user)
            .options(DeepSeekChatOptions.builder()
                .model(modelName)
                .temperature(temperature)

                .maxTokens(maxTokens)
                .build())
            .stream() // 流式调用
            .content();
    }
}
