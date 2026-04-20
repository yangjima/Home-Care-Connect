import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

export function renderMarkdown(input: string): string {
  const source = typeof input === 'string' ? input : String(input ?? '')
  const html = md.render(source)

  // Vite SPA runs in browser; keep SSR-safe fallback just in case.
  if (typeof window === 'undefined') return html
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}

