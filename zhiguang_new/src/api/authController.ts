// @ts-ignore
/* eslint-disable */
import request from "@/request";

/** 此处后端没有提供注释 POST /api/v1/auth/login */
export async function login(
  body: API.LoginRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAuthResponse>("/api/v1/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/auth/logout */
export async function logout(
  body: API.LogoutRequest,
  options?: { [key: string]: any }
) {
  return request<any>("/api/v1/auth/logout", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/v1/auth/me */
export async function me(options?: { [key: string]: any }) {
  return request<API.BaseResponseAuthUserResponse>("/api/v1/auth/me", {
    method: "GET",
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/auth/password/reset */
export async function resetPassword(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.resetPasswordParams,
  options?: { [key: string]: any }
) {
  return request<any>("/api/v1/auth/password/reset", {
    method: "POST",
    params: {
      ...params,
      request: undefined,
      ...params["request"],
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/auth/register */
export async function register(
  body: API.RegisterRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAuthResponse>("/api/v1/auth/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/auth/send-code */
export async function sendCode(
  body: API.SendCodeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSendCodeResponse>("/api/v1/auth/send-code", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/auth/token/refresh */
export async function refresh(
  body: API.TokenRefreshRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseTokenResponse>("/api/v1/auth/token/refresh", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}
