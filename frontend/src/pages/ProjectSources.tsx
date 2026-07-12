import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../api/client'

type SourceType = 'GITHUB' | 'JIRA' | 'CONFLUENCE'

interface FieldDef {
  key: string
  label: string
  placeholder: string
  required?: boolean
}
interface TypeDef {
  label: string
  icon: string
  desc: string
  fields: FieldDef[]
  tokenLabel: string
  tokenHint: string
  summary: (c: Record<string, unknown>) => string
}

const TYPES: Record<SourceType, TypeDef> = {
  GITHUB: {
    label: 'GitHub',
    icon: '🐙',
    desc: '저장소의 코드 · README · Wiki · PR 설명을 수집합니다.',
    fields: [
      { key: 'owner', label: '소유자 / 조직', placeholder: '예) octocat', required: true },
      { key: 'repo', label: '저장소 이름', placeholder: '예) hello-world', required: true },
      { key: 'branch', label: '브랜치 (선택)', placeholder: '기본: main' },
    ],
    tokenLabel: 'Personal Access Token (PAT)',
    tokenHint: 'GitHub → Settings → Developer settings → Personal access tokens 에서 발급. 저장소 읽기 권한 필요.',
    summary: (c) => `${c.owner ?? ''}/${c.repo ?? ''}${c.branch ? ' @' + c.branch : ''}`,
  },
  JIRA: {
    label: 'Jira',
    icon: '🟦',
    desc: 'Atlassian Cloud Jira 이슈를 수집합니다.',
    fields: [
      { key: 'baseUrl', label: '사이트 URL', placeholder: '예) https://your-team.atlassian.net', required: true },
      { key: 'projectKey', label: '프로젝트 키', placeholder: '예) ABC', required: true },
      { key: 'email', label: 'Atlassian 계정 이메일', placeholder: '예) me@team.com', required: true },
    ],
    tokenLabel: 'API Token',
    tokenHint: 'id.atlassian.com → Security → API tokens 에서 발급.',
    summary: (c) => `${c.baseUrl ?? ''} · ${c.projectKey ?? ''}`,
  },
  CONFLUENCE: {
    label: 'Confluence',
    icon: '📘',
    desc: 'Atlassian Cloud Confluence 스페이스의 페이지를 수집합니다.',
    fields: [
      { key: 'baseUrl', label: '사이트 URL', placeholder: '예) https://your-team.atlassian.net/wiki', required: true },
      { key: 'spaceKey', label: '스페이스 키', placeholder: '예) DOCS', required: true },
      { key: 'email', label: 'Atlassian 계정 이메일', placeholder: '예) me@team.com', required: true },
    ],
    tokenLabel: 'API Token',
    tokenHint: 'id.atlassian.com → Security → API tokens 에서 발급.',
    summary: (c) => `${c.baseUrl ?? ''} · ${c.spaceKey ?? ''}`,
  },
}

interface Connection {
  id: number
  projectId: number
  type: SourceType
  config: Record<string, unknown>
  lastSyncedAt?: string
}

