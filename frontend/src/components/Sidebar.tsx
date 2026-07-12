import { useEffect, useState } from 'react'
import { Link, useLocation, useMatch } from 'react-router-dom'
import { projectsApi, type Project } from '../api/client'

// 컨텍스트 인식 사이드바.
// 프로젝트 안(/projects/:id/*)이면 프로젝트 메뉴, 아니면 대시보드.
export default function Sidebar() {
  const match = useMatch('/projects/:projectId/*')
  const projectId = match?.params.projectId
  const { pathname } = useLocation()

  const [project, setProject] = useState<Project>()

  useEffect(() => {
    if (!projectId) {
      setProject(undefined)
      return
    }
    projectsApi.get(Number(projectId)).then(setProject).catch(() => setProject(undefined))
  }, [projectId])

  return (
    <aside className="sidebar">
      <h1><Link to="/">guide-genie</Link></h1>
      {projectId ? (
        <ProjectNav projectId={projectId} projectName={project?.name} pathname={pathname} />
      ) : (
        <nav>
          <Link className={pathname === '/' ? 'active' : ''} to="/">대시보드</Link>
        </nav>
      )}
    </aside>
  )
}

function ProjectNav({ projectId, projectName, pathname }: {
  projectId: string
  projectName?: string
  pathname: string
}) {
  const base = `/projects/${projectId}`
  const isGuides = pathname === base || pathname.startsWith(`${base}/guides`)
  const isSources = pathname.startsWith(`${base}/sources`)
  const isQna = pathname.startsWith(`${base}/qna`)

  return (
    <nav>
      <Link to="/" className="sidebar-back">← 프로젝트 목록</Link>
      <div className="sidebar-project" title={projectName}>{projectName ?? `프로젝트 #${projectId}`}</div>
      <Link className={isGuides ? 'active' : ''} to={base}>가이드</Link>
      <Link className={isSources ? 'active' : ''} to={`${base}/sources`}>소스 연동</Link>
      <Link className={isQna ? 'active' : ''} to={`${base}/qna`}>Q&amp;A</Link>
    </nav>
  )
}
