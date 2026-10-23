# 中转站计费契约：已答与待实测清单

2026-10-23，已读取 `external-billing-implementation-plan(2).md` 和
`external-billing-vls-integration-prompt(3).md` 原件。下表替代此前七项待答清单；
附件提供的契约已足够继续开发，不能继续将已答字段当成核心开发阻塞。
“已答”表示附件明确声明，并不表示目标服务已上线或已通过真实钱包验收。

## 已答，按此实现

| 范围 | 明确的契约 |
| --- | --- |
| 逐单冻结与归属 | hold 明细属于商户和用户，保存总额、captured、released、remaining、状态、到期及版本；remaining=total-captured-released |
| 原子 capture | POST `/api/v1/external-billing/charges` 带正 hold_id；同事务先锁 hold 再锁 user，同时减少 quota 与 held_quota、更新 hold、charge、ledger 和幂等结果；available 不变。不得先 release 再普通 charge |
| capture 异常 | 错归属 HOLD_MISMATCH，过期 HOLD_EXPIRED，冻结不足 422 INSUFFICIENT_HELD_QUOTA；不足可先 topup 同一 hold，再原 key、原业务号、原 body 重试 |
| 业务号冲突 | 同商户同业务号异用户、金额或 hold 返回 409 BUSINESS_REFERENCE_CONFLICT，不得换键绕过；原参数指纹保留 |
| 创建 hold | POST /holds 六字段 merchant_id/merchant_hold_id/user_id/hold_quota/expires_at/reason；金额和请求 expires_at 为规范十进制 int64 字符串，expires_at 为毫秒 |
| topup | POST /holds/{id}/topup 带 merchant_topup_id、user_id、topup_quota、reason；返回 hold_id、总额/captured/released/remaining、hold_status、balance_version；topup 不等于延期 |
| release | POST /holds/{id}/release 带 merchant_release_id、user_id、release_quota；超过剩余时截断，响应 released_quota 为本次实际释放额；同业务号同参数重放返回原结果 |
| 恢复入口 | GET /holds/{id}、/holds/by-reference/{merchant_hold_id} 查询明细；GET /hold-operations/{op_type}/{merchant_business_id} 查询 HOLD/TOPUP/RELEASE 操作。所有写请求带 Idempotency-Key |
| 时间与过期 | 明细 expires_at 为 Unix 毫秒，created_at/updated_at 为 Unix 秒。到期拒 capture、仍允许 release；没有自动解冻承诺，不刷新重试快照到期值 |
| 撤权善后 | 撤权后拒新 hold/capture，原商户、原用户的 release 和纠错 refund 允许善后；失效商户凭据仍拒绝。判权直接读授权表 |
| 退款 | 退款引用原 charge 和原 ledger，累计不超过原扣款。capture 退款回 quota；hold 仍有剩余时还可能回 held，不能把退款金额视为可用余额 |
| ledger | 新增 CAPTURE、CAPTURE_REFUND；退款后查询最终剩余量，用新的独立释放业务号处理新增退款额度，不能重放旧 final-release 当作再次全释放 |
| 权威身份 | 当前可信 SSO proof 的 /api/user/self 中 data.id 是付款身份，不接受浏览器任意 userId、OIDC subject 或 VLS shadow id 代替 |
| 计量与历史版本 | 安全停机请求不等于停止；提前请求停机并计量至有证据的实际停止。历史兑换版本不重算；INVALID_REQUEST 保留原快照进入异常核验，不自动改版本 |

附件实现方案第 11 节的旧“只支持冻结总额”文字属于残留，不再阻塞上述核心开发；
以新版逐 hold/capture 契约开发，部署的实际行为仍需下面的验收。
定价沿用现有已确认规则与接受的报价快照，本次不新增倍率或改动成本来源。

## 尚未定义完整响应的部分

1. **hold-operations 的完整响应。** 已明确路径及去重规则，但未提供成功/处理中/拒绝/查无的
   完整字段与机器码。必须能够核验操作类型、原业务号、hold、用户、请求额和实际额；
   在此之前，VLS 对该查询返回未确认状态，不能从任意 HTTP 200 推断操作成功。
   hold 当前明细也不能代替某一 topup/release 的原始操作回执。
2. **capture 查单的 hold 关联证据。** /charges 正式请求已明确 hold_id，但附件中
   /charges/by-reference 成功示例仍未列 hold_id。VLS 会保留已收到的 POST/查单证据；
   对绑定 hold 的区间，缺少可核对 hold 关联的详情时不能将其记为核验完成。
3. **退款未知结果的完整查单。** 退款成功回执字段已列出；按退款业务号查询路径未定义，
   operations/by-key 的示例目前只展示 CHARGE。不能凭空定义 REFUND 成功字段，
   不能在未知退款结果后自动换键或新建退款。需要实测或补齐明确的退款恢复响应。

这些局部限制采用保守状态和隔离回归处理，不阻止 DTO、适配器、本地请求日志与状态机开发。

## 待实测，一次性交接

| 验证项 | 需要的证据 |
| --- | --- |
| 共享钱包原子性 | 目标数据库中模型预扣/流式差额、GPU capture、缓存/批量及管理减额混合并发；quota、held、available 非负且流水、幂等和业务单同事务，注入故障整体回滚 |
| hold 并发与重放 | 同 key/业务号同参不重复，异用户/金额/hold 冲突；capture/release/topup 并发不超剩余；不足补冻后原 key/body 成功，无“先释放后扣款”窗口 |
| 到期与授权 | topup 保持原 expires_at；到期 capture 拒绝而 release 可执行；确认无自动解冻假设；撤权与 capture/release/refund 并发先后顺序及商户失效拒绝 |
| 恢复与退款 | 提交后丢响应、5xx/503、进程重启，恢复原快照；部分退款及上限、回 held 的最终剩余查询与新 release、旧 release 重放不释放新退款 |
| 权威查询 | self.id/scopes、hold 详情及操作查单完整响应、capture hold 关联、历史 conversion_version 的受理/拒绝，ledger 分页和不可变流水逐条对账 |
| VLS 安全生命周期 | 真实供应商实际停机证据、停止前尾账、未知区间和未归档任务保护；请求停机或模拟 STOPPED 不代替真实完成 |

真实资金联调本次没有执行。附件仅给出占位配置，未给可用测试 host、指定 merchant/user、
现成受限身份或无现金账本与金额/次数范围的证据。后续由父任务统一交接，通过批准的安全配置渠道提供，
不在本文或聊天粘贴 key、token、密码，不开户、不采购 GPU，不直接联系第三方。

本地 fake HTTP/H2 与 Maven 通过只证明 VLS 代码的隔离行为；不能声称中转站已上线或
模型与 GPU 的真实共享钱包并发验收完成。收费与写开关默认 OFF，未部署、未重启共享服务、未迁移真实数据库。
