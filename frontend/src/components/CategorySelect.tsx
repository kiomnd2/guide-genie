import { type Category } from '../api/client'

// 분류 선택 드롭다운: 미분류 + 대분류 + (대분류 › 중분류).
export default function CategorySelect({ categories, value, onChange }: {
  categories: Category[]
  value: number | null
  onChange: (v: number | null) => void
}) {
  const majors = categories.filter((c) => c.parentId === null)
  return (
    <select
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
    >
      <option value="">미분류</option>
      {majors.map((major) => (
        <optgroup key={major.id} label={major.name}>
          <option value={major.id}>{major.name} (대분류)</option>
          {categories
            .filter((c) => c.parentId === major.id)
            .map((minor) => (
              <option key={minor.id} value={minor.id}>{major.name} › {minor.name}</option>
            ))}
        </optgroup>
      ))}
    </select>
  )
}

/** 분류 id → "대분류 › 중분류" 라벨. */
export function categoryLabel(categories: Category[], categoryId: number | null): string | null {
  if (categoryId == null) return null
  const cat = categories.find((c) => c.id === categoryId)
  if (!cat) return null
  if (cat.parentId == null) return cat.name
  const parent = categories.find((c) => c.id === cat.parentId)
  return parent ? `${parent.name} › ${cat.name}` : cat.name
}
