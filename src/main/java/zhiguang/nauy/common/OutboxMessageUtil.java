package zhiguang.nauy.common;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**
 * Outbox 消息解析工具。
 *
 * <p>用于消费 Canal 推送的 binlog JSON 消息，从中提取 outbox 表的行数据（INSERT/UPDATE）。</p>
 */
@NoArgsConstructor
public final class OutboxMessageUtil {


    /**
     * 从 Canal Binlog 消息中提取 outbox 表的有效数据行
     *
     * @param message Canal 发送的 JSON 格式 Binlog 消息
     * @return 提取出的数据行列表，每条数据是一个 JSONObject
     */

    public static List<JSONObject> extractRows(String message) {
        try {
            // 1. 解析 JSON 消息
            JSONObject root = JSONUtil.parseObj(message);

            // 2. 过滤表名：只处理 outbox 表
            String table = root.getStr("table");
            if (table == null || !"outbox".equals(table)) {
                return Collections.emptyList();
            }

            // 3. 过滤操作类型：只处理 INSERT 和 UPDATE
            String type = root.getStr("type");
            if (type == null || (!"INSERT".equals(type) && !"UPDATE".equals(type))) {
                return Collections.emptyList();
            }

            // 4. 提取数据数组
            JSONArray data = root.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return Collections.emptyList();
            }

            // 5. 转换为 List<JSONObject>
            List<JSONObject> rows = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                rows.add(data.getJSONObject(i));
            }
            return rows;

        } catch (Exception e) {
            // 解析失败时返回空列表，避免消费者崩溃
            return Collections.emptyList();
        }
    }


}
