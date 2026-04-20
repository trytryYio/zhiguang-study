import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import UserBadge from "../../components/common/UserBadge";
import styles from "./AuthStatus.module.css";
const AuthStatus = () => {
  // 从认证上下文中获取用户信息、登出函数和加载状态
  const { user, logout, isLoading } = useAuth();

  // useNavigate用于获取导航函数，可以编程式地跳转页面
  const navigate = useNavigate();

  // useLocation用于获取当前路由的位置信息,包括路径、查询参数等
  const location = useLocation();

  //如果正在加载用户信息 则显示加载提示
  if (isLoading) {
    return (
      <div style={{ color: " var(--color-text-muted) ", fontSize: 13 }}>
        加载中...
      </div>
    );
  }
  //   如果用户未登录显示登录按钮
  if (!user) {
    return (
      <button
        type="button"
        className="ghost-button"
        onClick={() =>
          navigate("/login", {
            replace: false,
            state: {
              // 保存当前页面的路径，以便登录成功后跳转回该页面
              from: location.pathname + location.search + location.hash,
            },
          })
        }
      >
        登录
      </button>
    );
  }
  // 获取用户显示名称和头像URL
  const displayName = user.nickname || "用户";
  const avatarUrl = user.avatar || undefined;

  // 处理用户登出操作
  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error(退出失败, error);
    }
  };

  //   如果用户已经登录了 则显示用户信息和登出按钮
  return (
    <div className={styles.wrapper}>
      <UserBadge name={displayName} avatarUrl={avatarUrl} />
      <button
        type="button"
        className={styles.logoutButton}
        onClick={handleLogout}
      >
        退出
      </button>
    </div>
  );
};
export default AuthStatus;
