import { useConfig } from 'nextra-theme-docs'
import { useRouter } from 'next/router'

const Logo = () => (
  <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700 }}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="2" y="6" width="20" height="12" rx="6" fill="#7F52FF" />
      <circle cx="8" cy="12" r="2.5" fill="#fff" />
      <rect x="12" y="10.5" width="7" height="3" rx="1.5" fill="#fff" opacity="0.9" />
    </svg>
    <span>quick-actions-kmp</span>
  </span>
)

const SITE = 'quick-actions-kmp'
const REPO = 'https://github.com/AndroidPoet/quick-actions-kmp'
const DESCRIPTION =
  'quick-actions-kmp — one Kotlin Multiplatform API for home-screen quick actions: UIApplicationShortcutItem on iOS, dynamic shortcuts on Android.'

export default {
  logo: <Logo />,
  project: { link: REPO },
  docsRepositoryBase: `${REPO}/tree/main/website`,
  color: { hue: 255, saturation: 100 },
  footer: {
    content: (
      <span>
        MIT © {new Date().getFullYear()}{' '}
        <a href={REPO} target="_blank" rel="noreferrer">
          {SITE}
        </a>
        . One quick-actions API for Kotlin Multiplatform.
      </span>
    ),
  },
  head: function useHead() {
    const { frontMatter } = useConfig()
    const { asPath } = useRouter()
    const pageTitle = frontMatter?.title
    const title = pageTitle ? `${pageTitle} – ${SITE}` : SITE
    const description = frontMatter?.description ?? DESCRIPTION
    const base = 'https://androidpoet.github.io/quick-actions-kmp'
    const path = asPath === '/' ? '' : asPath.split('?')[0].split('#')[0]
    const canonical = `${base}${path}`
    const ogImage = `${base}/favicon.svg`
    return (
      <>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>{title}</title>
        <meta name="description" content={description} />
        <link rel="canonical" href={canonical} />
        <link rel="icon" href={`${base}/favicon.svg`} type="image/svg+xml" />
        <meta name="theme-color" content="#7F52FF" />
        <meta property="og:type" content="website" />
        <meta property="og:site_name" content={SITE} />
        <meta property="og:url" content={canonical} />
        <meta property="og:title" content={pageTitle ?? SITE} />
        <meta property="og:description" content={description} />
        <meta property="og:image" content={ogImage} />
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={pageTitle ?? SITE} />
        <meta name="twitter:description" content={description} />
        <meta name="twitter:image" content={ogImage} />
      </>
    )
  },
  sidebar: { defaultMenuCollapseLevel: 1 },
  toc: { backToTop: true },
  navigation: { prev: true, next: true },
  darkMode: true,
}
