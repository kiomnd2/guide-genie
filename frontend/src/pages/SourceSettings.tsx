import { useState } from 'react'
import { api } from '../api/client'

type SourceType = 'JIRA' | 'CONFLUENCE' | 'GITHUB'

// 소스 연동 설정: 연동 등록 폼 + 연결 테스트 + 수동 동기화.
export default function SourceSettings() {
  const [projectId, setProjectId] = useState('')
  const [type, setType] = useState<SourceType>('GITHUB')
  const [config, setConfig] = useState('{\n  "baseUrl": "",\n  "repo": ""\n}')
  const [token, setToken] = useState('')
  const [msg, setMsg] = useState<string>()

  const register = async () => {
    try {
      await api.post(`/projects/${projectId}/connections`, {
        type,
        config: JSON.parse(config),
        token,
      })
      setMsg('연동이 등록되었습니다. 초기 동기화가 백그라운드에서 진행됩니다.')
    } catch (e) {
      setMsg(String(e))
    }
  }

  return (
    <div>
      <h2>소스 연동 설정</h2>
      <div className="card">
        <label>프로젝트 ID</label>
        <input value={projectId} onChange={(e) => setProjectId(e.target.value)} />

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
        {msg && <p style={{ color: '#57606a' }}>{msg}</p>}
      </div>
    </div>
  )
}
