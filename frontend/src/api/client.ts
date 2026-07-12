// API 클라이언트. 사내 SSO JWT(Bearer)를 첨부한다.
// 스캐폴드: 토큰은 localStorage 에서 읽는다(실제 SSO 연동 시 교체).

const TOKEN_KEY = 'gg_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

function headers(): HeadersInit {
  const h: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.text()
    throw new Error(`${res.status} ${res.statusText}: ${body}`)
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}

export const api = {
  get: <T>(path: string) => fetch(`/api${path}`, { headers: headers() }).then(handle<T>),
  post: <T>(path: string, body?: unknown) =>
    fetch(`/api${path}`, { method: 'POST', headers: headers(), body: JSON.stringify(body ?? {}) })
      .then(handle<T>),
  put: <T>(path: string, body?: unknown) =>
    fetch(`/api${path}`, { method: 'PUT', headers: headers(), body: JSON.stringify(body ?? {}) })
      .then(handle<T>),
  del: (path: string) =>
    fetch(`/api${path}`, { method: 'DELETE', headers: headers() }).then((r) => handle<void>(r)),
}

// ---- 도메인 타입 ----

export interface Project {
  id: number
  name: string
  description?: string
  owner: string
}

export interface Guide {
  id: number
  projectId: number
  title: string
  status: 'DRAFT' | 'PUBLISHED'
  createdBy: string
}

export interface Citation {
  guideId: number
  guideTitle: string
  section?: string
  sourceUrl?: string
}

export const projectsApi = {
  list: () => api.get<Project[]>('/projects'),
  create: (name: string, description?: string) =>
    api.post<Project>('/projects', { name, description }),
  get: (id: number) => api.get<Project>(`/projects/${id}`),
}

export const guidesApi = {
  list: (projectId: number) => api.get<Guide[]>(`/projects/${projectId}/guides`),
  generate: (projectId: number, prompt: string) =>
    api.post<{ jobId: string }>(`/projects/${projectId}/guides/generate`, { prompt }),
  update: (projectId: number, guideId: number, title: string, contentMd: string) =>
    api.put<Guide>(`/projects/${projectId}/guides/${guideId}`, { title, contentMd }),
  publish: (projectId: number, guideId: number) =>
    api.post<Guide>(`/projects/${projectId}/guides/${guideId}/publish`),
}
