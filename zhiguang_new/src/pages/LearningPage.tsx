// React Hooks：useRef 用于引用可变对象（如 EventSource），useState 用于声明响应式状态
import { useRef, useState } from "react";
// 应用整体布局组件，提供统一的页面结构（侧边栏、顶栏、主内容区）
import AppLayout from "../components/layout/AppLayout";
// 顶部导航栏组件，显示标题、副标题和右侧插槽
import MainHeader from "../components/layout/MainHeader";
// 区块标题组件，显示区块的标题和副标题
import SectionHeader from "../components/common/SectionHeader";
// 认证状态组件，显示当前用户的登录状态（头像/登录按钮）
import AuthStatus from "../features/auth/AuthStatus";
// 当前页面的 CSS 模块样式，通过 styles.xxx 引用类名
import styles from "./LearningPage.module.css";

/**
 * LearningPage - AI 学习助手页面
 *
 * 功能：基于 RAG（检索增强生成）的知识问答
 * 用户输入知文 ID 和问题，系统从向量数据库中检索相关片段，
 * 然后通过 SSE（Server-Sent Events）流式返回 AI 生成的回答。
 */
const LearningPage = () => {
  // ========== 状态定义 ==========

  // postId：用户输入的知文 ID，用于定位要问答的知文
  const [postId, setPostId] = useState("");
  // question：用户输入的问题文本
  const [question, setQuestion] = useState("");
  // answer：AI 生成的回答（流式拼接，每次 SSE 推送追加一段）
  const [answer, setAnswer] = useState("");
  // loading：是否正在生成中，控制按钮禁用状态和加载提示
  const [loading, setLoading] = useState(false);
  // error：错误信息，非 null 时显示错误提示
  const [error, setError] = useState<string | null>(null);
  // esRef：通过 useRef 保存 EventSource 实例的引用
  // 为什么用 useRef 而不是 useState？因为关闭连接不需要触发重新渲染
  const esRef = useRef<EventSource | null>(null);

  /**
   * startRag - 发起 RAG 问答请求
   *
   * 流程：
   * 1. 校验输入（知文 ID 和问题不能为空）
   * 2. 关闭之前的 SSE 连接（避免重复连接）
   * 3. 构建 SSE 请求 URL，创建 EventSource 连接
   * 4. 监听 onmessage：每次服务器推送一段文本，追加到 answer
   * 5. 监听 onerror：连接断开或出错时，停止加载状态
   */
  const startRag = () => {
    // 去除问题和知文 ID 前后的空格
    const q = question.trim();
    const pid = postId.trim();
    // 校验：两者都不能为空
    if (!q || !pid) {
      setError("请输入知文 ID 和问题");
      return;
    }
    // 清除之前的错误和回答，准备新的问答
    setError(null);
    setAnswer("");

    // 关闭之前的 SSE 连接（如果存在）
    // esRef.current 持有上一次 EventSource 的引用
    if (esRef.current) {
      // try-catch 防止 close() 抛异常（如连接已断开）
      try { esRef.current.close(); } catch {}
      // 置空引用，表示当前没有活跃连接
      esRef.current = null;
    }

    // 构建 SSE 请求 URL
    // /api/v1/knowposts/{pid}/qa/stream 是后端 RAG 流式问答接口
    // question：用户问题（URL 编码防止特殊字符破坏 URL）
    // topK=5：从向量数据库检索最相关的 5 个文本片段
    // maxTokens=1024：限制 AI 生成的最大 token 数
    const url = `/api/v1/knowposts/${pid}/qa/stream?question=${encodeURIComponent(q)}&topK=5&maxTokens=1024`;

    // 创建 EventSource 连接，开始接收 SSE 流
    // EventSource 是浏览器原生 API，专门用于接收服务器推送事件
    // 它会自动处理连接、重连和数据接收
    const es = new EventSource(url);
    // 保存引用，以便后续手动关闭连接
    esRef.current = es;
    // 设置加载状态为 true，UI 显示"生成中…"
    setLoading(true);

    // 监听服务器推送的消息事件
    // 每当 AI 生成一段文本，后端就通过 SSE 推送一个 message 事件
    // e.data 是服务器发送的文本片段
    es.onmessage = (e) => {
      // 使用函数式更新：prev 是当前 answer 值，追加新片段
      // ?? 是空值合并运算符：如果 e.data 为 null/undefined，用空字符串
      setAnswer((prev) => prev + (e.data ?? ""));
    };

    // 监听错误事件（连接断开、网络错误、服务器错误等）
    es.onerror = () => {
      // 停止加载状态，UI 停止显示"生成中…"
      setLoading(false);
      // 关闭 EventSource 连接，释放资源
      try { es.close(); } catch {}
      // 置空引用
      esRef.current = null;
    };
  };

  /**
   * stopRag - 手动停止 AI 生成
   *
   * 用户点击"停止"按钮时调用，主动关闭 SSE 连接，
   * 服务器会停止生成，answer 保留已生成的部分内容。
   */
  const stopRag = () => {
    // 检查是否有活跃的 SSE 连接
    if (esRef.current) {
      try { esRef.current.close(); } catch {}
      esRef.current = null;
    }
    // 停止加载状态
    setLoading(false);
  };

  // ========== 渲染 JSX ==========
  return (
    // AppLayout：最外层布局容器
    // header 属性：传入顶部导航栏
    //   headline：主标题
    //   subtitle：副标题
    //   rightSlot：右侧插槽，放置认证状态组件
    <AppLayout
      header={
        <MainHeader
          headline="AI 学习助手"
          subtitle="基于 RAG 的知识问答，围绕知文内容提问"
          rightSlot={<AuthStatus />}
        />
      }
    >
      {/* 学习卡片容器 */}
      <div className={styles.learningCard}>
        {/* 区块标题：知识问答 */}
        <SectionHeader title="知识问答" subtitle="输入知文 ID，向 AI 提问" />

        {/* 知文 ID 输入字段 */}
        <div className={styles.field}>
          {/* label 的 htmlFor 与 input 的 id 关联，点击 label 会聚焦 input */}
          <label className={styles.label} htmlFor="postId">知文 ID *</label>
          <input
            id="postId"
            className={styles.input}
            value={postId}                                    // 受控组件：值由 React state 控制
            onChange={(e) => setPostId(e.target.value)}       // 输入时更新 state
            placeholder="请输入知文 ID"
          />
        </div>

        {/* 问题输入字段 */}
        <div className={styles.field}>
          <label className={styles.label} htmlFor="question">你的问题 *</label>
          <textarea
            id="question"
            className={styles.textarea}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="例如：这篇知文的核心观点是什么？"
          />
        </div>

        {/* 操作按钮区域 */}
        <div className={styles.actions}>
          {/* 发送按钮：loading 时或输入为空时禁用 */}
          <button
            type="button"
            className={styles.submitBtn}
            onClick={startRag}
            disabled={loading || !question.trim() || !postId.trim()}
          >
            {/* 根据 loading 状态显示不同文本 */}
            {loading ? "生成中…" : "发送"}
          </button>
          {/* 停止按钮：只有 loading 时才可点击 */}
          <button
            type="button"
            className={styles.stopBtn}
            onClick={stopRag}
            disabled={!loading}
          >
            停止
          </button>
        </div>

        {/* 错误提示：error 非 null 时显示 */}
        {error ? <div className={styles.error}>{error}</div> : null}

        {/* AI 回答展示区域 */}
        <div className={styles.answerBox}>
          {/* 回答区域标题 */}
          <div className={styles.answerLabel}>AI 回答</div>
          {/* 有回答内容时显示回答，否则显示占位提示 */}
          {answer ? (
            // answerContent：纯文本展示 AI 的流式回答
            <div className={styles.answerContent}>{answer}</div>
          ) : (
            // 占位提示：loading 时显示"等待生成…"，否则显示默认提示
            <div className={styles.placeholder}>
              {loading ? "等待生成…" : "这里将展示答案（支持流式输出）"}
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  );
};

export default LearningPage;
