// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /api/v1/action/fav */
export async function fav(
  body: API.ActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/api/v1/action/fav", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/action/like */
export async function like(
  body: API.ActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/api/v1/action/like", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/action/unfav */
export async function unfav(
  body: API.ActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/api/v1/action/unfav", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/action/unlike */
export async function unlike(
  body: API.ActionRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringObject>("/api/v1/action/unlike", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
