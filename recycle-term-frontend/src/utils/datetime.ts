export function formatDateTime(input?: string | null) {
  if (!input) return '-'
  const d = new Date(input)
  if (Number.isNaN(d.getTime())) {
    return String(input).replace('T', ' ').slice(0, 19)
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}
