import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom"; //监听url变化
import App from "./App";
import "./index.css";
import { AuthProvider } from "./context/AuthContext";

// 第二阶段取消下面这行的注释
// import { AuthProvider } from "./context/AuthContext";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      {/* ← 路由上下文提供者 */}
      {/* 第二阶段加上 AuthProvider 包裹 */}
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
