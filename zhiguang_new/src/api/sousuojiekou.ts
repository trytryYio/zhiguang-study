// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 全文搜索 GET /api/v1/search */
export async function search(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.searchParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSearchResult>("/api/v1/search", {
    method: "GET",
    params: {
      // page has a default value: 1
      page: "1",
      // size has a default value: 20
      size: "20",
      ...params,
    },
    ...(options || {}),
  });
}

/** 联想建议 GET /api/v1/search/suggest */
export async function suggest(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.suggestParams,
  options?: { [key: string]: any }
) {
  return request<string[]>("/api/v1/search/suggest", {
    method: "GET",
    params: {
      // size has a default value: 10
      size: "10",
      ...params,
    },
    ...(options || {}),
  });
}
