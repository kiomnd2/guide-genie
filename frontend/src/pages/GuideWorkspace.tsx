import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import { categoriesApi, guidesApi, type Category, type Guide } from '../api/client'
import CategorySelect, { categoryLabel } from '../components/CategorySelect'
import MarkdownEditor from '../components/MarkdownEditor'

type Tab = 'read' | 'edit'

// 가이드 워크스페이스: [읽기] / [편집] 2탭.
export default function GuideWorkspace() {
  const { projectId, guideId } = useParams()
  const pid = Number(projectId)
  const gid = Number(guideId)

  const [tab, setTab] = useState<Tab>('read')
  const [guide, setGuide] = useState<Guide>()
  const [categories, setCategories] = useState<Category[]>([])
  const [title, setTitle] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)
  const [status, setStatus] = useState<string>()
  const [error, setError] = useState<string>()

  const load = async () => {
    try {
      const [g, md, cats] = await Promise.all([
        guidesApi.get(pid, gid),
        guidesApi.latestContent(gid),
        categoriesApi.list(pid),
      ])
      setGuide(g)
      setTitle(g.title)
      setCategoryId(g.categoryId)
      setContent(md)
      setCategories(cats)
    } catch (e) {
      setError(String(e))
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid, gid])

  const save = async () => {
    setSaving(true)
    setStatus(undefined)
    try {
      const g = await guidesApi.update(pid, gid, { title, categoryId, contentMd: content })
      setGuide(g)
      setStatus('저장되었습니다 (새 리비전 생성).')
    } catch (e) {
      setError(String(e))
    } finally {
      setSaving(false)
    }
  }

  const publish = async () => {
    try {
      const g = await guidesApi.publish(pid, gid)
      setGuide(g)
      setStatus('게시되었습니다.')
    } catch (e) {
      setError(String(e))
    }
  }

  const toc = useMemo(() => extractToc(content), [content])

  return (
    <div>
      <div style={{ marginBottom: 8 }}>
        <Link to={`/projects/${pid}`}>← 프로젝트로</Link>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          {guide && categoryLabel(categories, guide.categoryId) && (
            <div style={{ fontSize: 12, color: '#57606a' }}>
              {categoryLabel(categories, guide.categoryId)}
            </div>
          )}
          <h2 style={{ margin: 0 }}>{guide?.title ?? `가이드 #${gid}`}</h2>
        </div>
        <span style={{ fontSize: 12, color: '#57606a' }}>
          {guide?.status === 'PUBLISHED' ? '게시됨' : '초안'}
        </span>
      </div>

      {/* 탭 바 */}
      <div style={{ display: 'flex', gap: 4, borderBottom: '1px solid #d0d7de', margin: '12px 0' }}>
        <TabButton active={tab === 'read'} onClick={() => setTab('read')}>읽기</TabButton>
        <TabButton active={tab === 'edit'} onClick={() => setTab('edit')}>편집</TabButton>
      </div>

      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}
      {status && <div className="card" style={{ color: '#1a7f37' }}>{status}</div>}

      {tab === 'read' ? (
        <div style={{ display: 'grid', gridTemplateColumns: '200px 1fr', gap: 12 }}>
          <div className="card" style={{ alignSelf: 'start' }}>
            <strong style={{ fontSize: 13 }}>목차</strong>
            {toc.length === 0 && <div style={{ color: '#57606a', fontSize: 13 }}>제목 없음</div>}
            <ul style={{ paddingLeft: 16, margin: '8px 0 0' }}>
              {toc.map((h, i) => (
                <li key={i} style={{ marginLeft: (h.level - 1) * 10, fontSize: 13, listStyle: 'none' }}>
                  {h.text}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <div className="card">
              {content ? <ReactMarkdown>{content}</ReactMarkdown> : <em style={{ color: '#57606a' }}>본문이 없습니다.</em>}
            </div>
            {guide?.status !== 'PUBLISHED' && (
              <button style={{ background: '#0969da' }} onClick={publish}>게시</button>
            )}
          </div>
        </div>
      ) : (
        <div>
          <div className="card">
            <label>제목</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} />
            <label style={{ marginTop: 8, display: 'block' }}>분류</label>
            <CategorySelect categories={categories} value={categoryId} onChange={setCategoryId} />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="card">
              <div style={{ fontSize: 12, color: '#57606a', marginBottom: 4 }}>Markdown</div>
              <MarkdownEditor value={content} onChange={setContent} />
            </div>
            <div className="card">
              <div style={{ fontSize: 12, color: '#57606a', marginBottom: 4 }}>미리보기</div>
              <ReactMarkdown>{content}</ReactMarkdown>
            </div>
          </div>
          <div style={{ marginTop: 12 }}>
            <button onClick={save} disabled={saving}>{saving ? '저장 중…' : '저장 (새 리비전)'}</button>
          </div>
        </div>
      )}
    </div>
  )
}

function TabButton({ active, onClick, children }: {
  active: boolean
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      onClick={onClick}
      style={{
        background: 'transparent',
        color: active ? '#0969da' : '#57606a',
        borderBottom: active ? '2px solid #0969da' : '2px solid transparent',
        borderRadius: 0,
        padding: '8px 16px',
        fontWeight: active ? 600 : 400,
      }}
    >
      {children}
    </button>
  )
}

interface Heading {
  level: number
  text: string
}

// 코드펜스 밖의 ATX 헤딩(#, ##, ###)만 추출.
function extractToc(md: string): Heading[] {
  const headings: Heading[] = []
  let inFence = false
  for (const line of md.split('\n')) {
    if (line.trim().startsWith('```')) {
      inFence = !inFence
      continue
    }
    if (inFence) continue
    const m = /^(#{1,3})\s+(.*)$/.exec(line)
    if (m) headings.push({ level: m[1].length, text: m[2].trim() })
  }
  return headings
}
