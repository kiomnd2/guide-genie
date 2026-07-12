import { useCallback, useEffect, useRef, useState, type CSSProperties, type DragEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { categoriesApi, guidesApi, type Category, type Guide } from '../api/client'

// 프로젝트 > 가이드 탭: 대분류/중분류 관리 + 가이드 목차 + 드래그 앤 드롭 이동.
export default function ProjectGuides() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const navigate = useNavigate()

  const [guides, setGuides] = useState<Guide[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [prompt, setPrompt] = useState('')
  const [msg, setMsg] = useState<string>()
  const [error, setError] = useState<string>()

  // 분류 추가/수정 입력
  const [newMajor, setNewMajor] = useState('')
  const [addMinorFor, setAddMinorFor] = useState<number | null>(null)
  const [newMinor, setNewMinor] = useState('')
  const [editing, setEditing] = useState<{ id: number; name: string } | null>(null)

  // 중복 제출 방지(한글 IME Enter 등으로 핸들러가 두 번 도는 것 방지)
  const busy = useRef(false)

  // 드래그 상태
  const [dragOver, setDragOver] = useState<string | null>(null)

  const loadGuides = useCallback(() => {
    guidesApi.list(pid).then(setGuides).catch((e) => setError(String(e)))
  }, [pid])
  const loadCategories = useCallback(() => {
    categoriesApi.list(pid).then(setCategories).catch((e) => setError(String(e)))
  }, [pid])

  useEffect(() => {
    loadGuides()
    loadCategories()
  }, [loadGuides, loadCategories])

  const majors = categories.filter((c) => c.parentId === null)
  const minorsOf = (majorId: number) => categories.filter((c) => c.parentId === majorId)
  const guidesIn = (categoryId: number | null) => guides.filter((g) => g.categoryId === categoryId)

  // ---- 분류 관리 ----
  // busy 가드로 동시/중복 호출을 1회로 제한한다.
  const guard = async (fn: () => Promise<void>) => {
    if (busy.current) return
    busy.current = true
    try {
      await fn()
    } catch (e) {
      setError(String(e))
    } finally {
      busy.current = false
    }
  }

  const addMajor = () => guard(async () => {
    const name = newMajor.trim()
    if (!name) return
    setNewMajor('')
    await categoriesApi.create(pid, name, null)
    loadCategories()
  })

  const addMinor = (majorId: number) => guard(async () => {
    const name = newMinor.trim()
    if (!name) return
    setNewMinor('')
    setAddMinorFor(null)
    await categoriesApi.create(pid, name, majorId)
    loadCategories()
  })

  const submitRename = () => guard(async () => {
    if (!editing) return
    const name = editing.name.trim()
    const id = editing.id
    setEditing(null)
    if (!name) return
    await categoriesApi.rename(pid, id, name)
    loadCategories()
  })

  const removeCategory = (id: number) => guard(async () => {
    if (!confirm('이 분류를 삭제할까요? 하위 중분류도 삭제되고, 소속 가이드는 미분류로 이동합니다.')) return
    await categoriesApi.del(pid, id)
    loadCategories()
    loadGuides()
  })

  // ---- 드래그 앤 드롭 ----
  const onDragStart = (e: DragEvent, guideId: number) => {
    e.dataTransfer.setData('text/plain', String(guideId))
    e.dataTransfer.effectAllowed = 'move'
  }
  const onDropTo = async (e: DragEvent, categoryId: number | null) => {
    const guideId = Number(e.dataTransfer.getData('text/plain'))
    setDragOver(null)
    if (!guideId) return
    const g = guides.find((x) => x.id === guideId)
    if (g && g.categoryId === categoryId) return // 제자리
    await guidesApi.move(pid, guideId, categoryId)
    loadGuides()
  }

  const generate = async () => {
    if (!prompt.trim()) return
    setMsg(undefined)
    try {
      const res = await guidesApi.generate(pid, prompt)
      setPrompt('')
      setMsg(`AI 생성 작업 접수됨 (job: ${res.jobId}). 완료되면 미분류에 초안으로 나타납니다.`)
    } catch (e) {
      setError(String(e))
    }
  }

  const dropProps = (ckey: string, categoryId: number | null, extra?: CSSProperties) => ({
    onDragOver: (e: DragEvent) => { e.preventDefault(); e.stopPropagation(); setDragOver(ckey) },
    onDrop: (e: DragEvent) => { e.preventDefault(); e.stopPropagation(); onDropTo(e, categoryId) },
    style: {
      borderRadius: 6,
      padding: 8,
      outline: dragOver === ckey ? '2px dashed #0969da' : '1px solid transparent',
      background: dragOver === ckey ? '#f0f8ff' : undefined,
      ...extra,
    } as CSSProperties,
  })

  return (
    <div>
      <h2>가이드</h2>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}

      <div className="card" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <button onClick={() => navigate(`/projects/${pid}/guides/new`)}>+ 직접 작성</button>
        <span style={{ color: '#57606a' }}>또는 AI로 초안 생성 ↓</span>
      </div>

      <div className="card">
        <label>AI 생성 프롬프트</label>
        <textarea
          rows={2}
          placeholder="예) 신규 입사자를 위한 로컬 개발환경 세팅 가이드 만들어줘"
          value={prompt}
          onChange={(e) => setPrompt(e.target.value)}
        />
        <div style={{ marginTop: 8 }}>
          <button style={{ background: '#8250df' }} onClick={generate}>AI 생성</button>
        </div>
        {msg && <p style={{ color: '#57606a' }}>{msg}</p>}
      </div>

      <h3>분류 & 가이드</h3>
      <p style={{ color: '#57606a', fontSize: 13, marginTop: 0 }}>
        가이드 항목을 끌어다 다른 분류로 놓으면 이동합니다. (⠿ 손잡이 드래그)
      </p>

      {/* 대분류 추가 */}
      <div className="card" style={{ display: 'flex', gap: 8 }}>
        <input placeholder="새 대분류 이름" value={newMajor}
          onChange={(e) => setNewMajor(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) addMajor() }} />
        <button onClick={addMajor} style={{ whiteSpace: 'nowrap' }}>+ 대분류</button>
      </div>

      {majors.map((major) => (
        <div className="card" key={major.id} {...dropProps(`m${major.id}`, major.id)}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            {editing?.id === major.id ? (
              <RenameInput editing={editing} setEditing={setEditing} onSubmit={submitRename} />
            ) : (
              <strong>{major.name}</strong>
            )}
            <span style={{ display: 'flex', gap: 6 }}>
              {editing?.id === major.id ? (
                <>
                  <button className="btn-sm" onClick={submitRename}>저장</button>
                  <button className="btn-sm" onClick={() => setEditing(null)}>취소</button>
                </>
              ) : (
                <>
                  <button className="btn-sm" onClick={() => setEditing({ id: major.id, name: major.name })}>수정</button>
                  <button className="btn-sm" onClick={() => { setAddMinorFor(major.id); setNewMinor('') }}>+ 중분류</button>
                  <button className="btn-sm btn-danger" onClick={() => removeCategory(major.id)}>삭제</button>
                </>
              )}
            </span>
          </div>

          {addMinorFor === major.id && (
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input autoFocus placeholder="새 중분류 이름" value={newMinor}
                onChange={(e) => setNewMinor(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) addMinor(major.id) }} />
              <button onClick={() => addMinor(major.id)} style={{ whiteSpace: 'nowrap' }}>추가</button>
            </div>
          )}

          <GuideList guides={guidesIn(major.id)} pid={pid} onDragStart={onDragStart} />

          {minorsOf(major.id).map((minor) => (
            <div key={minor.id} {...dropProps(`n${minor.id}`, minor.id, { marginLeft: 14, marginTop: 6 })}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                {editing?.id === minor.id ? (
                  <RenameInput editing={editing} setEditing={setEditing} onSubmit={submitRename} />
                ) : (
                  <span style={{ fontWeight: 600, color: '#57606a', fontSize: 14 }}>{minor.name}</span>
                )}
                <span style={{ display: 'flex', gap: 6 }}>
                  {editing?.id === minor.id ? (
                    <>
                      <button className="btn-sm" onClick={submitRename}>저장</button>
                      <button className="btn-sm" onClick={() => setEditing(null)}>취소</button>
                    </>
                  ) : (
                    <>
                      <button className="btn-sm" onClick={() => setEditing({ id: minor.id, name: minor.name })}>수정</button>
                      <button className="btn-sm btn-danger" onClick={() => removeCategory(minor.id)}>삭제</button>
                    </>
                  )}
                </span>
              </div>
              <GuideList guides={guidesIn(minor.id)} pid={pid} onDragStart={onDragStart} />
            </div>
          ))}
        </div>
      ))}

      {/* 미분류 */}
      <div className="card" {...dropProps('uncat', null)}>
        <strong style={{ color: '#57606a' }}>미분류</strong>
        <GuideList guides={guidesIn(null)} pid={pid} onDragStart={onDragStart} />
      </div>
    </div>
  )
}

