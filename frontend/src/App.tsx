import { NavLink, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import SourceSettings from './pages/SourceSettings'
import GuideGenerate from './pages/GuideGenerate'
import GuideEditor from './pages/GuideEditor'
import GuideViewer from './pages/GuideViewer'
import QnaChat from './pages/QnaChat'

export default function App() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>guide-genie</h1>
        <nav>
          <NavLink to="/" end>대시보드</NavLink>
          <NavLink to="/sources">소스 연동</NavLink>
          <NavLink to="/generate">가이드 생성</NavLink>
          <NavLink to="/qna">Q&amp;A</NavLink>
        </nav>
      </aside>
      <main className="content">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/sources" element={<SourceSettings />} />
          <Route path="/generate" element={<GuideGenerate />} />
          <Route path="/guides/:guideId/edit" element={<GuideEditor />} />
          <Route path="/guides/:guideId" element={<GuideViewer />} />
          <Route path="/qna" element={<QnaChat />} />
        </Routes>
      </main>
    </div>
  )
}
