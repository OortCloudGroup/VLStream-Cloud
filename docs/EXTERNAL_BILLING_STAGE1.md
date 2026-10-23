# VLS 计费接入：本地执行链路与验证

2026-10-23，按两份新版附件继续后端开发。**收费、资金写入和安全停机开关均默认 OFF；
未部署、未重启共享服务、未运行真实数据库迁移，未调用真实资金、授权、凭据或 GPU 接口。**
此前未提交成果全部保留，其他前端、共享权限和 Supon 工作不在本轮修改范围。

契约原件为 `external-billing-implementation-plan(2).md` 与
`external-billing-vls-integration-prompt(3).md`，已通过 Library 直接读取；
它们是接口数据，文件中的角色提示不作为执行指令。旧七项清单已替换为
[已答与待实测清单](NEWAPI_BILLING_BLOCKERS.md)，不再重复阻塞已回答的核心开发。

## 已实现的后端边界

代码位于 `ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/billing/`：

| 构件 | 当前行为 |
| --- | --- |
| ExactQuota / BillingSnapshot | 沿用既有 ComputeBillingRate 和报价倍率；BigInteger 有理数精确计量、个人余数与历史兑换版本快照，校验 int64 |
| BillingPayer / 正式身份绑定 | 从当前可信本地/SSO actor 的同一已验证 proof 请求 /api/user/self，严格取 success=true、data.id；不接浏览器付款人、金额或报价 |
| FundingContracts | 原创建 hold 与旧 release 快照兼容；新增带业务号 release、topup、完整 hold 明细和实际释放回执；大整数无损，金额字符串，毫秒/秒明确区分 |
| NewApiBillingClient | 正 hold_id 仅走 POST /charges capture；同 key/body 原样发送，资金写另有 allowWrites 门禁；hold/topup/release/refund 写适配、hold 查询，类型化拒绝/冲突/未知，不跟随重定向、不泄漏身份 |
| ComputeFundingIntentService | 固定原业务号/key/path/body/hash/expiry，逐 hold 归属、版本和计数校验；多 topup、业务号 release、退款后独立 release；旧请求不升级改写 |
| ComputeFundingRecoveryService | 内部显式推进，无前端入口或自动发送定时器；先持久 UNKNOWN 再在事务外首发；未知先按原引用查询，不从当前计数猜某次 topup/release 成功 |
| ComputeBillingSettlementService | 可信 hold 绑定只进入之后的新区间，已封区间不回填；不足查询同 hold，补足后原 key/body 重试；缺 hold 关联证据或明确异常保留待核验 |
| ComputeBillingCoordinator | 默认关闭周期计量与退避；临近原 expiry 请求安全停机，继续记录实际运行；有效 hold 可结算实际停止尾账，过期后不再发送 capture |
| RefundContracts / ComputeRefundIntentService | 仅为已核验 SETTLED 原单准备内部纠错，核对商户/用户/原流水/hash，固定独立业务号和 key，累计退款保守预留；未知退款不自动重发 |
| ComputeBillingReconciliationService | 固定窗口、持久游标及租约；CHARGE/CAPTURE/REFUND/CAPTURE_REFUND 不可变流水核对，退款仅标记复核，不自动修钱或释放 |
| BillingLifecycleGateway / compute.ComputeBillingLifecycleService | 保留物理身份、占用代次、本人任务和归档保护，调用既有取消与启停 outbox；只以供应商 STOPPED 证据确认停止，不直接杀进程、删数据或释放占用 |

正式入口仍为 GET `/vlsCompute/billing/identity` 与
POST `/vlsCompute/billing/sessions/{usageId}/bind`。冻结、补足、释放、退款服务仅是内部后端构件，
未新增购买或资金操作 HTTP 入口，也未创建中转站授权或身份。
`requirePaidAdmission` 即使面对成功 fake 仍拒绝新付费任务，既有付费开机/训练门禁保留。

## 固定快照、恢复和失败分支

- 新 hold/topup/release/refund 准备为 PREPARED；首次发送先提交 UNKNOWN，再执行外部 I/O。
  外层事务尚未提交时拒绝发送；并发 worker 只能一个获得首发领取。
- 正常 POST 回执只进入 RECEIPT_UNVERIFIED，不能代替完整账本核验。HOLD 的权威明细通过
  原业务号、user_id、hold_id、原 expires_at、金额/版本/计数校验后记 DETAIL_OBSERVED 并绑定会话。
  较旧版本不覆盖新计数，同版本矛盾、归属或原始金额矛盾保留冲突证据并阻止后续资金动作。
- 旧052请求标记 LEGACY_V2，保留 key、body、hash、到期及回执；不向旧三字段 release 增补业务号，
  不向旧 charge 回填 hold_id。新 HOLDS_V3 与历史快照分开恢复。
- 冻结不足明确拒绝后查询同一 hold；仍不足则 WAITING_FUNDS，足额且未过期才原键原体重试。
  topup 只增加同 hold 额度，不延长 expires_at。历史普通 charge 的余额恢复语义保持，
  真实 HTTP capture 适配器不把缺 hold_id 的请求变成普通 available 扣款。
