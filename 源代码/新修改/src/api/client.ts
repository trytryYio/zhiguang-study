import type { paths, components } from "./schema";

type Schemas = components["schemas"];

// --- 基础 fetch 封装 ---

const getBaseUrl = () => {
    const envBase = import.meta.env.VITE_API_BASE_URL as string | undefined;
    return envBase?.replace(/\/$/, "") ?? "";
};

const getStoredToken = (): string | null => {
    try {
        const raw = localStorage.getItem("zhiguang_auth_tokens");
        if (!raw) return null;
        return (JSON.parse(raw) as { accessToken?: string }).accessToken ?? null;
    } catch {
        return null;
    }
};

async function request<T>(path: string, method = "GET", body?: unknown): Promise<T> {
    const url = `${getBaseUrl()}${path}`;
    const isFormData = typeof FormData !== "undefined" && body instanceof FormData;
    const headers: Record<string, string> = isFormData ? {} : { "Content-Type": "application/json" };

    const token = getStoredToken();
    if (token) headers.Authorization = `Bearer ${token}`;

    const resp = await fetch(url, {
        method,
        headers,
        body: isFormData ? (body as FormData) : body ? JSON.stringify(body) : undefined,
        credentials: "include"
    });

    if (!resp.ok) {
        const text = await resp.text().catch(() => "");
        throw new Error(text || `请求失败：${resp.status}`);
    }
    if (resp.status === 204) return undefined as T;
    const ct = resp.headers.get("content-type");
    return ct?.includes("application/json") ? resp.json() : (await resp.text() as T);
}

// --- API 客户端 ---

