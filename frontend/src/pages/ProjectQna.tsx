import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { type Citation } from '../api/client'

interface Message {
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
}

// 프로젝트 > Q&A 탭: SSE 스트리밍 답변 + 출처. projectId는 URL에서.
// POST + ReadableStream 으로 SSE를 수신한다(EventSource는 GET만 지원).
export default function ProjectQna() {
  const { projectId } = useParams()
  const pid = Number(projectId)
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<Message[]>([])
  const [streaming, setStreaming] = useState(false)

  const ask = async () => {
    if (!question.trim()) return
    const q = question
    setQuestion('')
    setMessages((m) => [...m, { role: 'user', content: q }, { role: 'assistant', content: '' }])
    setStreaming(true)

    const res = await fetch(`/api/projects/${pid}/qna`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({ question: q }),
    })

    const reader = res.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (reader) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() ?? ''
      for (const evt of events) handleEvent(evt)
    }
    setStreaming(false)
  }

  const handleEvent = (evt: string) => {
    const nameLine = evt.split('\n').find((l) => l.startsWith('event:'))
    const dataLine = evt.split('\n').find((l) => l.startsWith('data:'))
    if (!dataLine) return
    const name = nameLine?.slice(6).trim()
    const data = dataLine.slice(5).trim()
    if (name === 'token') {
      setMessages((m) => {
        const copy = [...m]
        copy[copy.length - 1].content += data
        return copy
      })
    } else if (name === 'citations') {
      try {
        const citations = JSON.parse(data) as Citation[]
        setMessages((m) => {
          const copy = [...m]
          copy[copy.length - 1].citations = citations
          return copy
        })
      } catch { /* ignore */ }
    }
  }

  return (
    <div>
      <h2>Q&amp;A</h2>
      {messages.map((m, i) => (
        <div className="card" key={i} style={{ background: m.role === 'user' ? '#ddf4ff' : '#fff' }}>
          <div style={{ fontSize: 12, color: '#57606a' }}>{m.role}</div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{m.content}</div>
          {m.citations && m.citations.length > 0 && (
            <div style={{ marginTop: 8, fontSize: 13 }}>
              <strong>출처</strong>
              <ul>
                {m.citations.map((c, j) => (
                  <li key={j}>
                    {c.guideTitle}{c.section ? ` › ${c.section}` : ''}
                    {c.sourceUrl && (
                      <> — <a href={c.sourceUrl} target="_blank" rel="noreferrer">원본</a></>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ))}
      {messages.length === 0 && (
        <p style={{ color: '#57606a' }}>게시된 가이드를 기반으로 질문에 답합니다. 질문을 입력하세요.</p>
      )}

      <div className="card">
        <textarea
          rows={2}
          placeholder="질문을 입력하세요"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        />
        <div style={{ marginTop: 8 }}>
          <button onClick={ask} disabled={streaming}>{streaming ? '답변 중…' : '질문'}</button>
        </div>
      </div>
    </div>
  )
}
