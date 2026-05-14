// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /api/v1/storage/presign */
export async function presign(
  body: API.StoragePresignRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseStoragePresignResponse>(
    "/api/v1/storage/presign",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      data: body,
      ...(options || {}),
    }
  );
}
