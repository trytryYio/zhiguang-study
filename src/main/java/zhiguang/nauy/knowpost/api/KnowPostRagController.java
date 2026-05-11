package zhiguang.nauy.knowpost.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.llm.rag.service.RagIndexService;
import zhiguang.nauy.llm.rag.service.RagQueryService;

@Slf4j
@RestController
@Tag(name = "知文RAG接口", description = "基于RAG的知识问答")
@RequestMapping("/api/v1/knowposts")
public class KnowPostRagController {

    private final RagQueryService ragQueryService;
    private final RagIndexService ragIndexService;

    public KnowPostRagController(RagQueryService ragQueryService, RagIndexService ragIndexService) {
        this.ragQueryService = ragQueryService;
        this.ragIndexService = ragIndexService;
    }

    @Operation(summary = "知文知识问答（SSE流式）")
    @GetMapping(value = "/{id}/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamQA(
        @PathVariable("id") Long id,
        @RequestParam("question") String question,
        @RequestParam(value = "topK", defaultValue = "5") int topK,
        @RequestParam(value = "maxTokens", defaultValue = "1024") int maxTokens) {
        log.info("streamQA id={} question={} topK={} maxTokens={}", id, question, topK, maxTokens);
        return
            ragQueryService.StreamAnswerFlux(id, question, topK, maxTokens);
    }

    @Operation(summary = "手动触发RAG重建索引")
    @PostMapping("/{id}/rag/reindex")
    public BaseResponse<Integer> reindex(@PathVariable("id") Long postId) {
        log.info("reindex id={}", postId);
        return ResultUtils.success( ragIndexService.ensureIndexed(postId));
    }
}
