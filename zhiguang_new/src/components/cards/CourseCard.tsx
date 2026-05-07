// 知文卡片组件

import { ReactNode, useEffect, useRef, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import {
  deleteUsingDelete,
  detail,
  patchTop,
  patchVisibility,
} from "../../api/knowPostController";
import styles from "./CourseCard.module.css";
import { patch } from "../../api/profileController";
import Tag from "../common/Tag";
import { HeartIcon } from "../icons/Icon";
import clsx from "clsx";
import { Link } from "react-router-dom";

// 这个函数用于渲染高亮文本，主要用于搜索结果中高亮显示匹配的关键词。
const renderEmHighlightedText = (text: string): ReactNode => {
  // 匹配 <em> 标签</em> 没有这个标签就返回原文本
  if (!text.includes("<em")) return text;
  /**
 * 正则解释：
- <em - 匹配 <em 开始标签
- (?:\s[^>]*)? - 可选的属性（如 <em class="highlight">）  捕获第一次 
- > - 标签结束
- (.*?) - 捕获标签内的内容（非贪婪匹配）
- <\/em> - 匹配 </em> 结束标签 捕获第二次 
- gis - 全局匹配、忽略大小写、多行模式

 */
  //exec 发现 re 有 g 标志，就会从 re.lastIndex 的位置继续往后找，而不是从头开始。
  const re = /<em(?:\s[^>]*)?>(.*?)<\/em>/gis;
  // parts 的数组，用于存储解析后的文本片段和 React
  // 元素（如 <em>），最终组合成一个完整的 ReactNode 返回。
  const parts: ReactNode[] = [];

  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  // exec 是正则表达式的方法， 用来在字符串中匹配查找 符合规则的字符串
  while ((match = re.exec(text)) !== null) {
    const start = match.index;
    if (start > lastIndex) {
      //  提取高亮前的普通文本
      //   push 用于向数组末尾添加元素。
      parts.push(text.slice(lastIndex, start));
      //   创建高亮的 React 组件
      //match[0]是捕获第一次的值match[0] → "<em>习</em>"
      //match [1] 是捕获第二次的值 match[1] → "习"
      parts.push(<em key={key++}>{match[1]}</em>);

      lastIndex = start + match[0].length;
    }
    //  把最后一个 </em> 标签之后的剩余普通文本（未被高亮的部分）添加到 parts 数组中，
    // 确保整段文本完整不丢失。
    if (lastIndex < text.length) parts.push(text.slice(lastIndex));
    // 如果 parts 有内容（说明有高亮或普通文本），
    // 就用 React 的 Fragment <>{parts}</> 返回；
    return parts.length ? <>{parts}</> : text;
  }
};
type VisibleScope = "public" | "followers" | "school" | "private" | "unlisted";
export type CourseCardProps = {
  // 知文唯一标识
  id: string;
  // 知文标题（会经过 renderEmHighlightedText 处理高亮）
  title: string;
  // 知文摘要（也会处理高亮）
  summary: string;
  // 知文标签列表（如 ["技术", "前端"]）
  tags: string[];
  // 作者自定义标签（可选）
  authorTags?: string[];
  // 是否免费阅读（可选，默认 false）
  isFree?: boolean;
  // 是否置顶（可选）
  isTop?: boolean;
  // 作者信息对象
  teacher: {
    // 作者姓名
    name: string;
    // 头像文字（用于无头像时显示首字母，可选）
    avatarText?: string;
    // 头像图片 URL（可选）
    avatarUrl?: string;
  };
  // 统计数据（可选）
  stats?: {
    // 点赞数
    likes: number;
    // 浏览量
    views: number;
  };
  // 封面图 URL（可选）
  coverImage?: string;
  // 布局方向："vertical"（默认）或 "horizontal"
  layout?: "vertical" | "horizontal";
  // 是否显示内容类型角标（如“文章”、“视频”等，可选）
  showPlayBadge?: boolean;
  // 底部额外内容（可传入任意 React 元素）
  footerExtra?: ReactNode;
  // 点击跳转链接（可选）
  to?: string;
  // 自定义 CSS 类名（用于样式覆盖）
  className?: string;
  // 是否可编辑（控制操作按钮显示）
  editable?: boolean;
  // 编辑操作回调函数：

  onChanged?: (
    // action - 操作类型（置顶/可见性/删除）
    action: "top" | "visibility" | "delete",
    // payload - 附加数据（如删除确认信息）
    payload?: unknown,
  ) => void;
};
const CourseCard = ({
  id,
  title,
  summary,
  tags,
  authorTags,
  isFree = true,
  isTop,
  teacher,
  stats,
  coverImage,
  layout = "vertical",
  showPlayBadge,
  footerExtra,
  to,
  className,
  editable = false,
  onChanged,
}: CourseCardProps) => {
  const { tokens } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [details, setDetail] = useState<API.KnowPostDetailResponse | null>(
    null,
  );
  const [menuLoading, setMenuLoading] = useState(false);
  const [menuError, setMenuError] = useState<string | null>(null);
  const buttonRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  // 【数据加载函数】按需加载知文详情
  // 作用：当用户点击编辑菜单时，才去服务器获取完整的知文详情数据
  // 避免一开始就加载所有数据，节省网络资源和提高页面加载速度
  const loadDetailIfNeeded = async (id: string) => {
    // 守卫条件：如果已经加载过详情或者正在加载中，就直接返回，避免重复请求
    if (details || menuLoading) return;
    try {
      setMenuLoading(true); //显示加载状态
      // 嗲用服务器接口获取知文详情数据
      const res = await detail(id, tokens?.accessToken ?? undefined);
      setDetail(res); //保存获取到的详情数据
    } catch (e) {
      const msg = e instanceof Error ? e.message : "加载详情失败";
      setMenuError(msg);
    } finally {
      setMenuLoading(false); //不论是成功还是失败都要关闭加载状态
    }
  };

  //菜单切换函数 控制编辑菜单的显示和隐藏
  const toggleMenu = async (id: string) => {
    const next = !menuOpen; //  计算下一个状态（开/关）
    setMenuOpen(next); //更新菜单状态
    if (next) {
      await loadDetailIfNeeded(id); //如果菜单打开，则加载详情数据
    }
  };
  // 【点击外部关闭菜单】监听文档点击事件，实现点击卡片外部区域自动关闭菜单
  useEffect(() => {
    if (!menuOpen) return; // 如果菜单没打开，就不需要监听

    // 定义点击处理函数
    const onDocClick = (e: MouseEvent) => {
      const target = e.target as Node; // 获取点击的目标元素
      const btn = buttonRef.current; // 获取菜单按钮引用
      const menu = menuRef.current; // 获取菜单容器引用

      // 如果点击的是菜单内部或菜单按钮本身，就不关闭菜单
      if (menu && menu.contains(target)) return;
      if (btn && btn.contains(target)) return;

      // 其他情况都关闭菜单
      setMenuOpen(false);
    };

    // 添加事件监听器（使用捕获阶段确保能正确处理）
    document.addEventListener("mousedown", onDocClick, true);
    // 清理函数：组件卸载或依赖变化时移除监听器，防止内存泄漏
    return () => document.removeEventListener("mousedown", onDocClick, true);
  }, [menuOpen]); // 依赖数组：只有当 menuOpen 变化时才重新执行

  // 置顶操作 处理
  const handleSetTop = async (id: string, isTopped: boolean) => {
    try {
      // 权限检查：必须登录才能操作
      if (!tokens?.accessToken) {
        setMenuError("请先登录");
        return;
      }
      setMenuLoading(true); //显示加载状态
      // 调用服务器接口
      await patchTop(id, isTopped, tokens.accessToken);
      // 更新本地状态：保持UI与服务器数据同步

      setDetail((prev) =>
        prev
          ? {
              ...prev,
              isTop: isTopped,
            }
          : prev,
      );
      setMenuOpen(false); // 操作完成后关闭菜单
      // 通知父组件发生了置顶操作
      onChanged?.("top", { isTop });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "设置置顶失败";
      setMenuError(msg);
    } finally {
      setMenuLoading(false);
    }
  };

  // 处理设置可见范围

  // 【可见性设置处理】处理知文公开/私密设置
  const handleSetVisibility = async (id: string, visible: VisibleScope) => {
    // 核验登录权限
    if (!tokens?.accessToken) {
      setMenuError("请先登录");
      return;
    }
    setMenuLoading(true);
    try {
      await patchVisibility(id, visible, tokens.accessToken);
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              visible,
            }
          : prev,
      );
      setMenuOpen(false);
      onChanged?.("visibility", { visible });
    } catch (e) {
      const msg = e instanceof Error ? e.message : "设置可见性失败";
      setMenuError(msg);
    } finally {
      setMenuLoading(false);
    }
  };

  //删除处理
  const handleDelete = async (id: string) => {
    try {
      // 权限检查：必须登录才能操作
      if (!tokens?.accessToken) {
        setMenuError("请先登录");
        return;
      }
      if (!window.confirm("确认删除这篇知文吗？删除后不可恢复")) return;
      setMenuLoading(true);
      await deleteUsingDelete(id, tokens.accessToken);
      onChanged?.("delete");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "删除失败";
      setMenuError(msg);
    } finally {
      setMenuLoading(false);
    }
  };

  // 【卡片内容渲染】定义卡片的主要显示内容
  const content = (
    <>
      {/* 封面图片区域 */}
      {coverImage ? (
        <div className={styles.coverWrap}>
          <img
            className={styles.cover}
            src={coverImage}
            alt={title}
            loading="lazy"
          />
          {/* 播放标识：如果是视频等内容类型，显示播放按钮 */}
          {showPlayBadge ? (
            <div className={styles.playBadge}>
              <svg
                width="24"
                height="24"
                viewBox="0 0 16 16"
                fill="none"
                aria-hidden="true"
              >
                <polygon points="6,4 12,8 6,12" fill="currentColor" />
              </svg>
            </div>
          ) : null}
        </div>
      ) : null}

      {/* 主要内容区域 */}
      <div className={styles.content}>
        {/* 标题：支持搜索高亮显示 */}
        <h3 className={styles.title}>{title}</h3>
        {/* 摘要：只在有内容时显示，并支持关键词高亮 */}
        {summary.trim() ? (
          <p className={styles.description}>
            {renderEmHighlightedText(summary)}
          </p>
        ) : null}
        {/* 标签列表：显示知文的分类标签 */}
        {tags?.length ? (
          <div className={styles.tagGroups}>
            {tags.map((tag) => (
              <Tag key={tag}>#{tag}</Tag>
            ))}
          </div>
        ) : null}
      </div>

      {/* 元信息区域：作者信息和统计数据 */}
      <div className={styles.meta}>
        <div className={styles.teacher}>
          {/* 作者头像：优先显示图片，没有则显示首字母 */}
          {teacher.avatarUrl ? (
            <img
              className={styles.teacherAvatarImg}
              src={teacher.avatarUrl}
              alt={teacher.name}
            />
          ) : (
            <div className={styles.teacherAvatar}>
              {teacher.avatarText ?? (teacher.name?.charAt(0) || "?")}
            </div>
          )}
          {/* 作者信息：姓名和作者自定义标签 */}
          <div className={styles.teacherInfo}>
            <span className={styles.teacherName}>{teacher.name}</span>
            {authorTags?.length ? (
              <div className={styles.authorTags}>
                {authorTags.map((tag) => (
                  <span key={tag} className={styles.authorTag}>
                    #{tag}
                  </span>
                ))}
              </div>
            ) : null}
          </div>
        </div>
        {/* 统计数据：点赞数和浏览量（如果有footerExtra就不显示） */}
        {footerExtra ? null : (
          <div className={styles.stats}>
            {stats ? (
              <>
                <span className={styles.statItem}>
                  <HeartIcon width={16} height={16} strokeWidth={1.6} />
                  {stats.likes}
                </span>
                <span className={styles.statItem}>👁️ {stats.views}</span>
              </>
            ) : null}
          </div>
        )}
      </div>

      {/* 自定义底部内容：允许父组件传入额外的底部元素 */}
      {footerExtra ? (
        <div className={styles.footerExtra}>{footerExtra}</div>
      ) : null}
    </>
  );

  return (
    <article className={clsx(styles.card, className)}>
      {/* 置顶标识：根据详情数据或props显示置顶标签 */}
      {details?.isTop ?? isTop ? (
        <div className={styles.topBadge}>
          <span>置顶</span>
        </div>
      ) : null}

      {/* 编辑菜单：只有在editable为true时才显示 */}
      {editable ? (
        <>
          {/* 菜单触发按钮 */}
          <button
            ref={buttonRef}
            type="button"
            className={styles.menuButton}
            onClick={() => toggleMenu(id)}
            aria-haspopup="true"
            aria-expanded={menuOpen}
            title="编辑"
          >
            ⋯
          </button>
          {/* 菜单列表：只在菜单打开时显示 */}
          {menuOpen ? (
            <div ref={menuRef} className={styles.menuList} role="menu">
              {/* 错误信息显示 */}
              {menuError ? (
                <div style={{ color: "var(--color-danger)", padding: 6 }}>
                  {menuError}
                </div>
              ) : null}
              {/* 置顶/取消置顶按钮 */}
              <button
                type="button"
                className={styles.menuItem}
                onClick={() => handleSetTop(id, !details?.isTop)}
                disabled={menuLoading}
              >
                {details?.isTop ? "取消置顶" : "置顶"}
              </button>
              {/* 设为公开按钮 */}
              <button
                type="button"
                className={styles.menuItem}
                onClick={() => handleSetVisibility(id, "public")}
                disabled={menuLoading}
              >
                设为公开
              </button>
              {/* 设为私密按钮 */}
              <button
                type="button"
                className={styles.menuItem}
                onClick={() => handleSetVisibility(id, "private")}
                disabled={menuLoading}
              >
                设为私密
              </button>
              {/* 删除按钮（危险操作，特殊样式） */}
              <button
                type="button"
                className={clsx(styles.menuItem, styles.menuDanger)}
                onClick={() => handleDelete(id)}
                disabled={menuLoading}
              >
                删除
              </button>
            </div>
          ) : null}
        </>
      ) : null}
      {/* 内容包装：如果有跳转链接就用Link包裹，否则直接显示内容 */}
      {to ? <Link to={to}>{content}</Link> : content}
    </article>
  );
};

export default CourseCard;
