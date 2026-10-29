import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { billingCurrency, paidRunState, paidRunActive, paidRunQuoteText } from '../src/utils/computeBillingPresentation.mjs'
const marketSource = await readFile(new URL('../src/utils/computeMarketPresentation.js', import.meta.url), 'utf8')
const { marketPricePresentation } = await import('data:text/javascript;base64,' + Buffer.from(marketSource).toString('base64'))

test('official deployment price is exact CNY per instance hour and never supplier cost times two', () => {
  const official = { configurationManaged: true, provider: 'autodl', billingType: 'POINTS', pricePolicy: 'VLS_FIXED', userHourlyPrice: '13.00', priceCurrency: 'CNY', priceUnit: 'PER_INSTANCE_HOUR', pointsPerHour: 999999 }
  assert.deepEqual(marketPricePresentation(official), { amount: '￥13.00', unit: '/整实例小时', reference: false, fixed: true })
  assert.equal(marketPricePresentation({ ...official, userHourlyPrice: null }), null)
  assert.equal(marketPricePresentation({ ...official, pricePolicy: null }), null)
  assert.equal(marketPricePresentation({ ...official, userHourlyPrice: '0.00' }), null)
  assert.equal(marketPricePresentation({ ...official, priceUnit: 'PER_GPU_HOUR' }), null)
})

test('wallet projection keeps integers above JS safe range exact', () => {
  assert.equal(billingCurrency('9007199254740993', '500000', '1'), '18014398509.481986')
  assert.equal(billingCurrency('4999971278890', '500000', '1'), '9999942.55778')
  assert.equal(billingCurrency('1', '3', '1'), '≈ 0.333333')
  assert.equal(billingCurrency('0', '500000', '1'), '0')
})
test('invalid money or conversion cannot become a displayed zero balance', () => {
  for (const value of [1, null, '', '-1', '01', '1.5', '9223372036854775808']) assert.throws(() => billingCurrency(value, '500000', '1'))
  assert.throws(() => billingCurrency('1', '0', '1'))
  assert.throws(() => billingCurrency('1', '1', '0'))
})
test('quote states the accepted rate, finite hold and stop behavior', () => {
  const text = paidRunQuoteText({ reserveQuota: '5', maximumRunSeconds: 60, hourlyPrice: '1.20', currency: 'CNY', quotaUnit: 'quota' })
  for (const phrase of ['1.20 CNY/小时', '60 秒', '5 quota', '训练完成后实例仍会运行并计费', '可能保存此计费授权']) assert.ok(text.includes(phrase))
  assert.throws(() => paidRunQuoteText({ reserveQuota: '5', maximumRunSeconds: 0, hourlyPrice: '1' }))
  assert.throws(() => paidRunQuoteText({ reserveQuota: '5', maximumRunSeconds: 60, hourlyPrice: 1 }))
})
test('unknown states never masquerade as reconciled or running', () => {
  assert.equal(paidRunState('UNEXPECTED'), '状态待确认')
  assert.equal(paidRunActive({ state: 'STARTING' }), true)
  assert.equal(paidRunActive({ state: 'COMPLETED' }), false)
})
