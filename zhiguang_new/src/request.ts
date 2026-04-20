
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios';

// 获取 API 基础地址
const getBaseUrl = () => {
  const envBase = import.meta.env.VITE_BASE_URL as string | undefined;
  return envBase?.replace(/\/$/, "") ?? "";
};

// 从 localStorage 读取 accessToken
const getStoredToken = (): string | null => {
  try {
    const raw = localStorage.getItem('zhiguang_auth_tokens');
    if (!raw) return null;
    return (JSON.parse(raw) as { accessToken?: string }).accessToken ?? null;
  } catch {
    return null;
  }
};

// 创建 Axios 实例
const myAxios: AxiosInstance = axios.create({
  baseURL: getBaseUrl(),
  timeout: 60000,
  withCredentials: true,
});

// 请求拦截器 - 自动添加 token
myAxios.interceptors.request.use(
  function (config) {
    const token = getStoredToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  function (error) {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一处理响应结构
myAxios.interceptors.response.use(
  function (response) {
    // 直接返回 data 中的数据，避免前端每次都写 .data.data
    return response.data.data;
  },
  function (error) {
    // 处理错误响应
    if (error.response?.data?.message) {
      console.error('请求错误:', error.response.data.message);
    }

    // 未登录处理
    if (error.response?.data?.code === 40100) {
      // 跳转到登录页
      if (!window.location.pathname.includes('/login')) {
        alert('请先登录'); // 或使用 message.warning
        window.location.href = `/login?redirect=${window.location.href}`;
      }
    }

    return Promise.reject(error);
  }
);

/**
 * 发送请求的通用函数
 * - 拼接完整 URL
 * - 自动从 localStorage 拿 Token，塞进 `Authorization: Bearer xxx`
 * - 发送 axios 请求
 * - 处理错误（非 2xx 抛异常）
 * - 统一处理后端响应结构，直接返回真正的数据
 */
async function request<T>(url: string, options: AxiosRequestConfig = {}): Promise<T> {
  // 构建完整的配置对象
  const config: AxiosRequestConfig = {
    url,
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };

  // 处理查询参数
  if (options.params) {
    config.params = options.params;
  }

  // 处理请求体
  if (options.data) {
    config.data = options.data;
  }

  // 发送请求
  const response = await myAxios(config);
  return response as T;
}

export default request;
export { myAxios };