package zhiguang.nauy.llm.rag.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.knowpost.domain.KnowPosts;
import zhiguang.nauy.knowpost.mapper.KnowPostsMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 索引服务。
 *
 * <p>负责将知文内容分块、向量化并存储到 ES VectorStore，
 * 支持指纹去重和增量更新。</p>
 */
@Service
@Slf4j
public class RagIndexService {
    /**
     * RestTemplate Bean。用来 HTTP 拉取 OSS 上的 Markdown 正文
     */
    @Resource
    private RestTemplate restTemplate;

    @Resource
    private KnowPostsMapper knowPostsMapper;
    /**
     * 向量存储 Bean。
     */
    @Resource
    private VectorStore vectorStore;

    /**
     * 确保向量索引已建立。
     *
     * <p>如果索引不存在，则创建索引并初始化索引。
     * 如果索引已存在，则检查索引是否需要更新。
     * 如果索引需要更新，则更新索引。</p>
     *
     * @param postId 帖子 ID
     */
    public int ensureIndexed(long postId) {
        return reindexSinglePost(postId);
    }

    /**
     * 重建单个帖子的 RAG 索引。
     *
     * @param postId 帖子 ID
     * @return 索引的 chunk 数量
     */
    private int reindexSinglePost(long postId) {
        //1.查帖子 ->存不存在 有没有发布
        KnowPosts knowPosts = knowPostsMapper.selectById(postId);
        if (knowPosts == null || !"published".equals(knowPosts.getStatus())) {
            log.warn("Post not found or not published: {}", postId);
            return 0;
        }

        //2.oss 获取正文  ->有没有 contenturl
        ThrowUtils.throwIf(knowPosts.getContentUrl() == null || knowPosts.getContentUrl().isBlank(), ErrorCode.NOT_FOUND);
        String contentUrl = knowPosts.getContentUrl();

        // 2.1 校验 URL 是否指向文本文件（不支持图片等二进制内容）
        if (contentUrl.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp|svg)(\\?.*)?$")) {
            log.warn("Post {} contentUrl 指向图片文件，不是文本内容，跳过索引: {}", postId, contentUrl);
            return 0;
        }

        String content = restTemplate.getForObject(contentUrl, String.class);

        // 2.2 校验拉取的内容是否为纯文本（排除二进制数据）
        if (content == null || content.isBlank()) {
            log.warn("Post {} content is empty", postId);
            return 0;
        }
        if (!isPlainText(content)) {
            log.warn("Post {} content is not plain text (likely binary), skip indexing", postId);
            return 0;
        }

        //3.指纹对比-> 看内容有没有变化 对比不上->新帖子有变化
        //3.1 计算指纹
        String contentSha256 = sha256(content);
        // 3.2 去 ES 查这个 postId 有没有已索引的文档
        //查Es 里面有没有这个
        List<Document> existing = vectorStore.similaritySearch(SearchRequest.builder().
            query("match")
            .topK(1)
            .filterExpression("metadata.postId == '" + postId + "'")
            .build()
        );
        //3.3对比 指纹
        if (!existing.isEmpty()){
            //有
            String fingerprint = existing.get(0).getMetadata().get("contentSha256").toString();
            if (contentSha256.equals(fingerprint)){
                log.debug("帖子 {} 内容未变更，跳过重建", postId);
                return 0;
            }
        }


        //4.切片 按标题分段+滑动窗口
        List<String> chunks = chunkMarkdown(content);
        if (chunks.isEmpty()) {
            return 0;
        }

        //5.向量化 写入向量库 (删旧写新 )
        //5.1 删除旧切片(幂等)
        vectorStore.delete(List.of("metadata.postId == '" + postId + "'"));
        //5.2 构建Document 列表
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("postId", String.valueOf(postId));
            metadata.put("chunkId", postId + "_chunk_" + i);
            metadata.put("position", i);
            metadata.put("contentSha256", contentSha256);
            metadata.put("contentUrl", knowPosts.getContentUrl());
            metadata.put("title", knowPosts.getTitle());
            documents.add(new Document(chunks.get(i), metadata));}

        //5.3 写入向量库
        //5.3 分批写入向量库（阿里云 Embedding API 限制每批最多 10 个）
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            log.debug("Indexed batch {}/{} for post {}", end, documents.size(), postId);
        }
