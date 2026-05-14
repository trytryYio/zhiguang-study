// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 GET /api/v1/counter/${param0}/${param1} */
export async function getCounts(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getCountsParams,
  options?: { [key: string]: any }
) {
  const { etype: param0, eid: param1, ...queryParams } = params;
  return request<API.BaseResponseCountsResponse>(
    `/api/v1/counter/${param0}/${param1}`,
    {
      method: "GET",
      params: {
        ...queryParams,
      },
      ...(options || {}),
    }
  );
}
