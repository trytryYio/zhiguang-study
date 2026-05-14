import {useParams} from "react-router-dom";
import {useAuth} from "../context/AuthContext";
import {useEffect, useRef, useState} from "react";
import {detail} from "../api/knowPostController";
import styles from "./CourseDetailPage.module.css"
import remarkGfm from "remark-gfm";
import Tag from "../components/common/Tag";
import SectionHeader from "../components/common/SectionHeader";
import AppLayout from "../components/layout/AppLayout";
import MainHeader from "../components/layout/MainHeader";
import AuthStatus from "../features/auth/AuthStatus";
import LikeFavBar from "../components/common/LikeFavBar";
import ReactMarkdown from "react-markdown";
/**
 * 知文详情页面 - 显示单个知文的详细信息
 **/
const CourseDetailPage = () => {
  const {id} = useParams(); // 从URL参数中获取知文ID'
  // 获取认证信息
  const {tokens, user} = useAuth();
  // 定义状态变量 useState  当这个变量的值发生变化时，
  // React 会自动检测到变化，并立即更新页面上所有使用了该变量的地方
  const [detailData, setDetailData] = useState<any>(null);
  const [contentText, setContentText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [contentError, setContentError] = useState<string | null>(null);


  // RAG 问答状态
  const [ragQuestion, setRagQuestion] = useState("");
  const [ragAnswer, setRagAnswer] = useState("");
  const [ragLoading, setRagLoading] = useState(false);
  const [ragError, setRagError] = useState<string | null>(null);
  // 使用 useRef 来存储 EventSource 对象的引用，以便在组件卸载时正确关闭连接
  //useRef 值变化不会触发重新渲染
  const ragESRef = useRef<EventSource | null>(null);

  // 图片预览状态
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewIndex, setPreviewIndex] = useState(0);

  // 加载详情
  useEffect(() => {
    // cancelled 是一个标志位，用于防止组件卸载后仍然更新状态（避免内存泄漏）
    // 这是 React 中处理异步操作的标准做法
    let cancelled = false;

    // 定义一个异步函数来执行数据加载逻辑
    // async 关键字表示这个函数会返回一个 Promise
    const run = async () => {
      // 如果 id 不存在（null、undefined 或空字符串），直接返回，不执行后续逻辑
      if (!id) return;

      // 清除之前的错误状态，准备重新加载
      setError(null);
      try {
        // 调用 detail API 获取知文详情
        // Number(id) 将 id 转换为数字类型（因为 URL 参数通常是字符串）
        // await 等待异步请求完成，resp 是响应对象
        const resp = await detail({id:id});

        // 如果组件已经卸载（cancelled 为 true），则不再更新状态
        // 这可以防止在组件卸载后调用 setState 导致的警告
        if (cancelled) return;

        // resp 可能为 null（后端返回 data: null 时），需要做空值保护
        if (!resp) {
          setError("知文数据不存在");
          return;
        }

        // 更新详情数据状态
        // resp.data 包含从后端返回的知文详细信息
        setDetailData(resp);

        // 重置当前激活的图片索引为 0（显示第一张图片）
        setPreviewIndex(0);

        // 检查响应数据中是否有内容 URL
        // 可选链操作符 ?. 确保在 resp.data 为 null/undefined 时不会报错
        if (resp?.contentUrl) {
          try {
            // 使用 fetch API 获取正文内容
            // credentials: "omit" 表示不发送 Cookie 等认证信息（跨域请求时的安全设置）
            const text = await fetch(resp.contentUrl, {credentials: "omit"}).then((r) => {
              // r 是 Response 对象
              // 如果 HTTP 状态码不是 2xx（如 404、500），抛出错误
              if (!r.ok) throw new Error(`HTTP ${r.status}`);
              // 将响应体解析为文本格式并返回
              return r.text();

            });
            // 再次检查组件是否已卸载
            if (!cancelled) {
              //未卸载
              // 更新正文内容状态
              setContentText(text);

              // 清除之前的内容错误（如果有）
              setContentError(null);
            }
          } catch {
            // 如果获取正文失败（网络错误、跨域限制等）
            // 这里的 catch 没有参数，因为我们不需要具体的错误信息
            if (!cancelled) {
              // 设置友好的错误提示信息
              setContentError("正文暂不可读，可能为非公开或跨域受限");
            }

          }
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : "加载失败";
        // 只有在组件未卸载时才更新错误状态
        if (!cancelled) setError(msg)
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [id, tokens?.accessToken]);

// RAG 流式问答
  const startRag = () => {
    //如果没有认证信息 直接返回
    if (!id) return
    // 获取用户输入的问题，并去除首尾空格
    // trim() 方法会移除字符串两端的空白字符
    var q = ragQuestion.trim();
    // 如果问题为空（用户未输入或只输入了空格），直接返回
    if (!q) return
    // 清除之前的错误状态，准备开始新的问答
    setRagError(null);

    // 清空之前的答案，为新答案做准备
    setRagAnswer("");
    // 检查是否已有活跃的 EventSource 连接
    // ragESRef.current 存储了当前 SSE 连接的引用
    if (ragESRef.current) {
      try {
        // 关闭之前的连接，避免多个连接同时存在造成资源浪费
        ragESRef.current.close();
      } catch {  // 如果关闭失败（例如连接已断开），忽略错误
      }
      // 将引用设置为 null，表示没有活跃连接
      ragESRef.current = null;
    }
    // 构建 SSE（Server-Sent Events）请求 URL
    // encodeURIComponent 对问题进行 URL 编码，防止特殊字符导致请求失败
    // topK=5: 从向量数据库中检索最相关的 5 个片段
    // maxTokens=1024: 限制 AI 生成的最大 token 数量
    const url = `/api/v1/knowposts/${id}/qa/stream?question=${encodeURIComponent(q)}&topK=5&maxTokens=1024`;

// 创建 EventSource 对象，建立与服务器的 SSE 连接
    // EventSource 会自动处理连接、重连和数据接收
    const es = new EventSource(url);
// 将 EventSource 实例保存到 ref 中，方便后续关闭连接
    ragESRef.current = es;
// 设置加载状态为 true，UI 上显示"生成中…"
    setRagLoading(true);
    // 监听服务器推送的消息事件
    // 每当服务器发送一个新片段，这个回调就会被触发
    es.onmessage = (e) => {
      // e.data 包含服务器发送的文本片段
      // 使用函数式更新 prev + (e.data ?? "") 确保状态更新的准确性
      // ?? 是空值合并操作符，如果 e.data 为 null/undefined，则使用空字符串
      setRagAnswer((prev) => prev + (e.data ?? ""));
    };

    // 监听错误事件（连接断开、网络错误等）
    es.onerror = () => {
      // 设置加载状态为 false，UI 上停止显示"生成中…"
      setRagLoading(false);

      try {
        // 尝试关闭 EventSource 连接
        es.close();
      } catch {
        // 如果关闭失败，忽略错误
      }

      // 清除 ref 中的引用，表示连接已结束
      ragESRef.current = null;
    };

  }

// RAG 流式问答 - 手动停止生成
  const stopRag = () => {
    // 检查是否有活跃的 EventSource 连接
    if (ragESRef.current) {
      try {
        // 主动关闭 SSE 连接，停止接收服务器的数据
        ragESRef.current.close();
      } catch {
        // 如果关闭失败（例如连接已断开），忽略错误
      }
      // 清除 ref 中的引用
      ragESRef.current = null;
    }

    // 设置加载状态为 false，UI 上停止显示"生成中…"
    setRagLoading(false);
  };

  //清理sse 链接
  useEffect(() => {
    //触发时机：用户离开页面或组件被销毁时
    return () => {
      if (ragESRef.current) {
        try {
          ragESRef.current.close();
        } catch {
        }
        ragESRef.current = null;
      }
    };
  }, []);

//图片切换
  const prevImage = () => {
    if (!detailData?.image?.length) return
    // 点击次数:  0    1    2    3    4    5    6    7
    // 索引变化:  0 → 1 → 2 → 3 → 4 → 0 → 1 → 2
    // (首)                    (循环)
    setPreviewIndex((i) => (i + 1) % detailData.image.length);

  };

  const nextImage = () => {
    if (!detailData?.images?.length) return;
    setPreviewIndex((i) => (i + 1) % detailData.images.length);
  };

  return (
    // AppLayout (最外层布局容器)
    // └── MainHeader (顶部导航栏)
    //     └── article.detailCard (主内容卡片)
    //         ├── 错误提示区域
    //         ├── 图片列表区域
    //         ├── 标题信息区域
    //         ├── 内容正文 + RAG问答 (双栏布局)
    //         └── 图片预览弹窗 (条件渲染)
    <AppLayout
      header={<MainHeader headline={detailData?.title ?? ""} rightSlot={<AuthStatus/>}/>}
      variant="cardless"
    >
      <article className={styles.detailCard}>
        {/*错误提示区域*/}
        {error ? <div style={{color: "var(--color-danger)"}}>{error}</div> : null}
        {/* 图片列表 */}
        {detailData?.images?.length ? (
          <div className={styles.imageRow}>
            {/*.slice(0, 6)截取前 6 张图片*/}
            {/*map()：数组遍历方法，将每个元素转换为 JSX 元素
参数说明：
src: string：当前元素的值（图片 URL）
idx: number：当前元素的索引（0, 1, 2, ...）*/}

            {detailData.images.slice(0, 6).map((src: string, idx: number) => (
              // 单个图片项容器
              <div
                key={src + idx}
                className={styles.imageItem}
                onClick={() => {
                  setPreviewIndex(idx);
                  setPreviewOpen(true);
                }}
              >  <img className={styles.image} src={src} alt={detailData.title} />
              </div>
            ))}
          </div>
            ): null}

        {/*标题信息区域*/}
        <div className={styles.titleBlock}>
          <div className={styles.tagList}>
            {(detailData?.tags ?? []).map((tag: string) => (
              <Tag key={tag}>#{tag}</Tag>
            ))}
          </div>
          <div className={styles.meta}>
            {detailData?.authorAvatar ? (
              <img className={styles.authorAvatar} src={detailData.authorAvatar} alt={detailData.authorNickname} />
            ) : null}
            <span className={styles.authorName}>{detailData?.authorNickname ?? ""}</span>
            {detailData?.publishTime ? (
              <span>{new Date(detailData.publishTime).toLocaleDateString("zh-CN")}</span>
            ) : null}
          </div>
          <div className={styles.bottomBar}>
            {detailData ? (
              <LikeFavBar
                entityId={detailData.id}
                initialCounts={{ like: detailData.likeCount ?? 0, fav: detailData.favoriteCount ?? 0 }}
                initialState={{ liked: detailData.liked, faved: detailData.faved }}
              />
            ) : null}
          </div>
        </div>

        <SectionHeader title="内容正文" subtitle="" />

        <div className={styles.contentRow}>
          <div className={styles.contentMain}>
            <div className={`${styles.body} ${styles.markdown}`}>
              {contentText ? (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    a: ({ node, ...props }) => <a {...props} target="_blank" rel="noreferrer" />,
                    img: ({ node, ...props }) => <img {...props} style={{ maxWidth: "100%", borderRadius: 12 }} />,
                  }}
                >
                  {contentText}
                </ReactMarkdown>
              ) : (
                "暂无内容"
              )}
            </div>
            {contentError ? (
              <div style={{ color: "var(--color-danger)" }}>{contentError}</div>
            ) : null}
          </div>

          {/* RAG 问答面板 */}
          <aside className={styles.ragPanel}>
            <div className={styles.ragBody}>
              <textarea
                className={styles.ragTextarea}
                placeholder="围绕本知文提问，例如：这篇知文的核心观点是什么？"
                value={ragQuestion}
                onChange={(e) => setRagQuestion(e.target.value)}
              />
              <div className={styles.ragControls}>
                <button
                  type="button"
                  className={`${styles.ragBtn} ${styles.ragBtnPrimary}`}
                  onClick={startRag}
                  disabled={ragLoading || !ragQuestion.trim()}
                >
                  {ragLoading ? "生成中…" : "发送"}
                </button>
                <button
                  type="button"
                  className={`${styles.ragBtn} ${styles.ragBtnGhost}`}
                  onClick={stopRag}
                  disabled={!ragLoading}
                >
                  停止
                </button>
              </div>
              <div className={styles.ragHint}>
                说明：仅"公开"知文支持问答，答案基于当前知文的索引片段实时生成。
              </div>
              {ragError ? <div style={{ color: "var(--color-danger)" }}>{ragError}</div> : null}
              <div className={styles.ragAnswer}>
                {ragAnswer ? (
                  <div className={styles.markdown}>
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{ragAnswer}</ReactMarkdown>
                  </div>
                ) : (
                  <div className={styles.ragPlaceholder}>
                    {ragLoading ? "等待生成…" : "这里将展示答案（支持流式）"}
                  </div>
                )}
              </div>
            </div>
          </aside>
        </div>

        {/* 图片预览弹窗 */}
        {previewOpen && detailData?.images?.length ? (
          <div className={styles.previewOverlay} onClick={() => setPreviewOpen(false)}>
            <div className={styles.previewBox} onClick={(e) => e.stopPropagation()}>
              <img className={styles.previewImage} src={detailData.images[previewIndex]} alt={detailData.title} />
              <button type="button" className={styles.navButtonLeft} onClick={prevImage}>‹</button>
              <button type="button" className={styles.navButtonRight} onClick={nextImage}>›</button>
              <button type="button" className={styles.closeButton} onClick={() => setPreviewOpen(false)}>✕</button>
            </div>
          </div>
        ) : null}
      </article>
    </AppLayout>
  );
};


export default CourseDetailPage;
