import { Children, type ReactNode } from "react";
import Sidebar from "./Sidebar";
import styles from "./AppLayout.module.css";

type AppLayoutProps = {
  header?: ReactNode;
  children: ReactNode;
  variant?: "default" | "cardless";
};

/**
 * 主布局组件：侧边栏 + 内容区。
 *
 * <p>variant="default" 时内容区带白色卡片背景和阴影；
 * variant="cardless" 时不带卡片外壳（如登录/注册页用）。</p>
 */
const AppLayout=({header,children,variant="default"} : AppLayoutProps)=>{
    return (
    <div className="app-shell">
            {/* 侧边栏 */}
            <Sidebar />
            {/* 内容区 */}
            <div className={styles.container}> 
                {header}
                <div className={variant==="default"? styles.pageCard : styles.main}>{children}</div>
            </div>
        </div>
    );


};
export default AppLayout;