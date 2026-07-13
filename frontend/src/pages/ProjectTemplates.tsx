import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  categoriesApi, templatesApi,
  type Category, type DetailLevel, type GuideTemplate, type TemplateItem,
} from '../api/client'
import CategorySelect from '../components/CategorySelect'

interface Draft {
  id: number | null
  name: string
  items: TemplateItem[]
}

const emptyItem = (): TemplateItem => ({
  title: '', prompt: '', categoryId: null, audience: '', sections: [], detailLevel: 'STANDARD',
})

const DETAIL_OPTIONS: { value: DetailLevel; label: string }[] = [
  { value: 'BRIEF', label: '간단' },
  { value: 'STANDARD', label: '표준' },
  { value: 'DETAILED', label: '상세' },
]

// 프로젝트 > 템플릿 탭: 가이드 세트 템플릿 관리 + 일괄 생성(실행).
export default function ProjectTemplates() {
  const { projectId } = useParams()
  const pid = Number(projectId)

  const [templates, setTemplates] = useState<GuideTemplate[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [draft, setDraft] = useState<Draft | null>(null)
  const [msg, setMsg] = useState<string>()
  const [error, setError] = useState<string>()
  const busy = useRef(false)

  // 일괄 생성 진행률
  const [progress, setProgress] = useState<
    { name: string; total: number; done: number; failed: number; finished: boolean } | null
  >(null)
  const pollRef = useRef<number | null>(null)
  useEffect(() => () => { if (pollRef.current) clearInterval(pollRef.current) }, [])

  // 분류 생성 입력
  const [newMajor, setNewMajor] = useState('')
  const [minorParent, setMinorParent] = useState<number | null>(null)
  const [newMinor, setNewMinor] = useState('')

  const load = useCallback(() => {
    templatesApi.list(pid).then(setTemplates).catch((e) => setError(String(e)))
    categoriesApi.list(pid).then(setCategories).catch(() => {})
  }, [pid])

  useEffect(() => { load() }, [load])

  const guard = async (fn: () => Promise<void>) => {
    if (busy.current) return
    busy.current = true
    try { await fn() } catch (e) { setError(String(e)) } finally { busy.current = false }
  }

  // ---- 분류 생성(템플릿 화면에서 바로) ----
  const addMajor = () => guard(async () => {
    const name = newMajor.trim()
    if (!name) return
    setNewMajor('')
    await categoriesApi.create(pid, name, null)
    load()
  })
  const addMinor = () => guard(async () => {
    const name = newMinor.trim()
    if (!name || minorParent == null) return
    setNewMinor('')
    await categoriesApi.create(pid, name, minorParent)
    load()
  })

  const autoFromConnections = () => guard(async () => {
    setMsg(undefined)
    const t = await templatesApi.auto(pid)
    load()
    startEdit(t)
    setMsg(`연동 기준으로 "${t.name}" 템플릿을 생성했습니다 (${t.items.length}개 항목). 검토·분류 지정 후 저장하세요.`)
  })

  const runbookFromConnections = () => guard(async () => {
    setMsg(undefined)
    const t = await templatesApi.runbook(pid)
    load()
    startEdit(t)
    setMsg(`GitHub 기준으로 "${t.name}" 템플릿을 생성했습니다 (${t.items.length}개 챕터). 저장 후 실행하면 인수인계 수준 RUNBOOK 초안이 일괄 생성됩니다.`)
  })

  const startNew = () => { setMsg(undefined); setDraft({ id: null, name: '', items: [emptyItem()] }) }
  const startEdit = (t: GuideTemplate) => {
    setMsg(undefined)
    setDraft({ id: t.id, name: t.name, items: t.items.length ? t.items.map((i) => ({ ...i })) : [emptyItem()] })
  }

  const patchItem = (idx: number, patch: Partial<TemplateItem>) => {
    if (!draft) return
    const items = draft.items.map((it, i) => (i === idx ? { ...it, ...patch } : it))
    setDraft({ ...draft, items })
  }
  const addItem = () => draft && setDraft({ ...draft, items: [...draft.items, emptyItem()] })
  const removeItem = (idx: number) =>
    draft && setDraft({ ...draft, items: draft.items.filter((_, i) => i !== idx) })

  const save = () => guard(async () => {
    if (!draft) return
    if (!draft.name.trim()) { setError('템플릿 이름을 입력하세요.'); return }
    const items = draft.items
      .map((i) => ({ ...i, title: i.title.trim(), prompt: i.prompt.trim() }))
      .filter((i) => i.title && i.prompt)
    if (items.length === 0) { setError('제목·프롬프트가 있는 항목이 최소 1개 필요합니다.'); return }
    setError(undefined)
    if (draft.id == null) await templatesApi.create(pid, draft.name.trim(), items)
    else await templatesApi.update(pid, draft.id, draft.name.trim(), items)
    setDraft(null)
    load()
  })

  const run = (t: GuideTemplate) => guard(async () => {
    setMsg(undefined)
    const { jobId, total } = await templatesApi.run(pid, t.id)
    if (pollRef.current) clearInterval(pollRef.current)
    if (total === 0) {
      setProgress({ name: t.name, total: 0, done: 0, failed: 0, finished: true })
      return
    }
    setProgress({ name: t.name, total, done: 0, failed: 0, finished: false })
    pollRef.current = window.setInterval(async () => {
      try {
        const s = await templatesApi.runStatus(pid, jobId)
        setProgress({ name: t.name, total: s.total || total, done: s.done, failed: s.failed, finished: s.finished })
        if (s.finished && pollRef.current) {
          clearInterval(pollRef.current)
          pollRef.current = null
        }
      } catch { /* 폴링 계속 */ }
    }, 1500)
  })

  const remove = (t: GuideTemplate) => guard(async () => {
    if (!confirm(`템플릿 "${t.name}"을 삭제할까요?`)) return
    await templatesApi.del(pid, t.id)
    load()
  })

  const majors = categories.filter((c) => c.parentId === null)
  const childrenOf = (id: number) => categories.filter((c) => c.parentId === id)

  return (
    <div>
      <h2>템플릿</h2>
      <p style={{ color: '#57606a', fontSize: 13, marginTop: 0 }}>
        항목마다 <b>제목 · 목적 · 대상 독자 · 목차 · 상세 수준</b>을 지정해 두고 <b>실행</b>하면,
        세트 전체가 목차를 따르는 상세 초안으로 일괄 생성됩니다.
      </p>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}
      {msg && <div className="card" style={{ color: '#1a7f37' }}>{msg}</div>}

      {progress && (
        <div className="card">
          <div style={{ fontWeight: 600 }}>
            「{progress.name}」 일괄 생성 {progress.finished ? '완료' : '진행 중…'}
            {' '}— {progress.done + progress.failed}/{progress.total}
            {progress.failed > 0 && <span style={{ color: '#cf222e' }}> (실패 {progress.failed})</span>}
          </div>
          <div style={{ background: '#eaeef2', borderRadius: 6, height: 10, marginTop: 6, overflow: 'hidden' }}>
            <div style={{
              height: '100%',
              width: `${progress.total ? Math.round(((progress.done + progress.failed) / progress.total) * 100) : 100}%`,
              background: progress.finished ? '#1f883d' : '#0969da',
              transition: 'width .3s',
            }} />
          </div>
          {progress.finished && (
            <div style={{ marginTop: 6, fontSize: 13, color: '#57606a' }}>
              생성된 초안은 「가이드」 탭에서 확인하세요.{' '}
              <button className="btn-sm" onClick={() => setProgress(null)}>닫기</button>
            </div>
          )}
        </div>
      )}

      {/* 분류 관리 — 여기서 만든 분류를 아래 항목의 「분류」에서 바로 선택 */}
      <div className="card">
        <strong style={{ fontSize: 14 }}>분류 관리</strong>
        <div style={{ color: '#57606a', fontSize: 12, margin: '2px 0 8px' }}>
          대분류/중분류를 여기서 바로 만들 수 있습니다. 항목의 「분류」 선택에 반영됩니다.
        </div>
        <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
          <input placeholder="새 대분류 이름" value={newMajor}
            onChange={(e) => setNewMajor(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) addMajor() }} />
          <button className="btn-sm" style={{ whiteSpace: 'nowrap' }} onClick={addMajor}>+ 대분류</button>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select value={minorParent ?? ''} style={{ maxWidth: 200 }}
            onChange={(e) => setMinorParent(e.target.value === '' ? null : Number(e.target.value))}>
            <option value="">대분류 선택</option>
            {majors.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
          <input placeholder="새 중분류 이름" value={newMinor}
            onChange={(e) => setNewMinor(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) addMinor() }} />
          <button className="btn-sm" style={{ whiteSpace: 'nowrap' }} onClick={addMinor}
            disabled={minorParent == null}>+ 중분류</button>
        </div>
        {majors.length > 0 && (
          <div style={{ marginTop: 8, fontSize: 12, color: '#57606a' }}>
            {majors.map((m) => (
              <div key={m.id}>
                <b>{m.name}</b>
                {childrenOf(m.id).length > 0 && ' › ' + childrenOf(m.id).map((c) => c.name).join(', ')}
              </div>
            ))}
          </div>
        )}
      </div>

      {!draft && (
        <div className="card" style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <button onClick={startNew}>+ 새 템플릿</button>
          <button style={{ background: '#8250df' }} onClick={autoFromConnections}>⚙ 연동 기준 자동 생성</button>
          <button style={{ background: '#1f883d' }} onClick={runbookFromConnections}>📘 RUNBOOK 세트 자동 생성</button>
          <span style={{ color: '#57606a', fontSize: 12 }}>
            RUNBOOK 세트는 GitHub 소스를 근거로 개요·구조·환경·DB·API·배포·장애대응 등 인수인계 챕터를 상세하게 생성합니다.
          </span>
        </div>
      )}

      {draft && (
        <div className="card">
          <label>템플릿 이름</label>
          <input value={draft.name} placeholder="예) 신규 프로젝트 온보딩 세트"
            onChange={(e) => setDraft({ ...draft, name: e.target.value })} />

          <div style={{ marginTop: 12, fontWeight: 600 }}>항목 ({draft.items.length})</div>
          {draft.items.map((item, idx) => (
            <div key={idx} style={{ border: '1px solid #eaeef2', borderRadius: 6, padding: 8, marginTop: 8 }}>
              <div style={{ display: 'flex', gap: 8 }}>
                <input style={{ flex: 2 }} placeholder="가이드 제목" value={item.title}
                  onChange={(e) => patchItem(idx, { title: e.target.value })} />
                <div style={{ flex: 1 }}>
                  <CategorySelect categories={categories} value={item.categoryId}
                    onChange={(v) => patchItem(idx, { categoryId: v })} />
                </div>
                <button className="btn-sm btn-danger" onClick={() => removeItem(idx)}>삭제</button>
              </div>
              <textarea style={{ marginTop: 6 }} rows={2} placeholder="가이드 목적/요청 — 무엇을 다룰지"
                value={item.prompt} onChange={(e) => patchItem(idx, { prompt: e.target.value })} />
              <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                <input style={{ flex: 2 }} placeholder="대상 독자 (예: 신규 입사자)" value={item.audience ?? ''}
                  onChange={(e) => patchItem(idx, { audience: e.target.value })} />
                <select style={{ flex: 1 }} value={item.detailLevel ?? 'STANDARD'}
                  onChange={(e) => patchItem(idx, { detailLevel: e.target.value as DetailLevel })}>
                  {DETAIL_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>상세 수준: {o.label}</option>
                  ))}
                </select>
              </div>
              <textarea style={{ marginTop: 6 }} rows={3}
                placeholder={'포함할 목차 (한 줄에 하나씩) — 예)\n사전 준비물\n설치 단계\n동작 확인'}
                value={(item.sections ?? []).join('\n')}
                onChange={(e) => patchItem(idx, { sections: e.target.value.split('\n') })} />
              <div style={{ fontSize: 11, color: '#57606a', marginTop: 2 }}>
                목차를 지정하면 각 항목을 「## 섹션」으로 빠짐없이 채워 상세 가이드를 만듭니다. 비워두면 AI가 목차를 스스로 구성합니다.
              </div>
            </div>
          ))}
          <div style={{ marginTop: 8 }}>
            <button className="btn-sm" onClick={addItem}>+ 항목 추가</button>
          </div>

          <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
            <button onClick={save}>저장</button>
            <button className="btn-sm" onClick={() => { setDraft(null); setError(undefined) }}>취소</button>
          </div>
        </div>
      )}

      <h3>템플릿 목록 ({templates.length})</h3>
      {templates.map((t) => (
        <div className="card" key={t.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>{t.name}</strong>
            <div style={{ color: '#57606a', fontSize: 12 }}>{t.items.length}개 항목</div>
          </div>
          <span style={{ display: 'flex', gap: 6 }}>
            <button onClick={() => run(t)}>▶ 실행(일괄 생성)</button>
            <button className="btn-sm" onClick={() => startEdit(t)}>편집</button>
            <button className="btn-sm btn-danger" onClick={() => remove(t)}>삭제</button>
          </span>
        </div>
      ))}
      {templates.length === 0 && <p style={{ color: '#57606a' }}>아직 템플릿이 없습니다.</p>}
    </div>
  )
}
