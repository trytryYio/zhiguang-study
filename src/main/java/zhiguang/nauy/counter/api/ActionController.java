package zhiguang.nauy.counter.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zhiguang.nauy.auth.token.JwtService;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.counter.api.dto.ActionRequest;
import zhiguang.nauy.counter.service.CounterService;

import java.util.Map;

/**
 * 行为操作控制器。
 *
 * <p>处理用户行为操作：</p>
 * <ul>
 *   <li>POST /api/v1/action/like - 点赞</li>
 *   <li>POST /api/v1/action/unlike - 取消点赞</li>
 *   <li>POST /api/v1/action/fav - 收藏</li>
 *   <li>POST /api/v1/action/unfav - 取消收藏</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/action")
public class ActionController {


    private final CounterService counterService;
    private final JwtService jwtService;

    public ActionController(CounterService counterService, JwtService jwtService) {
        this.counterService = counterService;
        this.jwtService = jwtService;
    }

    /**
     * 点赞操作。
     */
    @PostMapping("/like")
    public BaseResponse<Map<String, Object>> like(@Valid @RequestBody ActionRequest req,
                                                                       @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        String entityType = req.getEntityType();
        String entityId = req.getEntityId();
        boolean changed = counterService.like(entityType, entityId, userId);

        return ResultUtils.success(Map.of("changed",changed,
                "liked", counterService.isLiked(entityType, entityId, userId)
            ));
    }

    /**
     * 取消点赞操作。
     */
    @PostMapping("/unlike")
    public BaseResponse<Map<String, Object>> unlike(@Valid @RequestBody ActionRequest req,
                                                      @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        String entityType = req.getEntityType();
        String entityId = req.getEntityId();
        boolean changed = counterService.unlike(entityType, entityId, userId);
        return ResultUtils.success(Map.of("changed",changed,
            "liked", counterService.isLiked(entityType, entityId, userId)
        ));
    }

    /**
     * 收藏操作。
     */
    @PostMapping("/fav")
    public BaseResponse<Map<String, Object>> fav(@Valid @RequestBody ActionRequest req,
                                                   @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        String entityType = req.getEntityType();
        String entityId = req.getEntityId();
        boolean changed = counterService.fav(entityType, entityId, userId);

        return ResultUtils.success(Map.of("changed",changed,
            "faved", counterService.isFaved(entityType, entityId, userId)
        ));

    }

    /**
     * 取消收藏操作。
     */
    @PostMapping("/unfav")
    public BaseResponse<Map<String, Object>> unfav(@Valid @RequestBody ActionRequest req,
                                                     @AuthenticationPrincipal Jwt jwt) {
        long userId = jwtService.extractUserId(jwt);
        String entityType = req.getEntityType();
        String entityId = req.getEntityId();
        boolean changed = counterService.unfav(entityType, entityId, userId);

        return ResultUtils.success(Map.of("changed",changed,
            "faved", counterService.isFaved(entityType, entityId, userId)
        ));
    }
}
