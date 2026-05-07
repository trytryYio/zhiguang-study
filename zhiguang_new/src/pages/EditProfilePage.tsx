import React, { useEffect, useMemo, useRef, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { patch, uploadAvatar } from "../api/profileController";
import { me } from "../api/authController";
import AppLayout from "../components/layout/AppLayout";
import AuthStatus from "../features/auth/AuthStatus";
import MainHeader from "../components/layout/MainHeader";
import SectionHeader from "../components/common/SectionHeader";
import styles from "./EditProfilePage.module.css";
import TagInput from "../components/common/TagInput";
/**
 * 编辑个人资料页面 - 用于修改用户信息
 **/
const EditProfilePage = () => {
  const { user, tokens, /* refresh, logout, */ reloadUser } = useAuth();
  const navigate = useNavigate();
  const displayName = useMemo(
    () => user?.nickname ?? user?.phone ?? "知光用户",
    [user],
  );

  // 表单字段状态：从 user 对象初始化，若无则为空字符串
  const [nickname, setNickname] = useState<string>(user?.nickname ?? "");
  // 简介文本输入，初始为空
  const [bio, setBio] = useState<string>(user?.bio ?? "");
  // ZG ID 文本输入，初始为空
  const [zgId, setZgId] = useState<string>(user?.zhId ?? "");
  // 性别文本输入（用户直接输入"男"/"女"），初始为空
  const [genderText, setGenderText] = useState<string>("");
  // 性别输入校验错误信息
  const [genderError, setGenderError] = useState<string>("");
  const [birthday, setBirthday] = useState<string>(user?.birthday ?? "");
  const [school, setSchool] = useState<string>(user?.school ?? "");
  const [phone, setPhone] = useState<string>(user?.phone ?? "");
  // 保存按钮加载状态
  const [isSaving, setIsSaving] = useState<boolean>(false);
  // 保存操作后的提示消息（成功/失败）
  const [saveMessage, setSaveMessage] = useState<string>("");
  // 头像上传加载状态
  const [uploading, setUploading] = useState<boolean>(false);
  // 当前头像 URL，优先使用 user.avatar，否则为 null
  const [avatarUrl, setAvatarUrl] = useState<string | null>(
    user?.avatar ?? null,
  );
  // 用于控制隐藏的文件上传 input 元素
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  // ... existing code ...

  //更新头像
  const onAvatarFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setSaveMessage("");
    try {
      const result = await uploadAvatar(file);
      setAvatarUrl(result.avatar || null);
      setSaveMessage("头像已更新");
      // 同步更新全局用户信息，刷新右上角头像
      try {
        await reloadUser?.();
      } catch {}
    } catch (error) {
      console.error(error);
      setSaveMessage("头像上传失败，请稍后重试");
    } finally {
      setUploading(false);
    }
  };

  // 点击头像上传按钮
  const onAvatarClick = () => {
    fileInputRef.current?.click();
  };

  //
  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); // 阻止表单提交
    setIsSaving(true);
    setSaveMessage("");
    const payload: API.ProfilePatchRequest = {};
    if (nickname.trim()) payload.nickname = nickname;
    if (bio.trim()) payload.bio = bio.trim();
    if (zgId.trim()) payload.zgId = zgId.trim();
    if (genderText === "男") {
      payload.gender = "MALE";
    } else if (genderText === "女") {
      payload.gender = "FEMALE";
    }
    if (birthday.trim()) payload.birthday = birthday.trim();
    if (school.trim()) payload.school = school.trim();
    // if (phone.trim()) payload.phone = phone.trim();
    try {
      await patch(payload);
      setSaveMessage("资料已保存");
      // 保存成功后，同步更新全局用户信息，返回“我的”页面可立即看到最新数据
      try {
        await reloadUser?.();
      } catch {}
    } catch (error) {
      console.error(error);
      setSaveMessage("保存失败，请稍后重试");
    } finally {
      setIsSaving(false);
    }
  };

  //在用户未上传头像时，作为头像区域的占位符显示：
  const avatarInitial = (displayName.trim().charAt(0) || "知").toUpperCase();

  // 进入页面时请求 auth/me，预填表单；带取消标记避免登出后回写旧数据
  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      try {
        if (!tokens?.accessToken) return;
        const current = await me(tokens.accessToken);
        if (cancelled) return;
        setNickname(current.nickname ?? "");
        setBio(current.bio ?? "");
        setZgId(current.zhId ?? "");
        setPhone(current.phone ?? "");
        setSchool(current.school ?? "");
        setBirthday(current.birthday ?? "");
        setAvatarUrl(current.avatar || null);
        if (current.gender === "MALE") setGenderText("男");
        else if (current.gender === "FEMALE") setGenderText("女");
        else setGenderText("");
        setGenderError("");
        if (typeof current.tagJson === "string") {
          try {
            const parsed = JSON.parse(current.tagJson);
            if (Array.isArray(parsed)) {
            }
          } catch (e) {
            console.warn("解析 tagJson 失败", e);
          }
        }
      } catch (error) {
        if (!cancelled) setSaveMessage("获取用户信息失败，请稍后重试");
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
    // 仅在进入页面时或令牌变化时触发
  }, [tokens?.accessToken]);

  // 退出登录时立刻清空本地个人信息状态，避免残留显示
  useEffect(() => {
    if (!tokens?.accessToken || !user) {
      setNickname("");
      setBio("");
      setZgId("");
      setGenderText("");
      setGenderError("");
      setBirthday("");
      setSchool("");
      setPhone("");
      setAvatarUrl(null);
    }
  }, [tokens?.accessToken, user]);

  return (
    <AppLayout
      variant="cardless"
      header={
        <MainHeader
          headline="编辑个人资料"
          subtitle="完善信息，帮助同学们更快认识你"
          rightSlot={<AuthStatus />}
        />
      }
    >
      <form className={styles.pageCard} onSubmit={onSubmit}>
        <SectionHeader
          title="基本信息"
          subtitle="头像、昵称、联系方式等"
          actions={
            <>
              <button
                type="button"
                className="ghost-button"
                onClick={() => navigate("/profile")}
              >
                返回
              </button>
              <button
                type="submit"
                className="ghost-button"
                disabled={isSaving}
              >
                {isSaving ? "保存中..." : "保存修改"}
              </button>
            </>
          }
        />

        <div className={styles.grid}>
          <div className={styles.avatarPanel}>
            <div
              className={styles.avatarPreview}
              onClick={onAvatarClick}
              role="button"
              aria-label="点击上传头像"
              tabIndex={0}
            >
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt="avatar"
                  className={styles.avatarImg}
                />
              ) : (
                <span>{avatarInitial}</span>
              )}
            </div>
            {/* 隐藏的文件输入，由点击头像触发 */}
            <input
              ref={fileInputRef}
              id="avatar"
              type="file"
              accept="image/*"
              onChange={onAvatarFileChange}
              style={{ display: "none" }}
            />
            {uploading ? (
              <span style={{ color: "var(--color-text-muted)" }}>
                上传中...
              </span>
            ) : null}
          </div>

          <div className={styles.fieldGroup}>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="nickname">
                昵称
              </label>
              <input
                id="nickname"
                className={styles.input}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
                placeholder="填写你的昵称"
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="phone">
                手机
              </label>
              <input
                id="phone"
                className={styles.input}
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="绑定手机号方便联系"
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="zgId">
                知光 ID
              </label>
              <input
                id="zgId"
                className={styles.input}
                value={zgId}
                onChange={(e) => setZgId(e.target.value)}
                placeholder="用于个性化主页地址"
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="gender">
                性别
              </label>
              <input
                id="gender"
                className={styles.input}
                value={genderText}
                onChange={(e) => {
                  const val = e.target.value.trim();
                  setGenderText(val);
                  if (!val || val === "男" || val === "女") {
                    setGenderError("");
                  } else {
                    setGenderError("性别仅支持“男”或“女”");
                  }
                }}
                placeholder="请输入 男 或 女"
              />
              {genderError ? (
                <span className={styles.errorMessage}>{genderError}</span>
              ) : null}
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="birthday">
                生日
              </label>
              <input
                id="birthday"
                className={styles.input}
                type="date"
                value={birthday}
                onChange={(e) => setBirthday(e.target.value)}
              />
            </div>
            <div className={styles.field}>
              <label className={styles.label} htmlFor="school">
                学校/机构
              </label>
              <input
                id="school"
                className={styles.input}
                value={school}
                onChange={(e) => setSchool(e.target.value)}
                placeholder="填写你的所在学校或机构"
              />
            </div>
            <div className={`${styles.field} ${styles.fullWidth}`}>
              <label className={styles.label} htmlFor="skills">
                擅长领域
              </label>
            </div>
            <div className={`${styles.field} ${styles.fullWidth}`}>
              <label className={styles.label} htmlFor="bio">
                个人简介
              </label>
              <textarea
                id="bio"
                className={styles.textarea}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="介绍一下自己..."
              />
            </div>
          </div>
        </div>

        <div className={styles.actions}>
          {saveMessage ? (
            <span style={{ color: "var(--color-primary-strong)" }}>
              {saveMessage}
            </span>
          ) : null}
        </div>
      </form>
    </AppLayout>
  );
};

export default EditProfilePage;
