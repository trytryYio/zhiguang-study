// 委托到统一的 api/client
import { api, type Schemas } from "@/api/client";
import type {
    CreateDraftResponse, PresignRequest, PresignResponse,
    ConfirmContentRequest, UpdateKnowPostRequest, FeedResponse,
    KnowpostDetailResponse, LikeActionResponse, FavActionResponse,
    CounterResponse, VisibleScope
} from "@/types/knowpost";

export const knowpostService = {
    createDraft: () =>
        api.knowposts.createDraft() as Promise<CreateDraftResponse>,

    presign: (payload: PresignRequest) =>
        api.storage.presign(payload as Schemas["StoragePresignRequest"]) as Promise<PresignResponse>,

    confirmContent: (id: string, payload: ConfirmContentRequest) =>
        api.knowposts.confirmContent(id, payload as Schemas["KnowPostContentConfirmRequest"]),

    update: (id: string, payload: UpdateKnowPostRequest) =>
        api.knowposts.update(id, payload as Schemas["KnowPostPatchRequest"]),

    publish: (id: string) =>
        api.knowposts.publish(id),

    setTop: (id: string, isTop: boolean, _accessToken: string) =>
        api.knowposts.setTop(id, { isTop }),

    setVisibility: (id: string, visible: VisibleScope, _accessToken: string) =>
        api.knowposts.setVisibility(id, { visible } as Schemas["KnowPostVisibilityPatchRequest"]),

    remove: (id: string, _accessToken: string) =>
        api.knowposts.remove(id),

    feed: (page = 1, size = 20) =>
        api.knowposts.feed(page, size) as Promise<FeedResponse>,

    mine: (page = 1, size = 20, _accessToken: string) =>
        api.knowposts.mine(page, size) as Promise<FeedResponse>,

    detail: (id: string, _accessToken?: string) =>
        api.knowposts.detail(id) as Promise<KnowpostDetailResponse>,

    suggestDescription: (content: string, _accessToken: string) =>
        api.knowposts.suggestDescription(content) as Promise<{ description: string }>,

    like: (entityId: string, _accessToken: string, entityType = "knowpost") =>
        api.action.like({ entityType, entityId }) as Promise<LikeActionResponse>,

    unlike: (entityId: string, _accessToken: string, entityType = "knowpost") =>
        api.action.unlike({ entityType, entityId }) as Promise<LikeActionResponse>,

    fav: (entityId: string, _accessToken: string, entityType = "knowpost") =>
        api.action.fav({ entityType, entityId }) as Promise<FavActionResponse>,

    unfav: (entityId: string, _accessToken: string, entityType = "knowpost") =>
        api.action.unfav({ entityType, entityId }) as Promise<FavActionResponse>,

    counters: (entityId: string, _accessToken: string, entityType = "knowpost") =>
        api.counter.getCounts(entityType, entityId, "like,fav") as Promise<CounterResponse>
};

// 直传工具（不变，非 API 逻辑）
export async function uploadToPresigned(putUrl: string, headers: Record<string, string>, file: File) {
    const resp = await fetch(putUrl, { method: "PUT", headers, body: file, credentials: "omit" });
    if (!resp.ok) {
        const text = await resp.text().catch(() => "");
        throw new Error(text || `上传失败：${resp.status}`);
    }
    const etag = resp.headers.get("ETag") || resp.headers.get("etag") || "";
    return { etag };
}

export async function computeSha256(file: File) {
    const buf = await file.arrayBuffer();
    const digest = await crypto.subtle.digest("SHA-256", buf);
    return Array.from(new Uint8Array(digest)).map(b => b.toString(16).padStart(2, "0")).join("");
}