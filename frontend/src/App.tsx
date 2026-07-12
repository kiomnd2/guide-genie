import { Route, Routes } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import Dashboard from './pages/Dashboard'
import ProjectGuides from './pages/ProjectGuides'
import ProjectTemplates from './pages/ProjectTemplates'
import ProjectSources from './pages/ProjectSources'
import ProjectQna from './pages/ProjectQna'
import GuideNew from './pages/GuideNew'
import GuideWorkspace from './pages/GuideWorkspace'

export default function App() {
  return (
    <div className="layout">
      <Sidebar />
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />

          {/* 프로젝트 컨텍스트 — 네비게이션은 사이드바가 담당 */}
          <Route path="/projects/:projectId" element={<ProjectGuides />} />
          <Route path="/projects/:projectId/templates" element={<ProjectTemplates />} />
          <Route path="/projects/:projectId/sources" element={<ProjectSources />} />
          <Route path="/projects/:projectId/qna" element={<ProjectQna />} />
          <Route path="/projects/:projectId/guides/new" element={<GuideNew />} />
          <Route path="/projects/:projectId/guides/:guideId" element={<GuideWorkspace />} />
        </Routes>
      </main>
    </div>
  )
}
