// Tag 组件 - 用于显示带样式的标签/徽章
// 这是一个可复用的UI组件，可以显示不同色调的标签

import clsx from "clsx"; // clsx 是一个条件性CSS类名拼接工具库
import styles from "./Tag.module.css"; // 导入对应的CSS模块样式

// 定义组件的props类型接口
type TagProps = {
    children: React.ReactNode; // 标签内显示的内容（可以是文本、图标等React元素）
    tone?: "primary" | "success" | "neutral"; // 标签的色调类型（可选，默认为primary）
};

// Tag组件实现
const Tag = ({children, tone = "primary"}: TagProps) => {
    // 使用clsx工具动态生成CSS类名
    // 基础类名styles.tag始终存在，根据tone属性条件性添加其他样式类
    const tagClass = clsx(styles.tag, {
        [styles.toneSuccess]: tone === "success", // 如果tone是success，添加success样式
        [styles.toneNeutral]: tone === "neutral"  // 如果tone是neutral，添加neutral样式
        // 注意：primary是默认样式，不需要额外的条件类名
    });

    // 返回一个span元素，应用计算出的CSS类名，并渲染子内容
    return <span className={tagClass}>{children}</span>;
};

export default Tag;