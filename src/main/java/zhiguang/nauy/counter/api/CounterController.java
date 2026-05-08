package zhiguang.nauy.counter.api;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import zhiguang.nauy.common.BaseResponse;
import zhiguang.nauy.common.ResultUtils;
import zhiguang.nauy.counter.api.dto.CountsResponse;
import zhiguang.nauy.counter.schema.CounterSchema;
import zhiguang.nauy.counter.service.CounterService;
import zhiguang.nauy.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计数查询控制器。
 *
 * <p>读取计数：</p>
 * <ul>
 *   <li>GET /api/v1/counter/{etype}/{eid}?metrics=like,fav - 查询指定指标计数</li>
 * </ul>
 */
@RestController

@RequestMapping("/api/v1/counter")
public class CounterController {
    @Resource
    private CounterService counterService;


    /**
     * 获取实体的计数汇总。
     *
     * @param entityType 实体类型（如 knowpost）
     * @param entityId   实体ID
     * @param metricsStr 指标列表（逗号分隔），为空则返回全部支持指标
     */
    @GetMapping("/{etype}/{eid}")
    public BaseResponse<CountsResponse> getCounts(@PathVariable("etype") String entityType,
                                                  @PathVariable("eid") String entityId,
                                                  @RequestParam(value = "metrics", required = false) String metricsStr) {
        //1.确定要查询的指标列表
        List< String> metrics;
        if (metricsStr == null) {
            //不传输参数 ->查询全部
            metrics = new ArrayList<>(CounterSchema.SUPPORTED_METRICS);
        } else {
            //拆分都好分割的字符串
           metrics = List.of(metricsStr.split(","));

            //验证每个是否合法
            for (String metric : metrics){
                if (!CounterSchema.SUPPORTED_METRICS.contains(metric)) {
                    return new BaseResponse<>(ErrorCode.PARAMS_ERROR);
                }

            }
        }

        //2.调用service查询
        Map<String, Long> counts = counterService.getCounts(entityType, entityId, metrics);
        return ResultUtils.success(new CountsResponse(entityType, entityId, counts));

    }

}
