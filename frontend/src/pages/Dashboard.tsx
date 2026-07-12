import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { projectsApi, type Project } from '../api/client'

// 프로젝트 목록/대시보드: 프로젝트 카드(클릭 시 상세) + 생성.
export default function Dashboard() {
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [error, setError] = useState<string>()

  const load = () => projectsApi.list().then(setProjects).catch((e) => setError(String(e)))
  useEffect(() => { load() }, [])

  const create = async () => {
    if (!name.trim()) return
    await projectsApi.create(name)
    setName('')
    load()
  }

  return (
    <div>
      <h2>프로젝트</h2>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}

      <div className="card">
        <input placeholder="새 프로젝트 이름" value={name} onChange={(e) => setName(e.target.value)} />
        <div style={{ marginTop: 8 }}>
          <button onClick={create}>프로젝트 생성</button>
        </div>
      </div>

      {projects.map((p) => (
        <Link
          className="card"
          key={p.id}
          to={`/projects/${p.id}`}
          style={{ display: 'block', color: 'inherit' }}
        >
          <strong>{p.name}</strong>
          <div style={{ color: '#57606a' }}>{p.description}</div>
          <div style={{ color: '#57606a', fontSize: 12 }}>owner: {p.owner}</div>
        </Link>
      ))}
      {projects.length === 0 && <p style={{ color: '#57606a' }}>아직 프로젝트가 없습니다.</p>}
    </div>
  )
}