//        vectorStore.add(documents);

        return documents.size();
    }
    /**
     * 将 Markdown 内容分块。
     *
     * <p>将原始 Markdown 内容进行分块，
     * 根据标题和内容进行分块，并返回分块后的列表。</p>
     *
     * @param markdown 待分块的 Markdown 内容
     * @return 分块后的列表
     */
    private List<String> chunkMarkdown(String markdown) {
        //1.第一轮按标题分段
        //
        //     遇到 # 开头就收束上一段：
        //     段落1: "# Redis 基础\nRedis 是一个开源的...（500字）"
        //     段落2: "# Redis 高级用法\n分布式锁、集群、哨兵...（1200字）"
        //     段落3: "# MySQL 索引\nB+树、覆盖索引...（300字）"
        List<String> sections = new ArrayList<>();
        String[] lines = markdown.split("\n");
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : lines){
            //按行搜索

            //   遇到新标题，且之前已经累积了内容
            if (line.startsWith("#")&&!stringBuilder.isEmpty()){
                // 把上一段保存起来
                sections.add(stringBuilder.toString());
                // 清空，准备记录新段落
                stringBuilder = new StringBuilder();
            }
            // 不断累积当前行的内容
            stringBuilder.append(line).append("\n");
        }
        //确保最后一段不被遗漏。
        if (!stringBuilder.isEmpty()){
            sections.add(stringBuilder.toString());
        }

        //2.第二轮 滑动窗口
        //段落1（500字）→ ≤ 800 → 直接保留 ✅
        //段落2（1200字）→ > 800 → 需要切片
        int CHUNK_SIZE = 800;
        int CHUNK_OVERLAPl=100;
        List<String> chunks = new ArrayList<>();
        //对每一个分段的内容进行滑动窗口切片
        for (String section : sections) {
            if (section.length()<= CHUNK_SIZE){
                // 短段直接保留
                chunks.add(section.trim());
            }else {
                //长段：滑动窗口切片
                int start = 0;
                while (start <section.length()){
                    // 计算当前窗口的结束位置
                    //：理想情况下，从 start 位置开始切 800 个字符    | 段落的实际总长度
                    // 防止数组越界，确保不会超出段落总长度
                    int end =Math.min(start + CHUNK_SIZE, section.length());

                    //从 start 到 end 提取子字符串
                    chunks.add(section.substring(start, end).trim());

                    //移动起始位置到下一个切片起点
                    //第1片: [0 ............... 799]
                    //            ↓ 重叠100字
                    //第2片:    [700 ............... 1499]
                    //移动到700的位置

                    start +=CHUNK_SIZE-CHUNK_OVERLAPl;
                }
            }
        }



        return chunks;
    }

    /**
     * 计算字符串的 SHA-256 哈希值。
     *
     * <p>将输入内容转换为 UTF-8 编码的字节数组，
     * 通过 SHA-256 算法生成 64 位十六进制字符串指纹，
     * 用于内容变更检测和去重判断。</p>
     *
     * @param content 待计算的原始文本内容
     * @return 64 位十六进制格式的 SHA-256 哈希值
     */
    /**
     * 判断字符串是否为纯文本（排除二进制数据）。
     * 检查前 512 个字符中是否包含大量不可打印字符。
     */
    private boolean isPlainText(String text) {
        if (text == null || text.isEmpty()) return false;
        int checkLen = Math.min(text.length(), 512);
        int nonPrintable = 0;
        for (int i = 0; i < checkLen; i++) {
            char c = text.charAt(i);
            // 不可打印字符（排除常见的空白符 \t \n \r）
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                nonPrintable++;
            }
        }
        // 如果超过 10% 是不可打印字符，认为是二进制数据
        return (double) nonPrintable / checkLen < 0.1;
    }

    private String sha256(String content) {
        try {
            //创建 SHA-256 消息摘要器实例，用于生成哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //将输入字符串转为 UTF-8 字节数组，并计算其 SHA-256 哈希（32字节）
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
//                遍历32个字节的哈希值，每个字节格式化为2位十六进制（如 0a、ff），拼接成64位字符串
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }


}
