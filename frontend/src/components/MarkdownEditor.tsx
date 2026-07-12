import { useRef } from 'react'
import CodeMirror, { type ReactCodeMirrorRef } from '@uiw/react-codemirror'
import { markdown } from '@codemirror/lang-markdown'

// 마크다운 에디터 + 이미지/링크 삽입 툴바. 커서 위치에 스니펫을 넣는다.
export default function MarkdownEditor({ value, onChange, height = '480px' }: {
  value: string
  onChange: (v: string) => void
  height?: string
}) {
  const ref = useRef<ReactCodeMirrorRef>(null)

  const insert = (snippet: string) => {
    const view = ref.current?.view
    if (!view) {
      onChange(value + snippet)
      return
    }
    const { from, to } = view.state.selection.main
    view.dispatch({
      changes: { from, to, insert: snippet },
      selection: { anchor: from + snippet.length },
    })
    view.focus()
  }

  const insertLink = () => {
    const url = window.prompt('링크 URL', 'https://')
    if (!url) return
    const text = window.prompt('링크 텍스트', '링크') || url
    insert(`[${text}](${url})`)
  }

  const insertImage = () => {
    const url = window.prompt('이미지 URL', 'https://')
    if (!url) return
    const alt = window.prompt('대체 텍스트(alt)', '') || ''
    insert(`![${alt}](${url})`)
  }

  return (
    <div>
      <div style={{ display: 'flex', gap: 6, marginBottom: 6 }}>
        <button type="button" className="btn-sm" onClick={insertLink}>🔗 링크</button>
        <button type="button" className="btn-sm" onClick={insertImage}>🖼 이미지</button>
      </div>
      <CodeMirror ref={ref} value={value} height={height} extensions={[markdown()]} onChange={onChange} />
    </div>
  )
}
