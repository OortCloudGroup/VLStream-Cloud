import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { billingCurrency, paidRunState, paidRunActive, paidRunErrorPresentation, paidRunQuoteText } from '../src/utils/computeBillingPresentation.mjs'
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

for (const codeField of ['errorCode', 'startFailureCode']) {
  for (const errorCode of ['NO_STOCK', 'START_REJECTED']) {
    for (const state of ['CANCELLING', 'RELEASING', 'CANCELLED']) {
      test(`${codeField}=${errorCode} explains the rejected start in ${state} without changing safe stop eligibility`, () => {
        const run = { [codeField]: errorCode, state }
        const presentation = paidRunErrorPresentation(run)
        assert.equal(presentation.type, 'warning')
        assert.match(presentation.message, errorCode === 'NO_STOCK' ? /当前资源被占用/ : /本次开机被供应商拒绝/)
        if (errorCode === 'START_REJECTED') assert.doesNotMatch(presentation.message, /当前资源被占用|容量不足/)
        if (state === 'CANCELLED') {
          assert.match(presentation.message, /已完成释放/)
          assert.doesNotMatch(presentation.message, /仍待确认/)
          assert.equal(paidRunActive(run), false)
        } else {
          assert.match(presentation.message, /释放仍待确认.*请勿重复开机/)
          assert.doesNotMatch(presentation.message, /已完成释放/)
          assert.equal(paidRunActive(run), true)
        }
      })
    }
  }
}

for (const codeField of ['errorCode', 'startFailureCode']) {
  for (const errorCode of ['UNKNOWN', 'START_OUTCOME_UNKNOWN', 'BadRequest', 'UNAVAILABLE', 'FUNDING_REJECTED', 'FUNDING_REQUIRES_REVIEW', 'RECOVERY_PENDING', 'RELEASE_RECEIPT_PENDING', 'no_stock', ' NO_STOCK', 'NO_STOCK ', 'PROVIDER_NO_STOCK', 'NO_STOCK_UNKNOWN']) {
    test(`${codeField}=${errorCode} stays pending even when provider messages claim no stock`, () => {
      const run = { state: 'STARTING', [codeField]: errorCode, msg: 'NO_STOCK 无库存 当前资源被占用', message: 'NO_STOCK' }
      const presentation = paidRunErrorPresentation(run)
      assert.equal(presentation.type, 'warning')
      assert.match(presentation.message, /状态确认暂未完成.*查询原单.*请勿重复提交启停操作/)
      assert.doesNotMatch(presentation.message, /当前资源被占用|容量不足|已完成释放|被供应商拒绝/)
      assert.equal(paidRunActive(run), true)
    })
  }
}

test('messages alone cannot create a no-stock order notice', () => {
  for (const run of [undefined, null, {}, { errorCode: null }, { errorCode: '' }, { msg: 'NO_STOCK 无库存', message: '当前资源被占用' }]) {
    assert.equal(paidRunErrorPresentation(run), null)
  }
})

test('confirmed original release is displayed as closed without guessing the old start result', () => {
  for (const errorCode of ['OWNER_ADOPTED_ORIGINAL_RELEASE', 'ADMIN_ADOPTED_ORIGINAL_RELEASE']) {
    const run = { state: 'CANCELLED', errorCode, chargedQuota: '0', remainingQuota: '0', startFailureCode: null }
    assert.equal(paidRunErrorPresentation(run).type, 'success')
    assert.match(paidRunErrorPresentation(run).message, /已取消.*已完成释放.*未扣费/)
    assert.doesNotMatch(paidRunErrorPresentation(run).message, /状态确认暂未完成|当前资源被占用/)
    for (const changed of [{ state: 'RELEASING' }, { remainingQuota: '1' }, { chargedQuota: '1' }, { remainingQuota: null }]) {
      assert.equal(paidRunErrorPresentation({ ...run, ...changed }).type, 'warning')
    }
  }
})

test('unverified ledger origin pauses funds recovery without promising an upstream query', () => {
  for (const startFailureCode of [undefined, 'NO_STOCK']) {
    const presentation = paidRunErrorPresentation({ state: 'RELEASING', errorCode: 'BILLING_ORIGIN_MISMATCH', startFailureCode })
    assert.match(presentation.message, /^原订单的账本来源.*资金恢复已暂停/)
    assert.doesNotMatch(presentation.message, /系统会查询原单|已完成释放/)
    if (startFailureCode) assert.match(presentation.message, /当前资源被占用/)
  }
})

for (const errorCode of ['FUNDING_REJECTED', 'FUNDING_REQUIRES_REVIEW', 'RECOVERY_PENDING', 'RELEASE_RECEIPT_PENDING', 'REMAINING_AFTER_CONFIRMED_RELEASE']) {
  test(`current ${errorCode} remains first while the independent original start failure stays visible`, () => {
    for (const startFailureCode of ['NO_STOCK', 'START_REJECTED']) {
      for (const state of ['CANCELLING', 'RELEASING', 'CANCELLED']) {
        const presentation = paidRunErrorPresentation({ state, errorCode, startFailureCode })
        assert.equal(presentation.type, 'warning')
        assert.match(presentation.message, /^状态确认暂未完成.*请勿重复提交启停操作/)
        assert.match(presentation.message, startFailureCode === 'NO_STOCK' ? /当前资源被占用/ : /本次开机被供应商拒绝/)
        assert.doesNotMatch(presentation.message, /已完成释放/)
      }
    }
  })
}

test('conflicting cause fields or a running, completed, unfunded, or unknown state cannot claim a rejected start', () => {
  const conflicts = [
    { state: 'CANCELLING', startFailureCode: 'START_REJECTED', errorCode: 'NO_STOCK' },
    { state: 'CANCELLING', startFailureCode: 'NO_STOCK', errorCode: 'START_REJECTED' },
    { state: 'CANCELLING', startFailureCode: 'UNKNOWN', errorCode: 'NO_STOCK' },
    { state: 'CANCELLING', startFailureCode: null, errorCode: 'NO_STOCK' },
    { state: 'CANCELLING', startFailureCode: undefined, errorCode: 'NO_STOCK' },
    { state: 'CANCELLING', startFailureCode: '', errorCode: 'NO_STOCK' },
    { state: 'CANCELLING', startFailureCode: false, errorCode: 'NO_STOCK' },
    ...['RUNNING', 'COMPLETED', 'QUOTED', 'FUNDING', 'UNEXPECTED'].map(state => ({ state, startFailureCode: 'NO_STOCK' }))
  ]
  for (const run of conflicts) {
    assert.match(paidRunErrorPresentation(run).message, /状态确认暂未完成/)
    assert.doesNotMatch(paidRunErrorPresentation(run).message, /当前资源被占用|被供应商拒绝|已完成释放/)
  }
})
