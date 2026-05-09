// 委托到统一的 api/client
import { api } from "@/api/client";
import type { RelationStatusResponse, RelationCountersResponse } from "@/types/relation";
import type { ProfileResponse } from "@/types/profile";

export const relationService = {
    follow: (toUserId: number, _accessToken: string) =>
        api.relation.follow(toUserId),

    unfollow: (toUserId: number, _accessToken: string) =>
        api.relation.unfollow(toUserId),

    status: (toUserId: number, _accessToken: string) =>
        api.relation.status(toUserId) as Promise<RelationStatusResponse>,

    following: (userId: number, limit = 20, offset = 0, cursor?: number, _accessToken?: string) =>
        api.relation.following(userId, limit, offset, cursor) as Promise<ProfileResponse[]>,

    followers: (userId: number, limit = 20, offset = 0, cursor?: number, _accessToken?: string) =>
        api.relation.followers(userId, limit, offset, cursor) as Promise<ProfileResponse[]>,

    counters: (userId: number, _accessToken: string) =>
        api.relation.counters(userId) as Promise<RelationCountersResponse>
};