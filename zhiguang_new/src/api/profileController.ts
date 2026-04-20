// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 PATCH /api/v1/profile */
export async function patch(
  body: API.ProfilePatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseProfileResponse>("/api/v1/profile", {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
