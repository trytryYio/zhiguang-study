/**
 * 一个React认证上下文（Authentication Context），
 * 用于管理整个应用程序的用户认证状态。
 * 它是一个全局状态管理器，
 * 处理用户登录、注册、登出、令牌刷新等认证相关的所有逻辑。
 */

import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { register, login, me, logout, refresh } from "../api/authController";

// 定义认证令牌的类型
type AuthTokens = {
  accessToken: string; // 访问令牌，用于API认证
  refreshToken: string; // 刷新令牌，用于获取新的访问令牌
  expiresAt: number; // 访问令牌过期时间（毫秒时间戳）
};

// 定义认证上下文值的类型
type AuthContextValue = {
  user: API.AuthUserResponse | null; // 当前登录用户信息
  isLoading: boolean; // 是否正在加载认证状态
  tokens: AuthTokens | null; // 认证令牌
  login: (payload: API.LoginRequest) => Promise<void>; // 登录方法
  register: (payload: API.RegisterRequest) => Promise<API.AuthUserResponse>; // 注册方法
  logout: () => Promise<void>; // 登出方法
  refresh: () => Promise<void>; // 刷新令牌方法
  reloadUser: () => Promise<void>; // 重新加载用户信息方法
};

// 创建认证上下文

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// 定义本地存储的键名
const STORAGE_KEY = "zhiguang_auth_tokens";
const USER_STOREAGE_KEY = "zhiguang_current_user";

// 从本地存储读取令牌
const readStoredTokens = (): AuthTokens | null => {
  // 检查是否在浏览器环境
  if (typeof window === "undefined") {
    return null;
  }
  // 从本地存储中获取令牌
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as AuthTokens; // 类型转换
    //如果令牌不存在 或者 令牌的备用令牌不存在 或者令牌已过期 则返回null
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.expiresAt)
      return null;

    return parsed;
  } catch {
    return null;
  }
};
// 从本地存储读取用户信息
function readStoredUser(): API.AuthUserResponse | null {
  // 检查是否在浏览器环境
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const raw = localStorage.getItem(USER_STOREAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as API.AuthUserResponse;
    // 检查用户对象 是否为空 或者 用户对象 不是object对象
    if (!parsed || typeof parsed !== "object") return null;
    return parsed;
  } catch {
    return null;
  }
}

// 将令牌持久化到本地存储
const persistTokens = (tokens: AuthTokens | null) => {
  // 检查是否在浏览器环境
  if (typeof window === "undefined") {
    return null;
  }
  // 检查令牌对象 是否为空 ,是空的就在本地存储中删除
  if (!tokens) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  // 将令牌对象 转换为JSON字符串
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
};
// 将用户信息持久化到本地存储
const persistUser = (user: null) => {
  // 检查是否在浏览器环境
  if (typeof window === "undefined") {
    return null;
  }
  if (!user) {
    localStorage.removeItem(USER_STOREAGE_KEY);
    return;
  }
  try {
    localStorage.setItem(USER_STOREAGE_KEY, JSON.stringify(user));
  } catch (e: any) {
    console.log(e.message);
  }
};

// 将字符串格式的时间转换为毫秒时间戳
const parseInstantToMillis = (value: string): number => {
  // 尝试当作数字处理
  const numeric = Number(value);
  if (!Number.isNaN(numeric)) {
    // 如果能成功转成数字，就按时间戳处理

    // 如果数值大于 1e12 则返回毫秒时间戳，否则认为是秒时间戳
    return numeric > 1e12 ? numeric : numeric * 1000;
  }
  // 如果不是数字，就走后面的日期字符串解析逻辑
  const t = Date.parse(value);
  // / 输入："2024-01-15T10:30:00Z"
  Number.isNaN(t); // false
  // // 返回有效时间戳，如 1705312200000

  // 如果解析失败，返回10分钟后的时间
  // 如果无法解析过期时间 → 假设 Token 10分钟后过期
  return Number.isNaN(t) ? Date.now() + 10 * 60 * 1000 : t;
};

// 将API响应的令牌格式转换为内部使用的令牌格式
// 类型转换函数  接收一个参数 token，类型是后端生成的 API.TokenResponse
//                                  返回值类型是 AuthTokens
//                            （前端定义的认证令牌类型）
const toTokens = (token: API.TokenResponse): AuthTokens => ({
  accessToken: token.accessToken,
  refreshToken: token.refreshToken,
  expiresAt: parseInstantToMillis(token.accessTokenExpiresAt),
});

// 定义AuthProvider的属性类型
// 描述认证提供者组件
type AuthProviderProps = {
  children: ReactNode; // 子组件
};

// 认证上下文提供者组件

