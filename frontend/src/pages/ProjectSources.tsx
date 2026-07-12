import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api/client'

type SourceType = 'JIRA' | 'CONFLUENCE' | 'GITHUB'

interface Connection {
  id: number
  projectId: number
  type: SourceType
  config: Record<string, unknown>
  lastSyncedAt?: string
}

// 프로젝트 > 소스 연동 탭: 연동 등록 + 등록된 연동 목록/수동 동기화. projectId는 URL에서.
export default function ProjectSources() {
  const { projectId } = useParams()
  const pid = Number(projectId)

  const [type, setType] = useState<SourceType>('GITHUB')
  const [config, setConfig] = useState('{\n  "baseUrl": "",\n  "repo": ""\n}')
  const [token, setToken] = useState('')
  const [connections, setConnections] = useState<Connection[]>([])
  const [msg, setMsg] = useState<string>()
  const [error, setError] = useState<string>()

  const load = () =>
    api.get<Connection[]>(`/projects/${pid}/connections`).then(setConnections).catch(() => {})

  useEffect(() => { load() }, [pid]) // eslint-disable-line react-hooks/exhaustive-deps

  const register = async () => {
    setError(undefined)
    setMsg(undefined)
    let parsed: unknown
    try {
      parsed = JSON.parse(config)
    } catch {
      setError('Config가 올바른 JSON이 아닙니다.')
      return
    }
    try {
      await api.post(`/projects/${pid}/connections`, { type, config: parsed, token })
      setToken('')
      setMsg('연동이 등록되었습니다. 초기 동기화가 백그라운드에서 진행됩니다.')
      load()
    } catch (e) {
      setError(String(e))
    }
  }

  const sync = async (cid: number) => {
    try {
      await api.post(`/projects/${pid}/connections/${cid}/sync`)
      setMsg(`연동 #${cid} 동기화를 시작했습니다.`)
    } catch (e) {
      setError(String(e))
    }
  }

  return (
    <div>
      <h2>소스 연동</h2>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}
      {msg && <div className="card" style={{ color: '#1a7f37' }}>{msg}</div>}

      <div className="card">
        <strong>새 소스 연동</strong>

        <label style={{ marginTop: 8, display: 'block' }}>소스 타입</label>
        <select value={type} onChange={(e) => setType(e.target.value as SourceType)}>
          <option value="GITHUB">GitHub</option>
          <option value="JIRA">Jira (Atlassian Cloud)</option>
          <option value="CONFLUENCE">Confluence (Atlassian Cloud)</option>
        </select>

        <label style={{ marginTop: 8, display: 'block' }}>Config (JSON)</label>
        <textarea rows={5} value={config} onChange={(e) => setConfig(e.target.value)} />

        <label style={{ marginTop: 8, display: 'block' }}>API Token</label>
        <input type="password" value={token} onChange={(e) => setToken(e.target.value)} />

        <div style={{ marginTop: 8 }}>
          <button onClick={register}>연동 등록</button>
        </div>
      </div>

      <h3>등록된 연동 ({connections.length})</h3>
      {connections.map((c) => (
        <div className="card" key={c.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>{c.type}</strong>
            <div style={{ color: '#57606a', fontSize: 12 }}>
              마지막 동기화: {c.lastSyncedAt ? new Date(c.lastSyncedAt).toLocaleString() : '없음'}
            </div>
          </div>
          <button onClick={() => sync(c.id)}>수동 동기화</button>
        </div>
      ))}
      {connections.length === 0 && (
        <p style={{ color: '#57606a' }}>아직 연동된 소스가 없습니다.</p>
      )}
    </div>
  )
}
