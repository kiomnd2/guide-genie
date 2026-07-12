import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import { categoriesApi, guidesApi, type Category } from '../api/client'
import CategorySelect from '../components/CategorySelect'
import MarkdownEditor from '../components/MarkdownEditor'

// 새 가이드 직접 작성: 제목 + 분류 선택 + 본문 → 저장 시 생성 후 워크스페이스로 이동.
export default function GuideNew() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [content, setContent] = useState('# 제목\n\n내용을 입력하세요.')
  const [categories, setCategories] = useState<Category[]>([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()

  useEffect(() => {
    categoriesApi.list(pid).then(setCategories).catch(() => {})
  }, [pid])

  const create = async () => {
    if (!title.trim()) {
      setError('제목을 입력하세요.')
      return
    }
    setSaving(true)
    setError(undefined)
    try {
      const g = await guidesApi.create(pid, { title, categoryId, contentMd: content })
      navigate(`/projects/${pid}/guides/${g.id}`)
    } catch (e) {
      setError(String(e))
      setSaving(false)
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 8 }}>
        <Link to={`/projects/${pid}`}>← 프로젝트로</Link>
      </div>
      <h2>새 가이드 작성</h2>
      {error && <div className="card" style={{ color: '#cf222e' }}>{error}</div>}

      <div className="card">
        <label>제목</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="가이드 제목" />

        <label style={{ marginTop: 8, display: 'block' }}>분류</label>
        <CategorySelect categories={categories} value={categoryId} onChange={setCategoryId} />
        <p style={{ color: '#57606a', fontSize: 12, marginBottom: 0 }}>
          분류는 「가이드」 탭에서 관리하며, 목록에서 목차로 묶여 표시됩니다. 나중에 드래그로 이동할 수 있습니다.
        </p>
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
        <button onClick={create} disabled={saving}>{saving ? '저장 중…' : '초안으로 저장'}</button>
      </div>
    </div>
  )
}
