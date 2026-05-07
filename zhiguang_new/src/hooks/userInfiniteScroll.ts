import { useState, useEffect, useRef, useCallback } from 'react';
/**
 * 
 * @param fetchFn 用于实现无限滚动加载功能。它适用于需要分页加载数据的场景（例如：动态加载列表、评论、商品等）。
 * @param initialSize 
 * @returns 
 */
export function useInfiniteScroll(
    fetchFn: (page: number, size: number) => Promise<{ items: any[], hasMore: boolean }>,
    initialSize = 20
) {
    const [items, setItems] = useState<any[]>([]);
    const [hasMore, setHasMore] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // ★ 所有可变值都用 ref 保存，彻底避免闭包陷阱
    const pageRef = useRef(1);
    const loadingRef = useRef(false);
    const hasMoreRef = useRef(true);

    // ★ fetchFn 用 ref 保存，loadMore 不依赖 fetchFn
    const fetchFnRef = useRef(fetchFn);
    useEffect(() => { fetchFnRef.current = fetchFn; }, [fetchFn]);

    // 保存 IntersectionObserver 实例的 ref
    const observerRef = useRef<IntersectionObserver | null>(null);
    // 指向页面底部"哨兵"元素的 ref
    const sentinelRef = useRef<HTMLDivElement | null>(null);

    // ★ 同步写入 ref + state，杜绝时序漏洞
    const setLoadingSync = (val: boolean) => {
        loadingRef.current = val;
        setLoading(val);
    };
    const setHasMoreSync = (val: boolean) => {
        hasMoreRef.current = val;
        setHasMore(val);
    };

    // ★ loadMore 不依赖 fetchFn，通过 ref 读取
    const loadMore = useCallback(async () => {
        if (loadingRef.current || !hasMoreRef.current) return;

        setLoadingSync(true);
        setError(null);

        try {
            const currentPage = pageRef.current;
            const resp = await fetchFnRef.current(currentPage, initialSize);

            // ★ null 安全：API 可能返回 null（比如响应拦截器解包失败）
            const safeItems = resp?.items ?? [];
            const safeHasMore = resp?.hasMore ?? false;

            setItems(prev => [...prev, ...safeItems]);
            setHasMoreSync(safeHasMore);
            pageRef.current += 1;
        } catch (err) {
            setError(err instanceof Error ? err.message : '加载失败');
        } finally {
            setLoadingSync(false);
        }
    }, [initialSize]);

    // 组件挂载时自动触发首次加载（只执行一次）
    const initializedRef = useRef(false);
    useEffect(() => {
        if (!initializedRef.current) {
            initializedRef.current = true;
            loadMore();
        }
    }, [loadMore]);

    // ★ 修复：Observer 绑定不能依赖 loadMore（它不变），需要在 sentinelRef 挂载后延迟绑定
    // 用 useEffect 监听 sentinel DOM 是否就绪，然后在下一个微任务绑定 Observer
    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel) return;

        // 如果已有 Observer 先断开
        if (observerRef.current) {
            observerRef.current.disconnect();
        }

        observerRef.current = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting && hasMoreRef.current && !loadingRef.current) {
                    loadMore();
                }
            },
            { threshold: 0.1, rootMargin: '200px' }
        );

        observerRef.current.observe(sentinel);

        return () => {
            if (observerRef.current) {
                observerRef.current.disconnect();
                observerRef.current = null;
            }
        };
    }, [loadMore, items]); // ★ items 变化时重新绑定，因为 DOM 可能更新了哨兵元素

    return {
        items,
        loading,
        error,
        hasMore,
        sentinelRef,
        retry: loadMore
    };
}
