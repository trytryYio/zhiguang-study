package zhiguang.nauy.knowpost.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.exception.ErrorCode;
import zhiguang.nauy.exception.ThrowUtils;
import zhiguang.nauy.knowpost.api.dto.*;
import zhiguang.nauy.knowpost.service.KnowPostFeedService;
import zhiguang.nauy.knowpost.service.KnowPostsService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowposts")
@Validated
@RequiredArgsConstructor
public class KnowPostController {

    private final KnowPostsService service;
    private final KnowPostFeedService feedService;
    private final JwtService jwtService;

    /**
     * 创建草稿，返回新 ID。默认类型为 image_text。
     */
    @PostMapping("/drafts")
    public BaseResponse<KnowPostDraftCreateResponse> createDraft(@AuthenticationPrincipal Jwt jwt) {
//        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);
//        1.调用service方法
        long draft = service.createDraft(userId);
        return ResultUtils.success(new KnowPostDraftCreateResponse(String.valueOf(draft)));
    }

    /**
     * 确认内容上传完成
     * <p>在用户上传内容到OSS后，记录文件的元数据信息</p>
     * <p>业务流程：</p>
     * <ul>
     *   <li>1. 从JWT中提取当前用户ID</li>
     *   <li>2. 调用Service确认内容</li>
     *   <li>3. 返回204 No Content</li>
     * </ul>
     *
     * @param id      知文ID
     * @param request 确认内容请求（包含objectKey、etag、size、sha256）
     * @param jwt     JWT令牌（自动注入）
     * @return 204 No Content
     */
    @PostMapping("/{id}/content/confirm")

    public BaseResponse<Void> confirmContent(@PathVariable("id")long id,
                                             @RequestBody @Valid KnowPostContentConfirmRequest request
            , @AuthenticationPrincipal Jwt jwt) {
//        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);

//      查询知文id 获取文件信息
        String objectKey = request.getObjectKey();
        String etag = request.getEtag();
        Long size = request.getSize();
        String sha256 = request.getSha256();

//        1.调用service方法 确认内容
        service.confirmContent(userId, id, objectKey, etag, size, sha256);
        return ResultUtils.success(null);
    }

    /**
     * 更新元数据（标题、标签、可见性、置顶、图片列表等）。
     */
    @PatchMapping("/{id}")
    public BaseResponse patchMetadata(@PathVariable("id") long id,
                                              @Valid @RequestBody KnowPostPatchRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        //        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);
        String title = request.title();
        Long tagId = request.tagId();
        List<String> tags = request.tags();
        List<String> imgUrls = request.imgUrls();
        String visible = request.visible();
        Boolean isTop = request.isTop();
        String description = request.description();

        service.updateMetadata(userId, id, title, tagId, tags, imgUrls, visible, isTop, description);
        return ResultUtils.success();
    }

    /**
     * 发布帖子（状态置为 published）。
     */
    @PostMapping("/{id}/publish")
    public BaseResponse  publish(@PathVariable("id") long id,
                                        @AuthenticationPrincipal Jwt jwt) {
//        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);
        service.publish(userId, id);
        return ResultUtils.success();
    }

    /**
     * 设置置顶状态。
     */
    @PatchMapping("/{id}/top")
    public BaseResponse<Void> patchTop(@PathVariable("id") long id,
                                         @Valid @RequestBody KnowPostTopPatchRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        //        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);
        service.updateTop(userId, id, request.isTop());


        return ResultUtils.success();
    }

    /**
     * 设置可见性（权限）。
     */
    @PatchMapping("/{id}/visibility")
    public BaseResponse<Void> patchVisibility(@PathVariable("id") long id,
                                                @Valid @RequestBody KnowPostVisibilityPatchRequest request,
                                                @AuthenticationPrincipal Jwt jwt) {
//        0.通过jwt 获取用户ID
        long userId = jwtService.extractUserId(jwt);
        service.updateVisibility(userId, id, request.visible());
        return ResultUtils.success();
    }

    /**
     * 删除知文（软删除）。
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable("id") long id,
                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        service.delete(userId, id);
        return ResultUtils.success();
    }

    /**
     * 首页 Feed（公开、已发布）分页查询；默认每页 20，最大 50。
     */
    @GetMapping("/feed")
    public BaseResponse<FeedPageResponse> feed(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        // 1. 提取用户 ID（可能为 null，表示匿名访问）
        Long userId = (jwt == null) ? null : jwtService.extractUserId(jwt);

        // 2. 调用 FeedService 获取数据
        return ResultUtils.success(feedService.getPublicFeed(page, size, userId));
    }

    /**
     * 我的知文（当前用户已发布）分页查询；默认每页 20，最大 50。
     */
    @GetMapping("/mine")
    public BaseResponse<FeedPageResponse>  mine(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        return ResultUtils.success(feedService.getMyPublished(userId, page, size));    }

    /**
     * 知文详情（公开：published+public；非公开需作者本人）。
     */
    @GetMapping("/detail/{id}")
    public BaseResponse<KnowPostDetailResponse > detail(@PathVariable("id") long id,@AuthenticationPrincipal Jwt jwt
                                         ) {
        long userId = jwtService.extractUserId(jwt);
        ThrowUtils.throwIf(userId <=0 , ErrorCode.PARAMS_ERROR);
        KnowPostDetailResponse knowPostDetailResponse = service.getDetail(id, userId);
        return ResultUtils.success(knowPostDetailResponse);
    }
}
