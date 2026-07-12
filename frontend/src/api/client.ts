// API 클라이언트. 인증(SSO/JWT)은 현재 미도입 — 별도 토큰 없이 호출한다.

function headers(): HeadersInit {
  return { 'Content-Type': 'application/json' }
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
  patch: <T>(path: string, body?: unknown) =>
    fetch(`/api${path}`, { method: 'PATCH', headers: headers(), body: JSON.stringify(body ?? {}) })
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

export interface Category {
  id: number
  parentId: number | null // null = 대분류
  name: string
}

export interface Guide {
  id: number
  projectId: number
  title: string
  categoryId: number | null
  status: 'DRAFT' | 'PUBLISHED'
  createdBy: string
}

export interface GuideInput {
  title: string
  categoryId: number | null
  contentMd: string
}

export interface Revision {
  id: number
  version: number
  contentMd: string
  editedBy: string
  createdAt: string
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

export interface TemplateItem {
  title: string
  prompt: string
  categoryId: number | null
}

export interface GuideTemplate {
  id: number
  name: string
  items: TemplateItem[]
}

export const templatesApi = {
  list: (projectId: number) => api.get<GuideTemplate[]>(`/projects/${projectId}/templates`),
  create: (projectId: number, name: string, items: TemplateItem[]) =>
    api.post<GuideTemplate>(`/projects/${projectId}/templates`, { name, items }),
  auto: (projectId: number) =>
    api.post<GuideTemplate>(`/projects/${projectId}/templates/auto`),
  update: (projectId: number, id: number, name: string, items: TemplateItem[]) =>
    api.put<GuideTemplate>(`/projects/${projectId}/templates/${id}`, { name, items }),
  del: (projectId: number, id: number) => api.del(`/projects/${projectId}/templates/${id}`),
  run: (projectId: number, id: number) =>
    api.post<{ jobId: string; total: number }>(`/projects/${projectId}/templates/${id}/run`),
  runStatus: (projectId: number, jobId: string) =>
    api.get<{ total: number; done: number; failed: number; finished: boolean }>(
      `/projects/${projectId}/templates/runs/${jobId}`),
}

export const categoriesApi = {
  list: (projectId: number) => api.get<Category[]>(`/projects/${projectId}/categories`),
  create: (projectId: number, name: string, parentId: number | null) =>
    api.post<Category>(`/projects/${projectId}/categories`, { name, parentId }),
  rename: (projectId: number, id: number, name: string) =>
    api.put<Category>(`/projects/${projectId}/categories/${id}`, { name }),
  del: (projectId: number, id: number) => api.del(`/projects/${projectId}/categories/${id}`),
}

export const guidesApi = {
  list: (projectId: number) => api.get<Guide[]>(`/projects/${projectId}/guides`),
  move: (projectId: number, guideId: number, categoryId: number | null) =>
    api.patch<Guide>(`/projects/${projectId}/guides/${guideId}/category`, { categoryId }),
  get: (projectId: number, guideId: number) =>
    api.get<Guide>(`/projects/${projectId}/guides/${guideId}`),
  create: (projectId: number, input: GuideInput) =>
    api.post<Guide>(`/projects/${projectId}/guides`, input),
  update: (projectId: number, guideId: number, input: GuideInput) =>
    api.put<Guide>(`/projects/${projectId}/guides/${guideId}`, input),
  publish: (projectId: number, guideId: number) =>
    api.post<Guide>(`/projects/${projectId}/guides/${guideId}/publish`),
  del: (projectId: number, guideId: number) =>
    api.del(`/projects/${projectId}/guides/${guideId}`),
  generate: (projectId: number, prompt: string) =>
    api.post<{ jobId: string }>(`/projects/${projectId}/guides/generate`, { prompt }),
  // 현재 본문은 최신 리비전에서 로드한다(리비전은 version desc 정렬).
  revisions: (guideId: number) => api.get<Revision[]>(`/guides/${guideId}/revisions`),
  latestContent: (guideId: number) =>
    api.get<Revision[]>(`/guides/${guideId}/revisions`).then((rs) => rs[0]?.contentMd ?? ''),
}
