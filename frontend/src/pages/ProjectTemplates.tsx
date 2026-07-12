import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  categoriesApi, templatesApi,
  type Category, type GuideTemplate, type TemplateItem,
} from '../api/client'
import CategorySelect from '../components/CategorySelect'

interface Draft {
  id: number | null
  name: string
  items: TemplateItem[]
}

const emptyItem = (): TemplateItem => ({ title: '', prompt: '', categoryId: null })

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
    const res = await templatesApi.run(pid, t.id)
    setMsg(`"${t.name}" 실행: ${res.triggered}개 초안 생성을 시작했습니다. 「가이드」 탭에 나타납니다.`)
  })

  const remove = (t: GuideTemplate) => guard(async () => {
    if (!confirm(`템플릿 "${t.name}"을 삭제할까요?`)) return
    await templatesApi.del(pid, t.id)
    load()
  })

  return (
    <div>
      <h2>템플릿</h2>
      <p style={{ color: '#57606a', fontSize: 13, marginTop: 0 }}>
        (제목 · 프롬프트 · 분류) 항목들을 묶어 두고, <b>실행</b>하면 세트 전체가 초안으로 일괄 생성됩니다.
      </p>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}
      {msg && <div className="card" style={{ color: '#1a7f37' }}>{msg}</div>}

      {!draft && (
        <div className="card">
          <button onClick={startNew}>+ 새 템플릿</button>
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
              <textarea style={{ marginTop: 6 }} rows={2} placeholder="AI 생성 프롬프트" value={item.prompt}
                onChange={(e) => patchItem(idx, { prompt: e.target.value })} />
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