// 프로젝트 > 소스 연동: 타입별 전용 폼 + 등록된 연동 목록/수동 동기화.
export default function ProjectSources() {
  const { projectId } = useParams()
  const pid = Number(projectId)

  const [type, setType] = useState<SourceType>('GITHUB')
  const [form, setForm] = useState<Record<string, string>>({})
  const [token, setToken] = useState('')
  const [connections, setConnections] = useState<Connection[]>([])
  const [msg, setMsg] = useState<string>()
  const [error, setError] = useState<string>()
  const busy = useRef(false)

  const load = useCallback(() => {
    api.get<Connection[]>(`/projects/${pid}/connections`).then(setConnections).catch(() => {})
  }, [pid])
  useEffect(() => { load() }, [load])

  const def = TYPES[type]

  const selectType = (t: SourceType) => {
    setType(t)
    setForm({})
    setToken('')
    setError(undefined)
  }

  const register = () => {
    if (busy.current) return
    setError(undefined)
    setMsg(undefined)

    const missing = def.fields.filter((f) => f.required && !form[f.key]?.trim()).map((f) => f.label)
    if (!token.trim()) missing.push(def.tokenLabel)
    if (missing.length) {
      setError(`다음 항목을 입력하세요: ${missing.join(', ')}`)
      return
    }

    const config: Record<string, string> = {}
    def.fields.forEach((f) => {
      const v = form[f.key]?.trim()
      if (v) config[f.key] = v
    })

    busy.current = true
    api.post(`/projects/${pid}/connections`, { type, config, token: token.trim() })
      .then(() => {
        setForm({}); setToken('')
        setMsg(`${def.label} 연동이 등록되었습니다. 초기 동기화가 백그라운드에서 진행됩니다.`)
        load()
      })
      .catch((e) => setError(String(e)))
      .finally(() => { busy.current = false })
  }

  const sync = (cid: number) => {
    api.post(`/projects/${pid}/connections/${cid}/sync`)
      .then(() => setMsg(`연동 #${cid} 동기화를 시작했습니다.`))
      .catch((e) => setError(String(e)))
  }

  const remove = (c: Connection) => {
    if (busy.current) return
    if (!confirm(`${TYPES[c.type]?.label ?? c.type} 연동을 제거할까요? 수집된 문서와 색인도 함께 삭제됩니다.`)) return
    busy.current = true
    api.del(`/projects/${pid}/connections/${c.id}`)
      .then(() => { setMsg('연동을 제거했습니다.'); load() })
      .catch((e) => setError(String(e)))
      .finally(() => { busy.current = false })
  }

  return (
    <div>
      <h2>소스 연동</h2>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}
      {msg && <div className="card" style={{ color: '#1a7f37' }}>{msg}</div>}

      <div className="card">
        <strong>새 소스 연동</strong>

        {/* 타입 선택 */}
        <div style={{ display: 'flex', gap: 8, margin: '10px 0' }}>
          {(Object.keys(TYPES) as SourceType[]).map((t) => (
            <button
              key={t}
              onClick={() => selectType(t)}
              style={{
                flex: 1,
                textAlign: 'left',
                background: type === t ? '#0969da' : '#f6f8fa',
                color: type === t ? '#fff' : '#24292f',
                border: type === t ? '1px solid #0969da' : '1px solid #d0d7de',
              }}
            >
              <span style={{ fontSize: 18, marginRight: 6 }}>{TYPES[t].icon}</span>
              {TYPES[t].label}
            </button>
          ))}
        </div>
        <p style={{ color: '#57606a', fontSize: 13, marginTop: 0 }}>{def.desc}</p>

        {/* 타입별 필드 */}
        {def.fields.map((f) => (
          <div key={f.key} style={{ marginBottom: 8 }}>
            <label>{f.label}{f.required && <span style={{ color: '#cf222e' }}> *</span>}</label>
            <input
              placeholder={f.placeholder}
              value={form[f.key] ?? ''}
              onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
            />
          </div>
        ))}

        {/* 토큰 */}
        <div style={{ marginBottom: 4 }}>
          <label>{def.tokenLabel}<span style={{ color: '#cf222e' }}> *</span></label>
          <input type="password" value={token} placeholder="●●●●●●●●"
            onChange={(e) => setToken(e.target.value)} />
          <div style={{ color: '#57606a', fontSize: 12, marginTop: 2 }}>🔒 {def.tokenHint} 토큰은 암호화 저장됩니다.</div>
        </div>

        <div style={{ marginTop: 12 }}>
          <button onClick={register}>연동 등록</button>
        </div>
      </div>

      <h3>등록된 연동 ({connections.length})</h3>
      {connections.map((c) => (
        <div className="card" key={c.id}
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <div>
              <span style={{ fontSize: 18, marginRight: 6 }}>{TYPES[c.type]?.icon}</span>
              <strong>{TYPES[c.type]?.label ?? c.type}</strong>
            </div>
            <div style={{ color: '#57606a', fontSize: 13, marginTop: 2 }}>
              {TYPES[c.type]?.summary(c.config)}
            </div>
            <div style={{ color: '#8b949e', fontSize: 12 }}>
              마지막 동기화: {c.lastSyncedAt ? new Date(c.lastSyncedAt).toLocaleString() : '없음'}
            </div>
          </div>
          <span style={{ display: 'flex', gap: 6 }}>
            <button onClick={() => sync(c.id)}>수동 동기화</button>
            <button className="btn-danger" onClick={() => remove(c)}>제거</button>
          </span>
        </div>
      ))}
      {connections.length === 0 && <p style={{ color: '#57606a' }}>아직 연동된 소스가 없습니다.</p>}
    </div>
  )
}