function RenameInput({ editing, setEditing, onSubmit }: {
  editing: { id: number; name: string }
  setEditing: (v: { id: number; name: string } | null) => void
  onSubmit: () => void
}) {
  return (
    <input
      autoFocus
      value={editing.name}
      onChange={(e) => setEditing({ id: editing.id, name: e.target.value })}
      onKeyDown={(e) => {
        if (e.key === 'Enter' && !e.nativeEvent.isComposing) onSubmit()
        else if (e.key === 'Escape') setEditing(null)
      }}
      style={{ maxWidth: 240 }}
    />
  )
}

function GuideList({ guides, pid, onDragStart }: {
  guides: Guide[]
  pid: number
  onDragStart: (e: DragEvent, guideId: number) => void
}) {
  if (guides.length === 0) {
    return <div style={{ color: '#8b949e', fontSize: 13, padding: '6px 0' }}>여기로 가이드를 끌어다 놓으세요</div>
  }
  return (
    <ul style={{ listStyle: 'none', margin: '4px 0 0', padding: 0 }}>
      {guides.map((g) => (
        <li
          key={g.id}
          draggable
          onDragStart={(e) => onDragStart(e, g.id)}
          style={{
            display: 'flex', alignItems: 'center', gap: 8,
            padding: '4px 6px', border: '1px solid #eaeef2', borderRadius: 6, marginBottom: 4,
            background: '#fff',
          }}
        >
          <span style={{ cursor: 'grab', color: '#8b949e' }} title="드래그해서 이동">⠿</span>
          <Link to={`/projects/${pid}/guides/${g.id}`} style={{ flex: 1, color: 'inherit' }}>
            {g.title}
          </Link>
          <StatusBadge status={g.status} />
        </li>
      ))}
    </ul>
  )
}

function StatusBadge({ status }: { status: Guide['status'] }) {
  const published = status === 'PUBLISHED'
  return (
    <span style={{
      fontSize: 12, padding: '2px 8px', borderRadius: 12, color: '#fff',
      background: published ? '#1f883d' : '#9a6700',
    }}>
      {published ? '게시됨' : '초안'}
    </span>
  )
}
