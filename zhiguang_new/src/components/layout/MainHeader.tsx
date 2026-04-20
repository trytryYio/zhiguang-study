import type { ReactNode } from "react";
import styles from "./MainHeader.module.css";

type MainHeaderProps = {
  headline: string;
  subtitle?: string;
  rightSlot?: ReactNode; //右侧插槽 -通常放操作按钮或图标。
  children?: ReactNode; // 组件内部嵌套的内容 -在标题下方显示额外内容（如筛选栏、标签等）。
};

/**
 * 页面头部：主标题 + 副标题 + 右侧插槽（通常放 AuthStatus）。
 *
 * <p>后续阶段可能增加 tabs 和 filters 插槽。</p>
 */
const MainHeader = ({
  headline,
  subtitle,
  rightSlot,
  children,
}: MainHeaderProps) => {
  return (
    <div className={styles.header}>
      <div>
        <h1 className={styles.headline}>{headline}</h1> 
        {/* 主标题 */}
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>} 
        {/* 副标题 */}
      </div>
      {rightSlot && <div className={styles.rightSlot}>{rightSlot}</div>}
      {children}
    </div>
  );
};

export default MainHeader;
