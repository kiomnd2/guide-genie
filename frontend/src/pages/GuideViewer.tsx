import { useParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'

// 가이드 뷰어: 렌더링된 가이드 + 목차 + 원본 변경 알림(스캐폴드).
export default function GuideViewer() {
  const { guideId } = useParams()
  const content = '# 가이드 #' + guideId + '\n\n> 렌더링된 가이드 본문이 여기에 표시됩니다.'

  return (
    <div>
      <div className="card" style={{ background: '#fff8c5', border: '1px solid #d4a72c' }}>
        원본 소스가 변경되었습니다. 가이드 검토가 필요할 수 있습니다. (수동 확정 원칙)
      </div>
      <div className="card">
        <ReactMarkdown>{content}</ReactMarkdown>
      </div>
    </div>
  )
}