export const api = {
    // Auth
    auth: {
        sendCode: (body: Schemas["SendCodeRequest"]) =>
            request<Schemas["SendCodeResponse"]>("/api/v1/auth/send-code", "POST", body),
        register: (body: Schemas["RegisterRequest"]) =>
            request<Schemas["AuthResponse"]>("/api/v1/auth/register", "POST", body),
        login: (body: Schemas["LoginRequest"]) =>
            request<Schemas["AuthResponse"]>("/api/v1/auth/login", "POST", body),
        refresh: (body: Schemas["TokenRefreshRequest"]) =>
            request<Schemas["TokenResponse"]>("/api/v1/auth/token/refresh", "POST", body),
        logout: (body: Schemas["LogoutRequest"]) =>
            request<void>("/api/v1/auth/logout", "POST", body),
        resetPassword: (body: Schemas["PasswordResetRequest"]) =>
            request<void>("/api/v1/auth/password/reset", "POST", body),
        me: () =>
            request<Schemas["AuthUserResponse"]>("/api/v1/auth/me"),
    },

    // KnowPost
    knowposts: {
        createDraft: () =>
            request<Schemas["KnowPostDraftCreateResponse"]>("/api/v1/knowposts/drafts", "POST"),
        confirmContent: (id: string, body: Schemas["KnowPostContentConfirmRequest"]) =>
            request<void>(`/api/v1/knowposts/${id}/content/confirm`, "POST", body),
        update: (id: string, body: Schemas["KnowPostPatchRequest"]) =>
            request<void>(`/api/v1/knowposts/${id}`, "PATCH", body),
        publish: (id: string) =>
            request<void>(`/api/v1/knowposts/${id}/publish`, "POST"),
        setTop: (id: string, body: Schemas["KnowPostTopPatchRequest"]) =>
            request<void>(`/api/v1/knowposts/${id}/top`, "PATCH", body),
        setVisibility: (id: string, body: Schemas["KnowPostVisibilityPatchRequest"]) =>
            request<void>(`/api/v1/knowposts/${id}/visibility`, "PATCH", body),
        remove: (id: string) =>
            request<void>(`/api/v1/knowposts/${id}`, "DELETE"),
        feed: (page = 1, size = 20) =>
            request<Schemas["FeedPageResponse"]>(`/api/v1/knowposts/feed?page=${page}&size=${size}`),
        mine: (page = 1, size = 20) =>
            request<Schemas["FeedPageResponse"]>(`/api/v1/knowposts/mine?page=${page}&size=${size}`),
        detail: (id: string) =>
            request<Schemas["KnowPostDetailResponse"]>(`/api/v1/knowposts/detail/${id}`),
        suggestDescription: (content: string) =>
            request<{ description?: string }>("/api/v1/knowposts/description/suggest", "POST", { content }),
    },

    // Storage
    storage: {
        presign: (body: Schemas["StoragePresignRequest"]) =>
            request<Schemas["StoragePresignResponse"]>("/api/v1/storage/presign", "POST", body),
    },

    // Action (like/fav)
    action: {
        like: (body: Schemas["ActionRequest"]) =>
            request<Schemas["ActionResponse"]>("/api/v1/action/like", "POST", body),
        unlike: (body: Schemas["ActionRequest"]) =>
            request<Schemas["ActionResponse"]>("/api/v1/action/unlike", "POST", body),
        fav: (body: Schemas["ActionRequest"]) =>
            request<Schemas["ActionResponse"]>("/api/v1/action/fav", "POST", body),
        unfav: (body: Schemas["ActionRequest"]) =>
            request<Schemas["ActionResponse"]>("/api/v1/action/unfav", "POST", body),
    },

    // Counter
    counter: {
        getCounts: (entityType: string, entityId: string, metrics?: string) => {
            const q = metrics ? `?metrics=${metrics}` : "";
            return request<Schemas["CountsResponse"]>(`/api/v1/counter/${entityType}/${entityId}${q}`);
        },
    },

    // Relation
    relation: {
        follow: (toUserId: number) =>
            request<boolean>(`/api/v1/relation/follow?toUserId=${toUserId}`, "POST"),
        unfollow: (toUserId: number) =>
            request<boolean>(`/api/v1/relation/unfollow?toUserId=${toUserId}`, "POST"),
        status: (toUserId: number) =>
            request<Schemas["RelationStatusResponse"]>(`/api/v1/relation/status?toUserId=${toUserId}`),
        following: (userId: number, limit = 20, offset = 0, cursor?: number) => {
            const p = new URLSearchParams({ userId: String(userId), limit: String(limit), offset: String(offset) });
            if (cursor) p.set("cursor", String(cursor));
            return request<Schemas["ProfileResponse"][]>(`/api/v1/relation/following?${p}`);
        },
        followers: (userId: number, limit = 20, offset = 0, cursor?: number) => {
            const p = new URLSearchParams({ userId: String(userId), limit: String(limit), offset: String(offset) });
            if (cursor) p.set("cursor", String(cursor));
            return request<Schemas["ProfileResponse"][]>(`/api/v1/relation/followers?${p}`);
        },
        counters: (userId: number) =>
            request<Schemas["RelationCountersResponse"]>(`/api/v1/relation/counter?userId=${userId}`),
    },

    // Profile
    profile: {
        update: (body: Schemas["ProfilePatchRequest"]) =>
            request<Schemas["ProfileResponse"]>("/api/v1/profile", "PATCH", body),
        uploadAvatar: (file: File) => {
            const form = new FormData();
            form.append("file", file);
            return request<Schemas["ProfileResponse"]>("/api/v1/profile/avatar", "POST", form);
        },
    },

    // Search
    search: {
        query: (q: string, size = 20, tags?: string, after?: string) => {
            const p = new URLSearchParams({ q, size: String(size) });
            if (tags) p.set("tags", tags);
            if (after) p.set("after", after);
            return request<Schemas["SearchResponse"]>(`/api/v1/search?${p}`);
        },
        suggest: (prefix: string, size = 10) =>
            request<Schemas["SuggestResponse"]>(`/api/v1/search/suggest?prefix=${prefix}&size=${size}`),
    },
};

// Re-export types for page-level use
export type { paths, components };
export type { Schemas };
