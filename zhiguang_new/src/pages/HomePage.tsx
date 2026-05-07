import { useCallback } from "react";
import AppLayout from "../components/layout/AppLayout";
import MainHeader from "../components/layout/MainHeader";
import AuthStatus from "../features/auth/AuthStatus";
import { feed } from "../api/knowPostController";
import styles from "../pages/HomePage.module.css";
import CourseCard from "../components/cards/CourseCard";
import LikeFavBar from "../components/common/LikeFavBar";
import { useInfiniteScroll } from "../hooks/userInfiniteScroll";

/**
 * 首页 — 第一阶段占位。
 *
 * <p>第二阶段接入 AuthContext 显示登录状态；
 * 第四阶段接入 Feed API 显示知文列表。</p>
 */

const HomePage = () => {
  // ★ useCallback 包裹，保证引用稳定，不会每次渲染创建新函数
  const fetchFeed = useCallback(async (page: number, size: number) => {
    console.log("请求分页:", { page, size });
    const resp = await feed({ page, size });
    console.log("返回数据:", resp);
    // ★ null 安全：API 可能返回 null
    return {
      items: resp?.items ?? [],
      hasMore: resp?.hasMore ?? false,
    };
  }, []);

  // 使用 Hook 获取状态
  // 在组件挂载时自动加载首页数据（知文列表）
  const { items, loading, error, sentinelRef } = useInfiniteScroll(
    fetchFeed,
    20,
  );

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
          // 列表
          <div key={item.id} className={styles.me}>
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
        {/* 哨兵元素 - 用于检测滚动到底部 */}
        <div ref={sentinelRef} style={{ height: "20px" }} />
        {loading ? (
          <div className={styles.masonryItem}>
            <div>加载中</div>
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
