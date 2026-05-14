// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 知文知识问答（SSE流式） GET /api/v1/knowposts/${param0}/qa/stream */
export async function streamQa(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.streamQAParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<string[]>(`/api/v1/knowposts/${param0}/qa/stream`, {
    method: "GET",
    params: {
      // topK has a default value: 5
      topK: "5",
      // maxTokens has a default value: 1024
      maxTokens: "1024",
      ...queryParams,
    },
    ...(options || {}),
  });
}

/** 手动触发RAG重建索引 POST /api/v1/knowposts/${param0}/rag/reindex */
export async function reindex(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.reindexParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseInteger>(
    `/api/v1/knowposts/${param0}/rag/reindex`,
    {
      method: "POST",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}
