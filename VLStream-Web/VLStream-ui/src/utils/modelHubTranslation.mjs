// Exact source matching prevents edited content from reusing an obsolete translation.
export function translateModelHubText(source, locale, dictionary, english, fallback = '') {
  if (typeof source !== 'string' || !source) return fallback
  if (String(locale).toLowerCase().startsWith('zh')) return source
  for (const bundle of [dictionary, english]) {
    if (bundle && Object.hasOwn(bundle, source) && typeof bundle[source] === 'string' && bundle[source]) {
      return bundle[source]
    }
  }
  // New Chinese content needs a static translation refresh; never leak raw copy
  // or guess a translation from a substring. Technical names remain unchanged.
  return /\p{Script=Han}/u.test(source) ? fallback : source
}
