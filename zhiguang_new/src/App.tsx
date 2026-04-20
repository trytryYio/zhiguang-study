import { Navigate, Route, Routes } from "react-router-dom";
// 第一阶段只有这三个页面
// 后续阶段会逐步导入更多页面组件
// 第二阶段：导入 LoginPage, RegisterPage
// 第三阶段：导入 CreatePage, ProfilePage, EditProfilePage
// 第四阶段：导入 HomePage（完整版本）
// 第五阶段：导入 SearchPage, CourseDetailPage
// 第六阶段：导入 LearningPage
import HomePage from "./pages/HomePage";
import SearchPage from "./pages/SearchPage";
import CreatePage from "./pages/CreatePage";
import LearningPage from "./pages/LearningPage";
import CourseDetailPage from "./pages/CourseDetailPage";
import EditProfilePage from "./pages/EditProfilePage";
import LoginPage from "./pages/LoginPage";
import ProfilePage from "./pages/ProfilePage";
import RegisterPage from "./pages/RegisterPage";

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/search" element={<SearchPage />} />
      <Route path="/create" element={<CreatePage />} />
      <Route path="/learn" element={<LearningPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/profile/edit" element={<EditProfilePage />} />
      <Route path="/post/:id" element={<CourseDetailPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
