import { useState } from 'react'
import { useParams } from 'react-router-dom'
import CodeMirror from '@uiw/react-codemirror'
import { markdown } from '@codemirror/lang-markdown'
import ReactMarkdown from 'react-markdown'

// 가이드 에디터: Markdown 편집/미리보기 분할 뷰 + 게시.
export default function GuideEditor() {
  const { guideId } = useParams()
  const [value, setValue] = useState('# 가이드 제목\n\n내용을 입력하세요.')

  return (
    <div>
      <h2>가이드 편집 #{guideId}</h2>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div className="card">
          <CodeMirror value={value} height="480px" extensions={[markdown()]} onChange={setValue} />
        </div>
        <div className="card">
          <ReactMarkdown>{value}</ReactMarkdown>
        </div>
      </div>
      <div style={{ marginTop: 12 }}>
        <button>저장 (새 리비전)</button>{' '}
        <button style={{ background: '#0969da' }}>게시</button>
      </div>
    </div>
  )
}
