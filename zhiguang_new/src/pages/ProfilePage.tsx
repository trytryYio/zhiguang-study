import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { mine } from "../api/knowPostController";
import { Link } from "react-router-dom";
import SectionHeader from "../components/common/SectionHeader";
import AppLayout from "../components/layout/AppLayout";
import MainHeader from "../components/layout/MainHeader";
import AuthStatus from "../features/auth/AuthStatus";
import styles from "./ProfilePage.module.css";
import RelationCounters from "../components/common/RelationCounters";
import CourseCard from "../components/cards/CourseCard";
import feedStyles from "./HomePage.module.css";
import LikeFavBar from "../components/common/LikeFavBar";

/**
 * 个人资料页面 - 显示用户个人信息和内容
 **/
const ProfilePage = () => {
  const { user, tokens } = useAuth();
  const displayName = user?.nickname ?? user?.phone ?? "知光用户";
  const avatarInitial = displayName.trim().charAt(0) || "知";

  // 领域标签展示：仅解析 tagJson
  // useMemo 用于缓存计算结果，避免在每次组件渲染时都进行重复的、开销较大的计算。
  const tags = useMemo(() => {
    if (user && typeof user.tagJson === "string") {
      try {
        const parsed = JSON.parse(user.tagJson);
        return Array.isArray(parsed)
          ? parsed.filter((t) => typeof t === "string")
          : [];
      } catch {
        return [];
      }
    }
    return [];
  }, [user]);

  // 我的知文列表
  const [items, setItems] = useState<Array<API.FeedItemResponse>>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 抽取成单一函数，供首次加载与编辑动作后复用
  // 重新加载当前登录用户（"我"）的个人内容列表（例如文章、帖子等）
  const reloadMine = async () => {
    if (!tokens?.accessToken) return; // 没有令牌
    setLoading(true);
    setError(null);
    try {
      const resp = await mine({ page: 1, size: 20 }, tokens.accessToken);
      console.log("mine API response:", resp);
      setItems(resp?.items ?? []);
      setHasMore(!!resp?.hasMore);
      setPage(resp?.page ?? 1);
    } catch (err) {
      console.error("mine API error:", err);
      const msg = err instanceof Error ? err.message : "加载失败";
      setError(`加载我的知文失败: ${msg}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void reloadMine();
  }, [tokens?.accessToken]);

  return (
    <AppLayout
      header={
        <MainHeader
          headline="我的知光主页"
          subtitle="完善个人信息，积累你的知识资产"
          rightSlot={<AuthStatus />}
        />
      }
    >
      <>
        <SectionHeader
          title="个人信息"
          subtitle="让同学们更快认识你"
          actions={
            <Link to="/profile/edit" className="ghost-button">
              编辑资料
            </Link>
          }
        />
        <div className={styles.profileGrid}>
          <div className={styles.avatarBox}>
            {user?.avatar ? (
              <img
                src={user.avatar}
                alt="avatar"
                className={styles.avatarImg}
              />
            ) : (
              <span>{avatarInitial}</span>
            )}
          </div>
          <div className={styles.infoBox}>
            <div className={styles.nickname}>{displayName}</div>
            <div className={styles.tags}>
              {tags.length > 0 ? (
                tags.map((tag) => <span key={tag}>{tag}</span>)
              ) : (
                <span>未设置</span>
              )}
            </div>
          </div>
        </div>
        <div className={styles.bioBlock}>{user?.bio ?? "暂无简介"}</div>

        {/* 关系计数展示 */}
        {user?.id ? (
          <div style={{ marginTop: 8 }}>
            <RelationCounters userId={user.id} />
          </div>
        ) : null}

        <SectionHeader title="我的知文" subtitle="仅显示你已发布的知文" />
        {error ? (
          <div style={{ color: "var(--color-danger)" }}>{error}</div>
        ) : null}
        {!user ? (
          <div style={{ color: "var(--color-text-muted)", padding: 12 }}>
            请登录后查看你的知文
          </div>
        ) : (
          <div className={feedStyles.masonry}>
            {items.map((item) => (
              <div key={item.id} className={feedStyles.masonryItem}>
                <CourseCard
                  id={item.id}
                  title={item.title}
                  summary={item.description ?? ""}
                  tags={item.tags ?? []}
                  isTop={item.isTop}
                  authorTags={(() => {
                    try {
                      return item.tagJson
                        ? ((JSON.parse(item.tagJson) as unknown[]).filter(
                            (t) => typeof t === "string",
                          ) as string[])
                        : [];
                    } catch {
                      return [];
                    }
                  })()}
                  teacher={{
                    name: item.authorNickname,
                    avatarUrl: item.authorAvatar ?? item.authorAvator,
                  }}
                  coverImage={item.coverImage}
                  to={`/post/${item.id}`}
                  editable
                  onChanged={(action) => {
                    if (action === "delete") {
                      setItems((prev) => prev.filter((x) => x.id !== item.id));
                    } else {
                      void reloadMine();
                    }
                  }}
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
            {loading ? (
              <div className={feedStyles.masonryItem}>
                <div>加载中…</div>
              </div>
            ) : null}
            {!loading && items.length === 0 ? (
              <div className={feedStyles.masonryItem}>
                <div>暂无内容</div>
              </div>
            ) : null}
          </div>
        )}
      </>
    </AppLayout>
  );
};

export default ProfilePage;
