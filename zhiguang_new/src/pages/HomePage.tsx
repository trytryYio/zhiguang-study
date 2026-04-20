import AppLayout from "@/components/layout/AppLayout";
import MainHeader from "@/components/layout/MainHeader";
import AuthStatus from "../features/auth/AuthStatus";

/**
 * 首页 — 第一阶段占位。
 *
 * <p>第二阶段接入 AuthContext 显示登录状态；
 * 第四阶段接入 Feed API 显示知文列表。</p>
 */
const HomePage = () => {
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
      cc
    </AppLayout>
  );
};

export default HomePage;