export const AuthProvider = ({ children }: AuthProviderProps) => {
  // 状态管理
  const [tokens, setTokens] = useState<AuthTokens | null>(() =>
    readStoredTokens(),
  ); // 认证令牌状态
  const [user, setUser] = useState<API.AuthUserResponse | null>(() =>
    readStoredUser(),
  ); // 用户信息状态

  const [isLoading, setIsLoading] = useState<boolean>(!!tokens); // 加载状态

  const fetchingRef = useRef<Promise<void> | null>(null); // 用于防止重复请求

  // 获取当前用户信息

  const fetchUser = useCallback(async (accessToken: string) => {
    try {
      const profile = await me(accessToken);
      setUser(profile);
      persistUser(profile);
    } catch (error) {
      console.error("获取用户信息失败", error);
      setUser(null);
      setTokens(null);
      persistTokens(null);
      persistUser(null);
    }
  }, []);

  // 当令牌变化时，自动获取用户信息
  useEffect(() => {
    //token不存在
    if (!tokens) {
      setIsLoading(false); //未登录
      return;
    }
    //token 存在
    if (!fetchingRef.current) {
      //确保只执行一次
      const task = fetchUser(tokens.accessToken).finally(() => {
        fetchingRef.current = null; // 请求完成后清空引用
        setIsLoading(false); // 关闭加载状态
      });
      fetchingRef.current = task; ///保存当前请求任务 也就是角色
    }
  }, [tokens, fetchUser]);

  /**
   * 用户登录
   *
   * 使用提供的登录凭据进行身份验证，成功后保存令牌和用户信息到状态和本地存储，
   * 并获取最新的用户资料。
   *
   * @param payload - 登录请求参数，包含用户名/邮箱和密码
   */
  const toLogin = useCallback(
    async (payload: API.LoginRequest) => {
      const response = await login(payload);
      const nextTokens = toTokens(response.token);
      setTokens(nextTokens);
      persistTokens(nextTokens);
      setUser(response.user);
      persistUser(response.user);
      await fetchUser(nextTokens.accessToken);
    },
    [fetchUser],
  );

  /**
   * 用户注册
   *
   * 创建新用户账户，注册成功后自动登录，保存令牌和用户信息，
   * 并获取最新的用户资料以确保数据同步。
   *
   * @param payload - 注册请求参数，包含用户名、邮箱、密码等信息
   * @returns 返回认证后的用户信息
   */
  const toRegister = useCallback(
    async (payload: API.RegisterRequest) => {
      const response = await register(payload);
      // 注册成功后直接登录：写入令牌与用户信息
      const nextTokens = toTokens(response.token);
      setTokens(nextTokens);
      persistTokens(nextTokens);
      setUser(response.user);
      persistUser(response.user);
      await fetchUser(nextTokens.accessToken);
    },
    [fetchUser],
  );

  /**
   * 用户注销
   *
   * 调用服务端注销接口（如果存在令牌），然后清除所有本地的认证状态和持久化数据。
   * 即使服务端注销失败，也会清除本地状态以确保用户已退出。
   */
  const toLogout = useCallback(async () => {
    if (tokens) {
      try {
        await logout({ refreshToken: tokens.refreshToken }, tokens.accessToken);
      } catch (error) {
        console.warn("注销请求失败，继续清除本地状态", error);
      }
    }
    setTokens(null);
    setUser(null);
    persistTokens(null);
    persistUser(null);
  }, [tokens]);

  /**
   * 刷新认证令牌
   *
   * 检查令牌是否即将过期（5秒内），如果是则使用刷新令牌获取新的访问令牌。
   * 如果刷新失败，则执行注销操作清除所有认证状态。
   */
  const refreshTokens = useCallback(async () => {
    if (!tokens) {
      return;
    }
    // 如果有令牌
    try {
      // 检查令牌时间是否过期
      if (Date.now() < tokens.expiresAt - 5000) {
        return;
      }
      // 调用认证服务刷新令牌

      const result = await refresh({ refreshToken: tokens.refreshToken });
      // 将刷新结果转换为令牌对象
      const nextTokens = toTokens(result);
      setTokens(nextTokens);
      persistTokens(nextTokens);
      await me();
      return result;
    } catch (error) {
      // 刷新失败时记录错误并执行登出操作
      console.error("刷新登录状态失败", error);
      await toLogout();
    }
  }, [tokens, fetchUser, toLogout]);

  /**
   * 重新加载用户信息
   *
   * 手动触发获取最新的用户信息，用于在用户资料可能发生变化时同步数据。
   */

  const reloadUserInfo = useCallback(async () => {
    if (!tokens) return;
    await me();
  }, [tokens, fetchUser]);

  // 设置定时任务，每分钟检查并刷新令牌
  useEffect(() => {
    if (!tokens) return;

    // 创建每分钟执行一次的定时器，用于检查并刷新认证令牌
    const timer = window.setInterval(() => {
      // 使用void关键字忽略异步函数的Promise返回值
      void refreshTokens();
    }, 60_000); // 每60秒执行一次

    // 清理函数：在useEffect依赖变更或组件卸载时清除定时器
    return () => window.clearInterval(timer);
  }, [tokens, refreshTokens]);

  //   memoize认证上下文值，避免不必要的重渲染
  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      tokens,
      isLoading,
      login: toLogin, // 明确指定映射关系
      register: toRegister, // 明确指定映射关系
      logout: toLogout, // 明确指定映射关系
      refresh: refreshTokens, // 明确指定映射关系
      reloadUser: reloadUserInfo, // 明确指定映射关系
    }),
    [
      user,
      tokens,
      isLoading,
      toLogin,
      toRegister,
      toLogout,
      refreshTokens,
      reloadUserInfo,
    ],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
/**
 * 角色权限
 * @returns
 */
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth 必须在 AuthProvider 内部使用");
  }
  return context;
};
