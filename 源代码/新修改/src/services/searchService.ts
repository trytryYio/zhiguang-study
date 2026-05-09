// 委托到统一的 api/client
import { api } from "@/api/client";
import type { SearchResponse, SuggestResponse } from "@/types/search";

export const searchService = {
    query: (params: { q: string; size?: number; tags?: string; after?: string | null }) => {
        const { q, size = 20, tags, after } = params;
        return api.search.query(q, size, tags, after ?? undefined) as Promise<SearchResponse>;
    },

    suggest: (prefix: string, size = 10) =>
        api.search.suggest(prefix, size) as Promise<SuggestResponse>
};