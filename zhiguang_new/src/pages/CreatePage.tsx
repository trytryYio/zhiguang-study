import { useRef, useState } from "react";
import AppLayout from "../components/layout/AppLayout";
import MainHeader from "../components/layout/MainHeader";
import SectionHeader from "../components/common/SectionHeader";
import TagInput from "../components/common/TagInput";
import AuthStatus from "../features/auth/AuthStatus";
import { useAuth } from "../context/AuthContext";
import {
  createDraft,
  confirmContent,
  patchMetadata,
  publish,
} from "../api/knowPostController";
import {uploadToPresigned,computeSha256} from "../components/common/OssAndSha";
import styles from "./CreatePage.module.css";
import {presign} from "../api/storageController";
/**
* 创建页面 - 用于发布新内容
**/
const CreatePage = () => {
  const { tokens } = useAuth();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [visiblePublic, setVisiblePublic] = useState(true);
  const [summary, setSummary] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [postId, setPostId] = useState<string | null>(null);

  // ========== 图片上传相关状态 ==========
  // 文件输入框的引用，用于程序化触发文件选择
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  // 图片上传中状态
  const [imageUploading, setImageUploading] = useState(false);
  // 已上传图片的 URL 数组
  const [uploadedImgUrls, setUploadedImgUrls] = useState<string[]>([]);
  // 最大图片数量限制
  const MAX_IMAGES = 15;



  /**
   * 确保草稿存在，如果不存在则创建新草稿
   * @returns Promise<string> 返回草稿 ID
   *
   * 作用：避免重复创建草稿，实现"懒创建"模式
   */
  const ensureDraft = async (): Promise<string> => {
    // 如果已有草稿 ID，直接返回（幂等性保证）
    if (postId) return postId;

    // 调用后端 API 创建新草稿
    const resp = await createDraft();

    // 从响应中提取草稿 ID，转换为字符串类型
    const idStr = String(resp.id);

    // 更新本地状态，保存草稿 ID
    setPostId(idStr);

    // 显示成功消息
    setMessage(`草稿已创建：${idStr}`);

    // 返回草稿 ID 供后续使用
    return idStr;
  };


  /**
   * 处理图片选择和上传
   * @param files FileList 对象，包含用户选择的文件
   *
   * 上传流程：
   * 1. 确保草稿存在（获取 postId）
   * 2. 检查剩余可上传数量
   * 3. 为每个文件请求预签名 URL
   * 4. 直传 OSS（不经过后端服务器）
   * 5. 保存上传后的公开 URL
   */
  const handleSelectImages = async (files: FileList | null) => {
// 如果没有选择文件或文件列表为空，直接返回
    if (!files || files.length === 0) return;

    // 清除之前的错误和消息
    setError(null);
    setMessage(null);

    // 设置上传中状态，禁用上传按钮
    setImageUploading(true);

    try {
      // 第一步：确保草稿存在，获取 postId（上传需要关联到具体草稿）
      const id = await ensureDraft();

      // 计算剩余可上传的图片数量（最多 15 张）
      const remaining = Math.max(0, MAX_IMAGES - uploadedImgUrls.length);

      // 如果已达到上限，显示错误并返回
      if (remaining <= 0) {
        setError(`最多可选择 ${MAX_IMAGES} 张图片`);
        return;
      }

      // 将 FileList 转换为数组，便于操作
      const allSelected = Array.from(files);

      // 截取允许上传的数量（防止超出限制）
      const arr = allSelected.slice(0, remaining);

      // 遍历每个文件，逐个上传
      for (const f of arr) {
        // 使用正则表达式提取文件扩展名（如 .jpg, .png）
        const match = f.name.match(/\.[^.]+$/);

        // 如果没有扩展名，默认使用 .jpg
        const ext = match ? match[0] : ".jpg";

        // 获取文件的 MIME 类型，默认为 image/jpeg
        const contentType = f.type || "image/jpeg";

        // 请求预签名 URL（包含上传地址和必要的请求头）
        // scene: 场景标识，区分不同用途的上传
        // postId: 关联的草稿 ID
        // contentType: 文件类型
        // ext: 文件扩展名
        const presignResp = await presign({
          scene: "KnowPosts_image",
          postId: id,
          contentType,
          ext,
        });

        // 使用预签名 URL 直传文件到 OSS
        // putUrl: OSS 提供的临时上传地址
        // headers: 必需的请求头（如 Content-Type）
        // f: 要上传的文件对象
        await uploadToPresigned(presignResp.putUrl, presignResp.headers, f);

        // 从上传 URL 中提取公开访问地址（去除查询参数部分）
        // 例如：https://bucket.oss.com/image.jpg?signature=xxx → https://bucket.oss.com/image.jpg
        const publicUrl = presignResp.putUrl.split("?")[0];

        // 将公开 URL 添加到已上传列表
        // 使用函数式更新确保状态正确累加
        setUploadedImgUrls((prev) => [...prev, publicUrl]);
      }

      // 显示上传成功消息
      setMessage(`图片上传成功：${arr.length} 张`);
    } catch (err) {
      // 捕获上传过程中的错误
      // 如果是 Error 对象，使用其 message；否则使用默认消息
      const msg = err instanceof Error ? err.message : "图片上传失败";
      setError(msg);
    } finally {
      // 无论成功或失败，都重置上传中状态
      setImageUploading(false);
    }
  };
  /**
   * 处理发布操作
   *
   * 发布流程：
   * 1. 校验必填字段（标题、内容）
   * 2. 确保草稿存在
   * 3. 上传正文内容到 OSS（Markdown 格式）
   * 4. 计算文件哈希值（SHA-256）用于完整性校验
   * 5. 确认正文上传完成
   * 6. 更新元数据（标题、标签、图片、可见性等）
   * 7. 正式发布知文
   */
  const handlePublish = async () => {
    // 清除之前的消息和错误
    setMessage(null);
    setError(null);

    // 校验标题不能为空
    if (!title.trim()) {
      setError("请填写标题");
      return;
    }

    // 校验正文内容不能为空
    if (!content.trim()) {
      setError("请填写内容正文");
      return;
    }

    // 设置提交中状态，防止重复点击
    setSubmitting(true);

    try {
      // 第一步：确保草稿存在，获取 postId
      const id = await ensureDraft();

      // 第二步：将正文内容包装为 File 对象
      // 文件名：content.md
      // MIME 类型：text/markdown
      const file = new File([content], "content.md", { type: "text/markdown" });

      // 获取文件大小（字节）
      const size = file.size;

      // 计算文件的 SHA-256 哈希值，用于后端验证文件完整性
      const sha256 = await computeSha256(file);

      // 第三步：请求正文内容的预签名 URL
      const presignResp = await presign({
        scene: "KnowPosts_content",  // 场景标识：正文内容
        postId: id,                 // 关联的草稿 ID
        contentType: "text/markdown", // 文件类型
        ext: ".md",                 // 文件扩展名
      });

      // 使用预签名 URL 直传正文文件到 OSS，获取 ETag（实体标签，用于版本控制）
      const { etag } = await uploadToPresigned(presignResp.putUrl, presignResp.headers, file);

      // 第五步：确认正文上传完成
      // 告知后端文件已上传，并提供文件元信息用于校验
      await confirmContent({ id }, {
        objectKey: presignResp.objectKey,
        etag,
        size,
        sha256,
      });

      // 第六步：更新知文的元数据
      await patchMetadata({ id }, {
        title: title.trim(),
        tags: tags.length ? tags : undefined,
        imgUrls: uploadedImgUrls.length ? uploadedImgUrls : undefined,
        visible: visiblePublic ? "public" : "private",
        description: summary.trim() || undefined,
      });

      // 第七步：正式发布知文
      // 将草稿状态变更为已发布，对外可见
      await publish({ id });

      // 显示发布成功消息
      setMessage("发布成功！");
    } catch (err) {
      // 捕获发布过程中的错误
      const msg = err instanceof Error ? err.message : "发布失败";
      setError(msg);
    } finally {
      // 无论成功或失败，都重置提交中状态
      setSubmitting(false);
    }
  };
  // ========== JSX 渲染部分 ==========
  return(
  // 使用应用布局组件，包裹整个页面
    <AppLayout
      // 自定义头部区域
      header={
        <MainHeader
          // 主标题
          headline="创建新内容"
          // 副标题
          subtitle="分享你的知识，让更多人受益"
          // 右侧插槽：显示认证状态
          rightSlot={<AuthStatus />}
        />
      }
    >
      {/* 表单卡片容器 */}
      <div className={styles.formCard}>
        {/* 基本信息区块头部 */}
        <SectionHeader title="基本信息" subtitle="精准描述你的内容" />

        {/* 表单网格布局 */}
        <div className={styles.formGrid}>
          {/* 标题输入字段 */}
          <div className={styles.field}>
            {/* 标签：关联到 input 元素 */}
            <label className={styles.label} htmlFor="title">标题 *</label>
            {/* 标题输入框 */}
            <input
              id="title"                                    // 与 label 关联
              className={styles.input}                      // 应用样式
              placeholder="输入内容标题"                     // 占位符提示
              value={title}                                 // 绑定状态值
              onChange={(e) => setTitle(e.target.value)}    // 实时更新状态
            />
          </div>

          {/* 图片上传字段（占据整行） */}
          <div className={`${styles.field} ${styles.fullWidth}`}>
            {/* 标签 */}
            <label className={styles.label}>图片（多选） *</label>
            
            {/* 自定义上传按钮区域 */}
            <div
              className={styles.uploadBox}                  // 应用样式
              role="button"                                 // 无障碍角色：按钮
              tabIndex={0}                                  // 可通过 Tab 键聚焦
              onClick={() => fileInputRef.current?.click()} // 点击时触发隐藏的文件输入框
            >
              {/* 根据上传状态显示不同文本 */}
              <span>{imageUploading ? "正在上传…" : "点击上传图片"}</span>
              {/* 提示信息 */}
              <small>支持 JPG / PNG / SVG，最多 {MAX_IMAGES} 张</small>
              
              {/* 隐藏的原生文件输入框 */}
              <input
                ref={fileInputRef}                          // 引用此元素
                type="file"                                 // 文件类型
                accept="image/*"                            // 只接受图片文件
                multiple                                    // 允许多选
                style={{ display: "none" }}                 // 隐藏原生控件
                onChange={(e) => handleSelectImages(e.target.files)} // 文件选择后处理
              />
            </div>
            
            {/* 如果已上传图片，显示缩略图网格 */}
            {uploadedImgUrls.length > 0 ? (
              <div className={styles.thumbGrid}>
                {/* 遍历已上传的 URL，渲染缩略图 */}
                {uploadedImgUrls.map((url, idx) => (
                  <img key={idx} src={url} alt="" className={styles.thumb} />
                ))}
              </div>
            ) : null}
          </div>

          {/* 正文内容输入字段（占据整行） */}
          <div className={`${styles.field} ${styles.fullWidth}`}>
            <label className={styles.label} htmlFor="content">内容正文 *</label>
            {/* 多行文本输入框 */}
            <textarea
              id="content"                                  // 与 label 关联
              className={styles.textarea}                   // 应用样式
              placeholder="写下你的知识内容..."              // 占位符提示
              value={content}                               // 绑定状态值
              onChange={(e) => setContent(e.target.value)}  // 实时更新状态
            />
          </div>

          {/* 知识摘要输入字段（占据整行） */}
          <div className={`${styles.field} ${styles.fullWidth}`}>
            <label className={styles.label} htmlFor="summary">知识摘要</label>
            <textarea
              id="summary"                                  // 与 label 关联
              className={styles.textarea}                   // 应用样式
              placeholder="填写内容摘要（50字以内）"         // 占位符提示
              value={summary}                               // 绑定状态值
              onChange={(e) => setSummary(e.target.value)}  // 实时更新状态
            />
          </div>

          {/* 标签输入字段 */}
          <div className={styles.field}>
            <label className={styles.label}>标签</label>
            {/* 使用专门的标签输入组件 */}
            <TagInput value={tags} onChange={setTags} placeholder="输入标签后按回车" />
          </div>

          {/* 可见性切换字段（占据整行） */}
          <div className={`${styles.field} ${styles.fullWidth}`}>
            {/* 可点击的切换区域 */}
            <div
              className={styles.toggle}                     // 应用样式
              role="button"                                 // 无障碍角色：按钮
              tabIndex={0}                                  // 可通过 Tab 键聚焦
              onClick={() => setVisiblePublic((prev) => !prev)} // 切换可见性状态
            >
              {/* 左侧：文字说明 */}
              <div>
                <div className={styles.label}>可见范围</div>
                {/* 根据状态显示"公开"或"私密" */}
                <small>{visiblePublic ? "公开" : "私密"}</small>
              </div>
              
              {/* 右侧：开关视觉效果 */}
              <div className={`${styles.switch} ${visiblePublic ? styles.switchOn : ""}`} />
            </div>
          </div>
        </div>

        {/* 操作按钮区域 */}
        <div className={styles.actions}>
          {/* 发布按钮 */}
          <button 
            type="button"                                   // 防止表单默认提交行为
            className={styles.submit}                       // 应用样式
            onClick={handlePublish}                         // 点击时执行发布逻辑
            disabled={submitting}                           // 提交中时禁用按钮
          >
            {/* 根据状态显示不同文本 */}
            {submitting ? "发布中…" : "发布"}
          </button>
        </div>

        {/* 条件渲染：如果有错误，显示错误消息 */}
        {error ? <div className={styles.error}>{error}</div> : null}
        
        {/* 条件渲染：如果有成功消息，显示成功消息 */}
        {message ? <div className={styles.success}>{message}</div> : null}
      </div>
    </AppLayout>
  );
};

export default CreatePage;
