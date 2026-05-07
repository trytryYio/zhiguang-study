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

/** 此处后端没有提供注释 POST /api/v1/profile/avatar */
export async function uploadAvatar(
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData();

  if (file) {
    formData.append("file", file);
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele];

    if (item !== undefined && item !== null) {
      if (typeof item === "object" && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ""));
        } else {
          formData.append(
            ele,
            new Blob([JSON.stringify(item)], { type: "application/json" })
          );
        }
      } else {
        formData.append(ele, item);
      }
    }
  });

  return request<API.BaseResponseProfileResponse>("/api/v1/profile/avatar", {
    method: "POST",
    data: formData,
    requestType: "form",
    ...(options || {}),
  });
}
