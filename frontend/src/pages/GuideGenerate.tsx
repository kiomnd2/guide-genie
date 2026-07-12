import { useState } from 'react'
import { guidesApi } from '../api/client'

// 가이드 생성: 프롬프트 입력 → 비동기 생성 작업 접수.
export default function GuideGenerate() {
  const [projectId, setProjectId] = useState('')
  const [prompt, setPrompt] = useState('신규 입사자를 위한 로컬 개발환경 세팅 가이드 만들어줘')
  const [jobId, setJobId] = useState<string>()
  const [error, setError] = useState<string>()

  const generate = async () => {
    setError(undefined)
    try {
      const res = await guidesApi.generate(Number(projectId), prompt)
      setJobId(res.jobId)
    } catch (e) {
      setError(String(e))
    }
  }

  return (
    <div>
      <h2>가이드 생성</h2>
      <div className="card">
        <label>프로젝트 ID</label>
        <input value={projectId} onChange={(e) => setProjectId(e.target.value)} />

        <label style={{ marginTop: 8, display: 'block' }}>프롬프트</label>
        <textarea rows={4} value={prompt} onChange={(e) => setPrompt(e.target.value)} />

        <div style={{ marginTop: 8 }}>
          <button onClick={generate}>초안 생성 (비동기)</button>
        </div>
        {jobId && (
          <p style={{ color: '#57606a' }}>
            생성 작업이 접수되었습니다 (job: {jobId}). 완료되면 초안(DRAFT) 목록에 나타납니다.
          </p>
        )}
        {error && <p style={{ color: '#cf222e' }}>{error}</p>}
      </div>
    </div>
  )
}
