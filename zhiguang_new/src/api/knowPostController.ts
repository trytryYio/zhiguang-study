// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 DELETE /api/v1/knowposts/${param0} */
export async function deleteUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseVoid>(`/api/v1/knowposts/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PATCH /api/v1/knowposts/${param0} */
export async function patchMetadata(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.patchMetadataParams,
  body: API.KnowPostPatchRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponse>(`/api/v1/knowposts/${param0}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/knowposts/${param0}/content/confirm */
export async function confirmContent(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.confirmContentParams,
  body: API.KnowPostContentConfirmRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponse>(
    `/api/v1/knowposts/${param0}/content/confirm`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/v1/knowposts/${param0}/publish */
export async function publish(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.publishParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponse>(`/api/v1/knowposts/${param0}/publish`, {
    method: "POST",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PATCH /api/v1/knowposts/${param0}/top */
export async function patchTop(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.patchTopParams,
  body: API.KnowPostTopPatchRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseVoid>(`/api/v1/knowposts/${param0}/top`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 PATCH /api/v1/knowposts/${param0}/visibility */
export async function patchVisibility(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.patchVisibilityParams,
  body: API.KnowPostVisibilityPatchRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseVoid>(
    `/api/v1/knowposts/${param0}/visibility`,
    {
      method: "PATCH",
      headers: {
        "Content-Type": "application/json",
      },
      params: { ...queryParams },
      data: body,
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/v1/knowposts/detail/${param0} */
export async function detail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.detailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.BaseResponseKnowPostDetailResponse>(
    `/api/v1/knowposts/detail/${param0}`,
    {
      method: "GET",
      params: { ...queryParams },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 POST /api/v1/knowposts/drafts */
export async function createDraft(options?: { [key: string]: any }) {
  return request<API.BaseResponseKnowPostDraftCreateResponse>(
    "/api/v1/knowposts/drafts",
    {
      method: "POST",
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/v1/knowposts/feed */
export async function feed(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.feedParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseFeedPageResponse>("/api/v1/knowposts/feed", {
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

/** 此处后端没有提供注释 GET /api/v1/knowposts/mine */
export async function mine(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.mineParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseFeedPageResponse>("/api/v1/knowposts/mine", {
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
