import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import type { ReactNode } from "react";
import { authService } from "@/services/authService";
import type {
  AuthenticatedUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  TokenResponse,
} from "@/types/auth";

type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
};

type AuthContextValue = {
  user: AuthenticatedUser | null;
  isLoading: boolean;
  tokens: AuthTokens | null;
  login: (payload: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<AuthenticatedUser>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
  reloadUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = "zhiguang_auth_tokens";
const USER_STORAGE_KEY = "zhiguang_current_user";

const readStoredTokens = (): AuthTokens | null => {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as AuthTokens;
    if (!parsed.accessToken || !parsed.refreshToken || !parsed.expiresAt)
      return null;
    return parsed;
  } catch {
    return null;
  }
};

const persistTokens = (tokens: AuthTokens | null) => {
  if (typeof window === "undefined") {
    return;
  }
  if (!tokens) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
};

const readStoredUser = (): AuthenticatedUser | null => {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as AuthenticatedUser;
    if (!parsed || typeof parsed !== "object") return null;
    return parsed;
  } catch {
    return null;
  }
};

const persistUser = (user: AuthenticatedUser | null) => {
  if (typeof window === "undefined") return;
  if (!user) {
    localStorage.removeItem(USER_STORAGE_KEY);
    return;
  }
  try {
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
  } catch {
    // ignore
  }
};

const parseInstantToMillis = (value: string): number => {
  const numeric = Number(value);
  if (!Number.isNaN(numeric)) {
    return numeric > 1e12 ? numeric : numeric * 1000;
  }
  const t = Date.parse(value);
  return Number.isNaN(t) ? Date.now() + 10 * 60 * 1000 : t;
};

const toTokens = (token: TokenResponse): AuthTokens => ({
  accessToken: token.accessToken,
  refreshToken: token.refreshToken,
  expiresAt: parseInstantToMillis(token.accessTokenExpiresAt),
});

type AuthProviderProps = {
  children: ReactNode;
};

/**
 * 认证提供者组件
 *
 * 提供全局的认证状态管理，包括用户信息、令牌管理、登录/注册/注销等功能。
 * 自动处理令牌的持久化存储、自动刷新和用户信息的同步。
 *
 * @param children - 子组件，将能够访问认证上下文
 * @returns 包含认证状态和操作的Provider组件
 */
export const AuthProvider = ({ children }: AuthProviderProps) => {
  // 从本地存储中读取并初始化认证令牌和用户信息
  const [tokens, setTokens] = useState<AuthTokens | null>(() =>
    readStoredTokens(),
  );
  const [user, setUser] = useState<AuthenticatedUser | null>(() =>
    readStoredUser(),
  );
  const [isLoading, setIsLoading] = useState<boolean>(!!tokens);
  const fetchingRef = useRef<Promise<void> | null>(null);

  /**
   * 获取当前用户信息
   *
   * 使用访问令牌从服务器获取当前用户的详细信息，并更新本地状态和持久化存储。
   * 如果获取失败，则清除所有认证状态。
   *
   * @param accessToken - 访问令牌，用于身份验证
   */
  const fetchUser = useCallback(async (accessToken: string) => {
    try {
      const profile = await authService.fetchCurrentUser(accessToken);
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
    if (!tokens) {
      setIsLoading(false);
      return;
    }

    if (!fetchingRef.current) {
      const task = fetchUser(tokens.accessToken).finally(() => {
        fetchingRef.current = null;
        setIsLoading(false);
      });
      fetchingRef.current = task;
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
  const login = useCallback(
    async (payload: LoginRequest) => {
      const response = await authService.login(payload);
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
  const register = useCallback(
    async (payload: RegisterRequest) => {
      const result = await authService.register(payload);
      // 注册成功后直接登录：写入令牌与用户信息
      const nextTokens = toTokens(result.token);
      setTokens(nextTokens);
      persistTokens(nextTokens);
      const userInfo = result.user as AuthenticatedUser;
      setUser(userInfo);
      persistUser(userInfo);
      // 为保证信息最新，拉取一次 /auth/me
      await fetchUser(nextTokens.accessToken);
      return userInfo;
    },
    [fetchUser],
  );

  /**
   * 用户注销
   *
   * 调用服务端注销接口（如果存在令牌），然后清除所有本地的认证状态和持久化数据。
   * 即使服务端注销失败，也会清除本地状态以确保用户已退出。
   */
  const logout = useCallback(async () => {
    if (tokens) {
      try {
        await authService.logout(
          { refreshToken: tokens.refreshToken },
          tokens.accessToken,
        );
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
  const refresh = useCallback(async () => {
    if (!tokens) return;

    // 如果有令牌
    try {
      // 检查当前时间是否小于过期时间减去5秒，如果未到刷新时间则直接返回
      if (Date.now() < tokens.expiresAt - 5_000) {
        return;
      }

      const result = await authService.refresh(tokens.refreshToken);
      const nextTokens = toTokens(result);
      setTokens(nextTokens);
      persistTokens(nextTokens);
      await fetchUser(nextTokens.accessToken);
    } catch (error) {
      console.error("刷新登录状态失败", error);
      await logout();
    }
  }, [tokens, fetchUser, logout]);

  /**
   * 重新加载用户信息
   *
   * 手动触发获取最新的用户信息，用于在用户资料可能发生变化时同步数据。
   */
  const reloadUser = useCallback(async () => {
    if (!tokens) return;
    await fetchUser(tokens.accessToken);
  }, [tokens, fetchUser]);

  // 设置定时任务，每分钟检查并刷新令牌
  useEffect(() => {
    if (!tokens) {
      return;
    }
    const timer = window.setInterval(() => {
      void refresh();
    });
    return () => window.clearInterval(timer);
  }, [tokens, refresh]);

  //  memoize认证上下文值，避免不必要的重渲染
  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      tokens,
      isLoading,
      login,
      register,
      logout,
      refresh,
      reloadUser,
    }),
    [user, tokens, isLoading, login, register, logout, refresh, reloadUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth 必须在 AuthProvider 内部使用");
  }
  return context;
};