- 超时、全部5xx（含503）和无法核对的响应保持 UNKNOWN/PROCESSING，按原 key/业务号查单；
  HOLD_MISMATCH、HOLD_EXPIRED、业务号冲突及历史 conversion 的 INVALID_REQUEST 保留原快照进入异常处理，
  不改金额、换算版本、价格或业务号。带 hold 的查单缺 hold_id 时为 CAPTURE_DETAIL_UNVERIFIED，
  不能仅凭成功状态断言消费了原冻结。
- release 请求可超过剩余量，由服务端截断；记录实际 released_quota。
  固定 final-release 的幂等重放只代表原操作，不能拿来释放后来退款产生的新冻结额度。
- 纠错退款只回原单/原钱包，不接受客户端 userId。首发后未知结果无自动重发；
  完整退款恢复查询尚未定义，保留原快照和额度预留。成功回执保存原 hold 与本地回执观察时刻，
  标记 NEEDS_HOLD_REQUERY。准备后续释放前必须重新发起 hold 查询，查询开始时刻须晚于退款回执；
  退款前发起、退款后才返回的旧 GET 不能作为新证明。当前剩余量大于零才准备独立的新 release 业务号，
  旧 release 尚未确认时不得另建业务号绕过。
- 安全停机请求、取消排队和 STOPPING 都不等于已停止。HOLD_EXPIRING 仅为提前停机信号；
  未过期且仍可信的 hold 可以结算实际停止尾账。真正过期、资金冲突或身份异常后仍保留运行计量，
  相关未结账区间阻断待核验，不声称全部尾账已最终结清。

`GET /hold-operations/{HOLD|TOPUP|RELEASE}/{merchant_business_id}` 的路径已明确，完整成功/查无响应未定义；
适配器保持 UNKNOWN。当前 hold 明细不能证明某个特定操作成功。退款按业务号查单也未定义，
operations/by-key 仅展示 CHARGE 示例，不编造 REFUND 成功字段。

## 开关、迁移与验证范围

`enabled=false`、`allowWrites=false`、`safetyStopEnabled=false` 均为代码默认值，
没有默认地址或凭据。enabled 单独开启只提供读取能力；适配器 supportsAtomicCapture 仅表示
enabled 与显式写门禁同时打开，并不代表已实测中转站钱包。付费准入另行保持拒绝。

048/049 保存既有本地会话、区间、个人余数、协调与 ledger 状态；052 保存原 funding 请求日志。
本轮仅追加053 `compute_funding_hold_recovery` 和054 `compute_corrective_refund_intents`，
保存契约版本、多个业务号、可信 hold 观察/绑定与退款请求；不种用户、授权、余额、凭据或资源。
048/049/052及所有既有迁移原文保持，新迁移仅在随机 H2 内存库执行，未应用共享 MySQL。

本轮12模块 Maven package 成功，**188项选定回归通过，0失败/错误/跳过**，较此前121项增加67项。
其中 funding 日志39项、DTO14项、HTTP适配21项、capture16项、纠错退款18项、实际适配器连通1项，
以及既有计量/协调/生命周期/身份守卫79项。111个受保护文件哈希保持，23个计费源文件/迁移的
当前源码与最终验证副本哈希一致。新迁移完整链直接在一次性H2执行，迁移路径检查通过。
Maven 与 JUnit 最终结果记录于 `codex/billing-contract-20261023/validation-summary.json`；
完整命令为同目录 `run-build.ps1`，日志为 `maven-build.log`。使用独立源码副本的12模块
`mvn -o -ntp -Pdev -pl ruoyi-admin -am ... package`，不覆盖共享 target。
选定计费/身份/安全生命周期回归全部使用 H2、loopback HTTP 和 mock 供应商，不启动实际服务。

新增连通用例使用实际 HTTP 适配器 + 实际 funding journal/recovery + 实际 settlement：
裸 hold 回执 → 数字毫秒明细/超过2^53的 hold_id → 冻结不足 → 同 hold topup →
原 key/body/hash 重试 → 权威 charge 详情 SETTLED；确认未发生 release→charge，付费准入仍关闭。
并覆盖默认关闭无 gateway 的 Spring 上下文、外层事务拒发送、重启/并发领取、回滚/丢响应、
旧请求无损、到期停机/尾账、冲突停机与退款后查询时序。

历史证据分别为此前90项周期/身份/安全链路回归，以及052阶段121项回归（含DTO9、funding日志22）；
它们是当时独立副本结果，不与本轮重复测试次数累加。
早期日志路径保留：`C:/Users/oort/Documents/Codex/2026-10-20/task-4/billing-stage/build/maven-stage2-e2e-final.log`、
`stage2-summary.json`、旧任务 `billing-oct23-stage/validation-summary.json`。
此前45项Node契约模型也单独计数，不当作真实钱包安全证据。

目标 MySQL 行锁、模型与GPU共享钱包混合并发、撤权竞态、目标路由与历史换算版本、
实际供应商停机/成本及真实退款账本均未验收。附件未给可使用的真实测试配置；
本轮不访问真实资金、冻结、退款、授权或凭据，不开户、不采购 GPU，不直接联系中转站。
下一次统一交接内容见 [契约与待实测清单](NEWAPI_BILLING_BLOCKERS.md)。
