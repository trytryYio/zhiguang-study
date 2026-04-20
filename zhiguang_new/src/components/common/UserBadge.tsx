import styles from "./UserBadge.module.css";

/**
 * 用户头像 + 名字 + 别名
 */
type UserBadgeProps = {
  name: string;
  alias?: string; // 别名
  avatarUrl?: string;
};

// 获取首字母
const getInitial = (value: string) =>
  value.trim().charAt(0).toUpperCase() || "?";

const UserBadge = ({ name, alias, avatarUrl }: UserBadgeProps) => {
  return (
    <div className={styles.badge}>
      <div className={styles.avatar}>
        {avatarUrl ? (
          // 有地址 就生成图片
          <img src={avatarUrl} alt="avatar" className={styles.avatarImg} />
        ) : (
          // 无地址 就生成文字 作为头像
          getInitial(name)
        )}
      </div>
      <div className={styles.meta}>
        <span className={styles.name}>{name}</span>
        {alias ? <span className={styles.alias}>{alias}</span> : null}
      </div>
    </div>
  );
};

export default UserBadge;
