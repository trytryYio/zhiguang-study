import {useEffect, useRef, useState} from "react";
import type {ReactNode} from "react";
import clsx from "clsx";
import {Link} from "react-router-dom";
import Tag from "@/components/common/Tag";
import {HeartIcon} from "@/components/icons/Icon";
import {useAuth} from "@/context/AuthContext";
import {knowpostService} from "@/services/knowpostService";
import type {KnowpostDetailResponse, VisibleScope} from "@/types/knowpost";
import styles from "./CourseCard.module.css";

const renderEmHighlightedText = (text: string): ReactNode => {
    if (!text.includes("<em")) return text;

    const parts: ReactNode[] = [];
    const re = /<em(?:\s[^>]*)?>(.*?)<\/em>/gis;
    let lastIndex = 0;
    let match: RegExpExecArray | null;
    let key = 0;

    while ((match = re.exec(text)) !== null) {
        const start = match.index;
        if (start > lastIndex) parts.push(text.slice(lastIndex, start));
        parts.push(<em key={`em-${key++}`}>{match[1]}</em>);
        lastIndex = start + match[0].length;
    }

    if (lastIndex < text.length) parts.push(text.slice(lastIndex));
    return parts.length ? <>{parts}</> : text;
};

export type CourseCardProps = {
    id: string;
    title: string;
    summary: string;
    tags: string[];
    authorTags?: string[];
    isFree?: boolean;
    isTop?: boolean;
    teacher: {
        name: string;
        avatarText?: string;
        avatarUrl?: string;
    };
    stats?: {
        likes: number;
        views: number;
    };
    coverImage?: string;
    layout?: "vertical" | "horizontal";
    showPlayBadge?: boolean;
    footerExtra?: ReactNode;
    to?: string;
    className?: string;
    editable?: boolean;
    onChanged?: (action: "top" | "visibility" | "delete", payload?: unknown) => void;
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
                        onChanged
                    }: CourseCardProps) => {
    const {tokens} = useAuth();
    const [menuOpen, setMenuOpen] = useState(false);
    const [detail, setDetail] = useState<KnowpostDetailResponse | null>(null);
    const [menuLoading, setMenuLoading] = useState(false);
    const [menuError, setMenuError] = useState<string | null>(null);
    const buttonRef = useRef<HTMLButtonElement | null>(null);
    const menuRef = useRef<HTMLDivElement | null>(null);

    // 【数据加载函数】按需加载知文详情
    // 作用：当用户点击编辑菜单时，才去服务器获取完整的知文详情数据
    // 避免一开始就加载所有数据，节省网络资源和提高页面加载速度
    const loadDetailIfNeeded = async (id: string) => {
        // 守卫条件：如果已经加载过详情或者正在加载中，就直接返回，避免重复请求
        if (detail || menuLoading) return;
        try {
            setMenuLoading(true); // 显示加载状态
            // 调用服务层获取知文详情，传入访问令牌进行身份验证
            const d = await knowpostService.detail(id, tokens?.accessToken ?? undefined);
            setDetail(d); // 保存获取到的详情数据
        } catch (e) {
            // 错误处理：统一错误消息格式
            const msg = e instanceof Error ? e.message : "加载详情失败";
            setMenuError(msg); // 显示错误信息
        } finally {
            setMenuLoading(false); // 无论成功失败都要关闭加载状态
        }
    };

    // 【菜单切换函数】控制编辑菜单的显示和隐藏
    const toggleMenu = async (id: string) => {
        const next = !menuOpen; // 计算下一个状态（开/关）
        setMenuOpen(next); // 更新菜单状态
        if (next) {
            // 如果要打开菜单，就加载详情数据
            await loadDetailIfNeeded(id);
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

    // 【置顶操作处理】处理知文置顶/取消置顶功能
    const handleSetTop = async (id: string, isTop: boolean) => {
        try {
            // 权限检查：必须登录才能操作
            if (!tokens?.accessToken) {
                setMenuError("请先登录");
                return;
            }
            setMenuLoading(true); // 显示加载状态
            
            // 调用服务层设置置顶状态
            await knowpostService.setTop(id, isTop, tokens.accessToken);
            // 更新本地状态：保持UI与服务器数据同步
            setDetail(prev => prev ? {...prev, isTop} : prev);
            setMenuOpen(false); // 操作完成后关闭菜单
            // 通知父组件发生了置顶操作
            onChanged?.("top", {isTop});
        } catch (e) {
            const msg = e instanceof Error ? e.message : "设置置顶失败";
            setMenuError(msg);
        } finally {
            setMenuLoading(false);
        }
    };

    // 【可见性设置处理】处理知文公开/私密设置
    const handleSetVisibility = async (id: string, visible: VisibleScope) => {
        try {
            if (!tokens?.accessToken) {
                setMenuError("请先登录");
                return;
            }
            setMenuLoading(true);
            // 调用服务层设置可见性
            await knowpostService.setVisibility(id, visible, tokens.accessToken);
            // 更新本地状态
            setDetail(prev => prev ? {...prev, visible} : prev);
            setMenuOpen(false);
            // 通知父组件可见性变更
            onChanged?.("visibility", {visible});
        } catch (e) {
            const msg = e instanceof Error ? e.message : "设置可见性失败";
            setMenuError(msg);
        } finally {
            setMenuLoading(false);
        }
    };

    // 【删除操作处理】处理知文删除功能
    const handleDelete = async (id: string) => {
        try {
            if (!tokens?.accessToken) {
                setMenuError("请先登录");
                return;
            }
            // 删除前确认：防止误操作
            if (!window.confirm("确认删除这篇知文吗？删除后不可恢复")) return;
            setMenuLoading(true);
            // 调用服务层删除知文
            await knowpostService.remove(id, tokens.accessToken);
            setMenuOpen(false);
            // 通知父组件删除操作
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
                    <img className={styles.cover} src={coverImage} alt={title} loading="lazy"/>
                    {/* 播放标识：如果是视频等内容类型，显示播放按钮 */}
                    {showPlayBadge ? (
                        <div className={styles.playBadge}>
                            <svg width="24" height="24" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                                <polygon points="6,4 12,8 6,12" fill="currentColor"/>
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
                    <p className={styles.description}>{renderEmHighlightedText(summary)}</p>
                ) : null}
                {/* 标签列表：显示知文的分类标签 */}
                {tags?.length ? (
                    <div className={styles.tagGroups}>
                        {tags.map(tag => (
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
                        <img className={styles.teacherAvatarImg} src={teacher.avatarUrl} alt={teacher.name}/>
                    ) : (
                        <div
                            className={styles.teacherAvatar}>{teacher.avatarText ?? (teacher.name?.charAt(0) || "?")}</div>
                    )}
                    {/* 作者信息：姓名和作者自定义标签 */}
                    <div className={styles.teacherInfo}>
                        <span className={styles.teacherName}>{teacher.name}</span>
                        {authorTags?.length ? (
                            <div className={styles.authorTags}>
                                {authorTags.map(tag => (
                                    <span key={tag} className={styles.authorTag}>#{tag}</span>
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
                  <HeartIcon width={16} height={16} strokeWidth={1.6}/>
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

    // 【主渲染函数】组合所有元素并返回最终的卡片组件
    return (
        <article className={clsx(styles.card, className)}>
            {/* 置顶标识：根据详情数据或props显示置顶标签 */}
            {(detail?.isTop ?? isTop) ? (
                <div className={styles.topBadge}><span>置顶</span></div>
            ) : null}
            {/* 编辑菜单：只有在editable为true时才显示 */}
            {editable ? (
                <>
                    {/* 菜单触发按钮 */}
                    <button ref={buttonRef} type="button" className={styles.menuButton} onClick={() => toggleMenu(id)}
                            aria-haspopup="true" aria-expanded={menuOpen} title="编辑">
                        ⋯
                    </button>
                    {/* 菜单列表：只在菜单打开时显示 */}
                    {menuOpen ? (
                        <div ref={menuRef} className={styles.menuList} role="menu">
                            {/* 错误信息显示 */}
                            {menuError ?
                                <div style={{color: "var(--color-danger)", padding: 6}}>{menuError}</div> : null}
                            {/* 置顶/取消置顶按钮 */}
                            <button type="button" className={styles.menuItem}
                                    onClick={() => handleSetTop(id, !(detail?.isTop))} disabled={menuLoading}>
                                {detail?.isTop ? "取消置顶" : "置顶"}
                            </button>
                            {/* 设为公开按钮 */}
                            <button type="button" className={styles.menuItem}
                                    onClick={() => handleSetVisibility(id, "public")} disabled={menuLoading}>
                                设为公开
                            </button>
                            {/* 设为私密按钮 */}
                            <button type="button" className={styles.menuItem}
                                    onClick={() => handleSetVisibility(id, "private")} disabled={menuLoading}>
                                设为私密
                            </button>
                            {/* 删除按钮（危险操作，特殊样式） */}
                            <button type="button" className={clsx(styles.menuItem, styles.menuDanger)}
                                    onClick={() => handleDelete(id)} disabled={menuLoading}>
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
