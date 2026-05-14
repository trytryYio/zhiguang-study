// @ts-ignore
/* eslint-disable */

import request from "../request";

/** 此处后端没有提供注释 GET /api/v1/relation/counter */
export async function counter(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.counterParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringLong>("/api/v1/relation/counter", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/relation/follow */
export async function follow(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.followParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/api/v1/relation/follow", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 GET /api/v1/relation/followers */
export async function followers(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.followersParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListProfileResponse>(
    "/api/v1/relation/followers",
    {
      method: "GET",
      params: {
        // limit has a default value: 20
        limit: "20",

        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/v1/relation/following */
export async function following(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.followingParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListProfileResponse>(
    "/api/v1/relation/following",
    {
      method: "GET",
      params: {
        // limit has a default value: 20
        limit: "20",

        ...params,
      },
      ...(options || {}),
    }
  );
}

/** 此处后端没有提供注释 GET /api/v1/relation/status */
export async function status(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.statusParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseMapStringBoolean>("/api/v1/relation/status", {
    method: "GET",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}

/** 此处后端没有提供注释 POST /api/v1/relation/unfollow */
export async function unfollow(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.unfollowParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>("/api/v1/relation/unfollow", {
    method: "POST",
    params: {
      ...params,
    },
    ...(options || {}),
  });
}
