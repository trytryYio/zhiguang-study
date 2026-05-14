import { useEffect, useRef, useState } from "react";
import AppLayout from "../components/layout/AppLayout";
import MainHeader from "../components/layout/MainHeader";
import AuthStatus from "../features/auth/AuthStatus";
import { feed } from "../api/knowPostController";
import styles from "../pages/HomePage.module.css";
import CourseCard from "../components/cards/CourseCard";
import LikeFavBar from "../components/common/LikeFavBar";

const HomePage = () => {
  const [items, setItems] = useState<any[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadingRef = useRef(false);
  const hasMoreRef = useRef(true);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  const loadMore = async () => {
    if (loadingRef.current || !hasMoreRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    setError(null);
    try {
      const resp = await feed({ page, size: 20 });
      const newItems = resp?.items ?? [];
      const more = resp?.hasMore ?? false;
      setItems((prev) => [...prev, ...newItems]);
      setHasMore(more);
      hasMoreRef.current = more;
      setPage((p) => p + 1);
    } catch (err) {
      setError(err instanceof Error ? err.message : "加载失败");
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  };

  // 首次加载
  useEffect(() => {
    loadMore();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // IntersectionObserver 监听哨兵
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasMoreRef.current && !loadingRef.current) {
          loadMore();
        }
      },
      { rootMargin: "200px" },
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [page, hasMore]);

  return (
    <AppLayout
      header={
        <MainHeader
          headline="知光 · 让思想有温度，让知识会发光"
          subtitle="欢迎来到知光平台"
          rightSlot={<AuthStatus />}
        />
      }
    >
      {error ? <div>{error}</div> : null}
      <div className={styles.masonry}>
        {items.map((item) => (
          <div key={item.id} className={styles.masonryItem}>
            <CourseCard
              id={item.id}
              title={item.title}
              summary={item.description ?? ""}
              tags={item.tags ?? []}
              teacher={{
                name: item.authorNickname,
                avatarUrl: item.authorAvatar,
              }}
              authorTags={(() => {
                try {
                  return item.tagJson
                    ? ((JSON.parse(item.tagJson) as unknown[]).filter(
                        (t) => typeof t === "string",
                      ) as string[])
                    : [];
                } catch (e) {
                  return [];
                }
              })()}
              coverImage={item.coverImage}
              to={`/post/${item.id}`}
              footerExtra={
                <LikeFavBar
                  entityId={item.id}
                  compact
                  initialCounts={{
                    like: item.likeCount ?? 0,
                    fav: item.favoriteCount ?? 0,
                  }}
                  initialState={{ liked: item.liked, faved: item.faved }}
                />
              }
            />
          </div>
        ))}
        {/* 哨兵元素 */}
        <div ref={sentinelRef} style={{ height: "20px" }} />
        {loading ? (
          <div className={styles.masonryItem}>
            <div>加载中…</div>
          </div>
        ) : null}
        {!loading && !hasMore && items.length > 0 ? (
          <div className={styles.masonryItem} style={{ textAlign: "center", color: "var(--color-text-muted)" }}>
            <div>到底啦，没有更多了</div>
          </div>
        ) : null}
        {!loading && items.length === 0 ? (
          <div className={styles.masonryItem}>
            <div>暂无内容</div>
          </div>
        ) : null}
      </div>
    </AppLayout>
  );
};

export default HomePage;
