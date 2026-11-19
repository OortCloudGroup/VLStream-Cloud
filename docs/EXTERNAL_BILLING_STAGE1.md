# VLS 计费接入：本地执行链路与验证

## 当前产品与配置边界（2026-11-06）

算力租用 → 平台训练 → 停止 → 结算是本仓库的正式常驻业务。单租户和多租户部署共用 `/compute-market` 与 `/vlsCompute/billing` 入口，租户模式仍由 `token.tenantType` 决定；单租户本地登录用户可通过正常平台授权取得本人付款身份。没有 `external-billing.mode`，也不再使用 `enabled`、`allow-writes`、`paid-admission-enabled`、`safety-stop-enabled` 或 `official-autodl-control.enabled` 控制这条业务。

常驻入口不会自动查询钱包、接受报价、冻结或开机。读取余额前仍须用户明确接受平台计费权限；租赁须重新核对当前可信 `self.id`、本地用户和租户，展示服务端固定报价、预冻结、最长运行时间及安全边界，并由用户明确接受本次报价。菜单权限、本人归属、可信租户和原单恢复规则继续生效；本地管理员、VLS 影子用户、商户身份及浏览器自报 ID 均不能替代付款人。

能力由真实配置和安全政策推导：权威用户身份查询只需合法 HTTPS 目标和当前可信用户 proof，商户查询与资金操作还须商户 ID 和商户凭据；新付费准入须有效运行时限、冻结上限、结算周期、本地停机预算、轮询余量及覆盖预算的提前量。官方 AutoDL 目标、API 身份和固定整机价格由 `company-resources` 的同一资源配置提供。能力响应不再返回部署 `mode`；缺材料或无效政策明确指出对应配置项，不能显示“尚未开放”或把缺身份视为可付费。既有订单读取、停止及原资金善后不会因新准入政策缺失而被额外关闭；缺有效商户身份或权威证明时仍拒绝不能核验的资金动作。

`cloud-control.enabled` 继续用于既有非官方资源／其他供应商的通用控制边界，不是官方付费流程的前置开关。`external-billing.trial` 和 `autodl-local-ssh` 仍是隔离 DEV 验收限制，正式部署不依赖它们；危险自动删盘、故障赔付及其他业务开关不在本次改动范围。官方 API 不支持的供应商定时器、额外租赁和自动删盘没有作为准入条件扩展。

### 正常配置

沿用当前部署的安全配置渠道提供下列非公开材料和数值，不把真实凭据写入仓库。下面只有占位符；`billing-policy.stop-budget-seconds` 是本地停止计划与额度预留，须通过现有政策校验，`hold-stop-lead-seconds` 覆盖该预算，技术运行上限、可选的绝对冻结上限、轮询余量和接受时固定的绝对截止继续生效；实际冻结额度与运行窗口按当前用户可用余额推导，余额用完前用户可自行决定何时停止。冻结到期时间由原报价的运行与安全预算推导，不另加 TTL 开关。预算不是供应商 SLA、已观测停机上限或费用硬封顶；实际计费持续至确认物理关机，超时与 UNKNOWN 继续按原单恢复，不能提前释放冻结或伪称停止。

`verified-provider-stop-seconds` 是可选的已有停机观测证据，缺少证据不阻止首次使用本地有效预算；若提供，须为正且与 `polling-safety-seconds` 之和不超过本地停机预算。不得为通过校验填写猜测值，也不把一次观测写成供应商保证。

```yaml
vlstream:
  compute:
    external-billing:
      base-url: ${VLS_BILLING_BASE_URL}
      merchant-id: ${VLS_BILLING_MERCHANT_ID}
      api-key: ${VLS_BILLING_API_KEY}
      # 技术上限；实际运行窗口由用户可用余额推导
      maximum-run-seconds: ${VLS_BILLING_MAXIMUM_RUN_SECONDS}
      # 可选；留空或 0 表示不设固定上限
      maximum-hold-quota: ${VLS_BILLING_MAXIMUM_HOLD_QUOTA:}
      hold-stop-lead-seconds: ${VLS_BILLING_HOLD_STOP_LEAD_SECONDS}
      settlement-interval-seconds: ${VLS_BILLING_SETTLEMENT_INTERVAL_SECONDS}
```

官方资源继续使用既有 `company-resources.resources` 中的 `provider: autodl`、稳定 `node-id`、`instance-id`、`account-scope`、`provider-api-token`、可信 SSH／动态连接材料及 `fixed-price`；这些字段必须对应同一目标实例。不要重建资源或更换稳定 ID 来绕过旧单占用。平台登录、验票和计费服务须属于同一环境，用户 SSO Bearer 与商户 Bearer 分离。删除外部 overlay、启动参数和环境映射中的旧计费模式及布尔键；它们不再提供兼容授权或启停语义。

### 验证边界与原单证据

正式常驻功能的实现、本地加载与目标环境的真实验收分别核对：**真实 AutoDL 开机 → 平台训练 → 产物归档 → 停止 → 正常结算完整链路仍未通过外部验收**。缺实测证据不转化为额外产品开关，也不能由单元测试、页面夹具或健康检查推导已经验收。

历史实测保留以下结论，不能重复消费或清理已结束原单：

- 2026-10-28 的隔离 DEV 资金 API 已完成冻结、补冻、原键 capture、退款及释放的最小循环；它不代表 Java＋GPU 产品闭环。证据为 `codex/billing-contract-20261023/isolated-test-runs/vls-lab-48ea817b-f4d3-42a7-a1be-190283d5f895.json`。
- 2026-10-30 原 run `24263603-fa7f-481c-b979-01db16bb62e7` 经过页面确认、冻结、API 开关机，训练未启动；查单流水矛盾触发保护。三笔原扣费和原退款各 54,166 quota，第四尾段未发，实际冻结已释放，订单为失败回滚 `CANCELLED`。供应商实际运行费与钱包退款独立。后续中转站查询／旧 hold4 一致性修复已经核验，不能继续称旧差异是当前开发阻塞。详见 [中转站契约与当前验收边界](NEWAPI_BILLING_BLOCKERS.md)。
- 2026-11-02 原 run `81688526-2366-443c-9808-84dd4f896d42` 开机结果为 `UNKNOWN`，未重发、未训练、未 capture；原 hold5 已按原请求一次释放，钱包 held 为零。原 UNKNOWN、资金审计及失败恢复证据保留，不能伪称正常完成。证据为 `new-trial-safe-cleanup-summary-20261102.json`、`new-trial-original-release-result-20261102.json` 和 `runtime-recovery-fix-20261102/final-verification.json`（均位于 `codex/billing-contract-20261023/`）。
- 2026-11-04 本地 mainflow 候选及删除额外安全入口的记录是旧模式时代的加载证据，不代表当前源码已加载。hold5／hold6 已由用户正常管理员入口核销，不再退款或释放；原失败和冲突审计保留。当前本地交接证据在 `codex/runtime-paid-trial-20261104/checkpoint.json`。

以下按日期保留的实现与验证记录反映各阶段事实；其中“默认关闭”“尚未接入”或模式／开关表述属于历史行为，当前产品和配置规则以上述章节为准。

## 已实现的后端边界

代码位于 `ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/billing/`：

| 构件 | 当前行为 |
| --- | --- |
| ExactQuota / BillingSnapshot | 沿用既有 ComputeBillingRate 和报价倍率；BigInteger 有理数精确计量、个人余数与历史兑换版本快照，校验 int64 |
| BillingPayer / 正式身份绑定 | 从当前可信本地/SSO actor 的同一已验证 proof 请求 /api/user/self，严格取 success=true、data.id；不接浏览器付款人、金额或报价 |
| FundingContracts | 原创建 hold 与旧 release 快照兼容；新增带业务号 release、topup、完整 hold 明细和实际释放回执；大整数无损，金额字符串，毫秒/秒明确区分 |
| NewApiBillingClient | 常驻客户端仅在有效商户配置下发起请求，正 hold_id 仅走 POST /charges capture；同 key/body 原样发送；hold/topup/release/refund 写适配、hold 查询，类型化拒绝/冲突/未知，不跟随重定向、不泄漏身份；客户端存在不替代订单准入或用户确认 |
| ComputeFundingIntentService | 固定原业务号/key/path/body/hash/expiry，逐 hold 归属、版本和计数校验；多 topup、业务号 release、退款后独立 release；旧请求不升级改写 |
| ComputeFundingRecoveryService | 由原单编排在内部推进，不暴露浏览器自由资金入口；先持久 UNKNOWN 再在事务外首发；未知先按原引用查询，不从当前计数猜某次 topup/release 成功 |
| ComputeBillingSettlementService | 可信 hold 绑定只进入之后的新区间，已封区间不回填；不足查询同 hold，补足后原 key/body 重试；缺 hold 关联证据或明确异常保留待核验 |
| ComputeBillingCoordinator | 对原订单推进周期计量与退避；临近原 expiry 请求安全停机，继续记录实际运行；有效 hold 可结算实际停止尾账，过期后不再发送 capture；新准入政策缺失不关闭已有订单恢复 |
| RefundContracts / ComputeRefundIntentService | 仅为已核验 SETTLED 原单准备内部纠错，核对商户/用户/原流水/hash，固定独立业务号和 key，累计退款保守预留；未知退款不自动重发 |
| ComputeBillingReconciliationService | 固定窗口、持久游标及租约；CHARGE/CAPTURE/REFUND/CAPTURE_REFUND 不可变流水核对，退款仅标记复核，不自动修钱或释放 |
| BillingLifecycleGateway / compute.ComputeBillingLifecycleService | 保留物理身份、占用代次、本人任务和归档保护，调用既有取消与启停 outbox；只以供应商 STOPPED 证据确认停止，不直接杀进程、删数据或释放占用 |

正常产品入口包含能力查询、显式账户检查、按节点报价、本人原单列表／详情、明确接受报价、停止与归档恢复报价；`identity` 与原 usage 绑定接口保留。冻结、补足、扣款、释放和纠错退款由内部原单编排推进，浏览器不能直接选择商户、付款人或金额。服务不创建平台身份，也不因前端显示可用而跳过报价、HOLD、安全政策和原单归属核验。

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
- 纠错退款只回原单/原钱包，不接受客户端 userId。首发后未知结果不盲目重发；
  按原引用、原 charge 和原 ledger 交叉核验，缺关联证据时保留原快照和额度预留。成功回执保存原 hold 与本地回执观察时刻，
  标记 NEEDS_HOLD_REQUERY。准备后续释放前必须重新发起 hold 查询，查询开始时刻须晚于退款回执；
  退款前发起、退款后才返回的旧 GET 不能作为新证明。当前剩余量大于零才准备独立的新 release 业务号，
  旧 release 尚未确认时不得另建业务号绕过。
- 安全停机请求、取消排队和 STOPPING 都不等于已停止。HOLD_EXPIRING 仅为提前停机信号；
  未过期且仍可信的 hold 可以结算实际停止尾账。真正过期、资金冲突或身份异常后仍保留运行计量，
  相关未结账区间阻断待核验，不声称全部尾账已最终结清。

`GET /hold-operations/{HOLD|TOPUP|RELEASE}/{merchant_business_id}`、原请求回执与 fresh hold 联合核验原操作；当前 hold 明细不能单独证明某个特定操作成功。capture 和退款需核对原引用、原流水和不可变归属，未出现过、缺必要关联或相互矛盾的响应保留 UNKNOWN／待核验，不编造成功字段。具体已实现恢复和实测边界见 [响应差异与 Java 恢复说明](NEWAPI_BILLING_BLOCKERS.md)。

## 迁移与历史验证范围（2026-10-23）

以下为当时构建和迁移证据。该阶段默认关闭的计费布尔门禁已由本文顶部的常驻业务规则取代，没有默认商户地址或凭据；`supportsAtomicCapture` 的适配能力不代表真实钱包或完整产品已经验收。

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

## 2026-10-28 API 实测后的离线修正

- 原冻结不足现返回正确的 `422 INSUFFICIENT_HELD_QUOTA`，同 hold topup 后原 key/业务号/body 的 capture4 成功。
  重放同一 capture 返回 200/SUCCEEDED，随后 fresh hold/余额仍仅扣4；
  但测试脚本在重放回执正整数校验触发历史 `INTEGER_RANGE`，**完整回执字段的幂等验收未通过**。
- 当时未保存具体重放字段值，候选仅为 `charged_quota`、`charge_id`、`ledger_entry_id`、`operation_id`。
  在旧 Node 校验中，该错误意味着正整数字段为0或超过 int64；缺字段会先触发另一种类型错误。
  后续真实 GET 的 charge_id=1、ledger_entry_id=1，operation 查询没有 operation_id，
  这些新 GET 不能还原原 POST 的字段，也不能证明发生了余额窄整数溢出。
- 退款首次400/INVALID_REQUEST、原 refund key 查单404且 refunded=0；临时脚本把原 ledger ID 发成 JSON 数值。
  在原未执行证据齐备后，只将该字段编码改为契约字符串，保留原被拒 body，原 key/ref/charge/ledger/额度不变；
  第二次 HTTP200，退款4、持有额度由1回到5，再实际 release5，最终 Q/H/A 与起始相同。
  这是本地临时脚本序列化修正，不把它归咎于账号或商户授权。
- Java `RefundContracts` 原本就输出字符串；本次在 `NewApiBillingClient.refund` 再加发送前字符串要求，
  numeric ledger ID 或 numeric refund_quota 不再发到上游，也不静默改写已有不可变历史请求。
  新回归覆盖超过 JavaScript 精度边界的字符串 ledger ID、int64 最大额度、0/越界/小数/缺失回执ID保持 UNKNOWN。
- 本次独立目录执行 Maven `-o`，12模块 package 成功；选定 Java 回归72项（HTTP23、DTO14、退款18、capture16、适配集成1）全部通过，无失败/错误/跳过。
  Node 60项离线回归通过。Java集成仅loopback夹具，不连接NewAPI；历史188项不与本次重复相加。
  日志为 `codex/billing-contract-20261023/maven-offline-final-20261028.log`，共享target未覆盖。
- Node 主脚本也改为字符串退款 ID，校验前记录脱敏回执字段；新增字段级错误，未来能准确定位失败字段。
  未放宽成功判定，未为了重现历史问题再发 capture、退款或其它网络请求。

## 产品编排前的缺口（本轮开始时的历史检查点）

| 链路 | 当前代码事实 | 可在本地独立完成的下一步 | 仍需真实环境验收 |
| --- | --- | --- | --- |
| 身份绑定 | `ComputeBillingBindingController` 已有 identity 与现有 usage bind；`ComputePlatformAuthorizationService.billingPayer` 以同一已验证 proof 获取 self.id；不接客户端付款人 | 在普通启动流程接入已有绑定入口，补本人/跨用户/租户切换/SSO失效测试；应用后台一次性身份与普通用户操作分离 | 用正常用户会话核对绑定与真实 usage/占用；不能直接用此次脚本结果代替 |
| 余额展示 | Java gateway 可读余额，但现有前端 `src/api/compute.js` 未调用 billing identity/bind，也未形成当前付款人的计费余额展示链 | 设计当前可信用户的只读余额视图与字符串 DTO、前端加载/不可用状态；余额不可用不显示0，不暴露 API key，不让用户配置商户 | 配置审批后的服务端受限身份，正常SSO下实际读/失效/撤销行为；本次不保存凭据或改开关 |
| 冻结与开机 | hold/topup/release 持久意图和内部推进服务已存在；`requirePaidAdmission` 仍无条件拒绝付费准入 | 编排当前 usage→持久 hold→权威归属/余额核验→既有开机 outbox；补未知/不足/到期时不得开机及并发去重测试 | 原子准入与真实供应商开机、实际运行计量、整机占用和故障恢复；本次未开 GPU |
| 结算与安全停机 | 周期/尾账/租约及安全停止服务已有默认关闭的代码接缝，排队不算停止 | 按原 expiry/费率/身份快照连接工作器，补重复观察、尾账、租约、停止未确认测试，保持失败保护 | 供应商 STOPPED 证据及实际停止时刻、产物归档/回读和资源释放 |
| 未知资金恢复 | 当前内部 funding recovery 对部分 hold-op 仍保守UNKNOWN；真实 REFUND by-key 已见最小状态，但未携带完整金额/原流水证据 | 以原 key/ref/请求hash结合 charge、hold、ledger 补全恢复证据；状态查询缺字段时保留 RECEIPT_UNVERIFIED，不凭余额或单独SUCCEEDED自动修钱 | 丢响应、进程重启、并发、撤销、失效商户及完整回执形状；不复活REVOKED |

以上为产品编排前的历史缺口；下面记录后续已完成的源码接入。真实迁移和部署仍未做。

## 2026-10-28 产品源码接入（本地验证，尚未加载或真实 GPU 验收）

普通用户入口为现有算力市场和训练配置。用户复用当前统一平台账户，前端不接收付款人 ID、商户配置、密钥或金额。
应用后台沿原 `ComputePlatformAuthorizationService.billingPayer` 用同一可信 SSO proof 调 `/api/user/self`，以权威 `data.id` 付款。
没有修改 main/auth/router 或当前会话判权策略，也没有自动扩大、恢复撤销的授权。

| 接口（均在 `/vlsCompute/billing` 下） | 行为 |
| --- | --- |
| GET `/capabilities` | 仅查看本地服务能力，不请求平台或产生授权 |
| POST `/account/check` | 当前用户明确接受余额/GPU计费权限后查询；此请求可能触发平台 trusted auto-grant，不能称纯只读 |
| POST `/nodes/{nodeId}/quote` | 按当前可用余额准备短时报价，确认报价后才开始冻结和占用 |
| POST `/runs/{runId}/accept` | 核对当前 self、可用余额、报价归属/有效期、可信锁价与停机条件，持久化占用、usage、会话和原 hold 意图 |
| GET `/runs`、`/runs/{runId}` | 仅本人当前租户/平台身份/付款人可查看的原单状态；不隐式查询余额或授权 |
| POST `/runs/{runId}/resume-settlement` | 已关机原单的付款人验证 NewAPI 计费授权，核对原冻结后恢复原流水对账 |
| POST `/runs/{runId}/stop` | 记录取消/安全停止意图；不把提交成功当作实例已停止或资金已释放 |

`ComputePaidRunService` 与新增空表迁移 `V1_2_0_056__compute_paid_run_orchestration.sql` 保存报价、原开机操作号、付款身份、usage 与原 hold/release 引用。
唯一 `active_instance_key` 禁止两个付费原单同时使用同一物理实例。重复接受同一报价不新建 hold/usage。
主状态为 `QUOTED → FUNDING → STARTING → RUNNING → RELEASING → COMPLETED`；取消为 `CANCELLED`，不确定结果保留原阶段与恢复提示。
取消抢在 PREPARED 发送前时不再冻结；已经可能发送则查询原资源，不替换业务单或盲重试。

冻结资源通过权威 hold detail 归属、额度、版本、到期校验并保存后，才向既有 `ComputePowerWorker` outbox 排一次开机。
计费起点来自真实供应商运行观察，不把预冻结或排队时间算成运行时间。新的 RESERVED 会话初始计量游标为0，首个已证实运行观察才初始化。
付费训练入口核对当前平台用户/租户、同一开机原单、已持有额度、原到期和安全状态；普通免费/自接/公司共享训练路径保留。
页面显示精确字符串余额及其查询时间，开机前显示服务器报价、预留额度、最长运行时限及持续授权说明；状态可在刷新后从原单恢复。

停止沿既有本人任务取消标记和回存流程推进，ARCHIVING 等待完成，不取消其他人的任务。
只有供应商证实经济停机（现有实现为 Aliyun ECS 的 `StopCharging` / `PostPaid`）才结束计费；普通 STOPPED 不足。
已证实用量按原区间/原键结算，全部区间确认后释放原 hold 剩余部分；未知 release 不重复发送，也不声明完成。
正常消费保留扣款，未消费预冻结通过 release 退回可用余额；错误扣款仍使用已有 corrective refund 原流水机制，不自动退款全部正常用量。
停机、余额释放与整机工作空间转交是不同状态。继续保留本人占用/产物；脏工作空间无清理证据时释放占用仍隔离，不自动删除或交给其他用户。

配置边界：`enabled`、`allow-writes`、`paid-admission-enabled`、`safety-stop-enabled` 均默认关闭。
启用前还需有效的单实例 `rates`（可信来源/锁价证据/时效/换算版本）、`maximum-run-seconds`、`maximum-hold-quota`、
`verified-provider-stop-seconds` 及覆盖停止预算的 `hold-stop-lead-seconds`。没有写入配置值或保存 key；不把缺配置默认为免费、无上限或可启动。
AutoDL Pro 现有源码只确认 running，价格原值仍标 UNKNOWN 单位/币种/范围，不能凭历史展示参考价绕过上述条件。

本轮最终验证：87项后端定向回归（含7项产品集成、3项账户与4项controller边界回归）、4项前端精确金额/状态回归、
5个本轮前端文件 Vue essential 校验及生产构建已通过，失败/错误/跳过均为0。H2/钱包/供应商夹具覆盖同一原单的冻结、丢响应恢复、单次开机提交、训练准入、
运行区间、本人任务取消/归档等待、停机/结算/释放，以及取消前不发送、开机失败无扣款释放、未知释放不重发。
浏览器使用真实 ComputeMarket/ComputeBillingPanel 源码、完全隔离的 API 夹具，实际点击到“已停止并完成结算／已结算4／剩余0”；
这是页面夹具结果，不是新的上游资金/GPU或真实训练验收。截图及日志位于 `codex/billing-contract-20261023/build-product-20261028/`。
Flyway 路径检查通过，056只在H2夹具执行；共享库/服务未迁移或重启，运行页面未验收加载本轮后端。

继续到真实产品验收的必要条件：明确 DEV 供应商实例和当前可信费率/锁价/停止收费证据、运行时限/总额与停止清理计划，
批准并安全配置应用后台服务身份/有限开关，再批准仅本地验收服务加载与迁移。未获得这些前不启动真实付费 GPU，
也不要求用户重复输入密钥或执行命令。旧核心API净零证明继续有效，完整 VLS 产品任务保持进行中。

## 2026-10-28 真实供应商验收只读准备

### 已实际核实与当前缺口

- 既有候选为用户已购 AutoDL Pro `pro-7920a22db144`、VLS 公司节点 `792022144331380001`。
  10月15日四类独立GPU实训、固定Python环境和产物下载曾通过；10月19日正常控制台曾显示按量CNY6.46/小时。
  这些是历史证据，不证明今天仍存在、运行、预付或当前价格。当前从既有Edge会话打开该实例列表，实际跳转正常登录页，未读凭据或代填。
  页面已保留等待用户正常登录；当前计费方式、开关机状态、有效单价、扩容磁盘日费、既有任务与定时关机状态均未取得实时值。
- 本轮在浏览器完整读取了官方 [Pro API](https://autodl.com/docs/instance_pro_api/)、
  [Pro计费](https://autodl.com/docs/price/#pro) 与 [Pro数据/磁盘](https://autodl.com/docs/instance_pro_data/) 页面。
  Pro按量计算按实例开关机区间计费，精确到秒且最低0.01元；包年包月为预付，不能仅因已购就当作免费。
  Pro只有系统盘，基础30GB为0.10元/日，扩容价格以实例页面为准，关机不消除保留磁盘的日费；扩容不能直接缩容。
  官方说明连续关机60天会释放并清空数据，不能把释放作为无损清理动作。现有资产不删除、不改计费方式。
- Pro API详情的 `payg_price` 只注释为折后按量价格，没有给出缩放单位；状态示例只给running。
  公布了power_off与release，但成功回执的null不等于已停机或结束收费；需正常页面现有查询的实际状态/时间和账单证明。
  当前适配器仍不能将未说明的值映射为可靠STOPPED，当前准入只支持已证实的Aliyun经济停机。
  即使登录后读到当前Pro价格，也不能把它冒充供应商锁价承诺；现有LOCKED_FOR_RUN/SEGMENTED_NOTICE约束还需真实证据或后续合法接入实现。

### 有限验收方案草案（未获执行批准，条件未满足则不启动）

只用上述现有实例，不采购、扩容、换计费方式或释放。正常登录后由本任务读取其当前整实例CNY小时价、payg/prepaid类型、磁盘当日日费和当前任务。
如已有非本任务运行或计费状态无法证明，不控制它。若供应商停止状态/可靠停机时限不能确认，不开机。
拟定条件为当前计算价不高于8元/实例小时、保留系统盘日费不高于1元；使用既有本地小样本、1epoch/batch2/320、固定环境/权重，不重装依赖。
拟定训练运行上限600秒，停止/查询预算360秒，**供应商计费区间总预算960秒必须包含启动等待**，不得把600秒之外的开机时间隐去。
按此上限，计算费向上取分不超过2.14元，加保守计入一个完整自然日系统盘1元，为3.14元，拟申请本次窗口总上限4元。
避免跨供应商自然日；实际价格/磁盘费超阈值、原计费类型不同或无法证明停机保障则取消本方案，不转成无上限批准。
这是待确认的预算条件，不是当前实例报价或硬停机机制的实证；若需供应商一次性定时关机保障，须先核实正常UI支持，再将具体设置与撤销纳入明确批准，当前未改计划。

平台测试账本仍只获批旧5/4/4 quota，不能直接用于这轮真实GPU业务。
若最终仍按成本×2、换算500000quota/CNY且二者有效，则上述960秒计算售价上限约4.266667元；可另提最多5元对应的测试预冻结上限，必须在实际换算核实后明确批准，不能沿用旧批准。
正常用量结算，未消费hold释放，只有确有错误扣款才按原ledger纠正；先归档并哈希回读产物，再等待本人任务终态和供应商关机/计算账单。
不删除旧目录、不释放现有实例、不把旧产物交给别人。保留现有磁盘在后续日期仍有既有日费，因此4元草案不是整台资产未来全部存储费用的永久上限；若要求未来零存储费，必须另审不可恢复的数据/实例释放，此轮不做。

### 运行目标与加载批准边界

本轮只读HTTP实际确认：8080 health UP/mode single，库 `oortcloud_workflowforms_vls_local`；8094 UP/mode multi，库 `oortcloud_workflowforms_vls`，数据库端点均为既有192.168.60.77:32443。
两服务当前Flyway均false，`/actuator/flyway`均404，不能据此声称历史表未应用或056是唯一pending。
8094实际只注册旧billing identity与sessions/bind；没有本轮account/quote/run路由，ComputePaidRunService/AccountService未观察到注册。
当前MultiTenantAuthService确实来自 `codex/multi-event-20261028/runtime-auth/candidate`，以后加载不得覆盖认证候选。
前端开发本地文件的代理指向8094；源码配置不当作3000/3001各运行进程的实测代理。

本工作项新增迁移为056；产品还依赖已存在的048、049、052、053、054及原compute/owner基础表。
执行前须用现有合法连接只读核对目标库Flyway历史和实际表；不能手工执行散装DDL、跳过未审迁移或直接打开Flyway让055等无关pending一起跑。
本轮仅尝试查看已有Navicat窗口，因最小化不能读取状态，未打开连接/执行SQL/查看连接密码；当前迁移历史准确pending列表仍未知。
在该列表和目标库确定前，不形成可直接执行的迁移批准。

最小安全配置提案：复用既有vls商户身份，不新建key；NewAPI key仅给指定DEV HTTPS后端使用，不能送浏览器或日志。
如需要跨重启保存，必须另外批准外部受限目录的Spring configtree秘密文件（例如用户LocalAppData下专用目录，绝不repo，ACL仅运行账户/SYSTEM），测试后删除本地副本；当前没有保存。
AutoDL若需开发者Token，仅请求既有实例读/启停所需能力；公开文档未证明该Token具备实例级权限，不能声称其自身只限此实例，实际作用域须先确认并纳入审批。
既有专用SSH身份可在已批准范围内供应用使用，不读私钥内容，不生成或更换现有算力加密主密钥。
默认Actuator Sanitizer用非秘密哨兵离线验证了新api-key/env命名均被遮蔽；这不代替实际运行端点配置验收，真实秘密加载前仍须确认无泄露。

新准入/有限rate/hold/时长和安全停止配置均需针对这次验收的明确批准；不恢复撤销授权，不移动已被拒的当前身份比对。
加载应只更新VLS后端的本计费候选与依赖并保持认证候选/事件/视频等字节，待multi/auth拥有者协调后再申请8094短暂停启及回滚；
不重启8080、WVP、数据库、Redis、MinIO、NewAPI或VM，不部署生产。Vite源码通常HMR，若实际是静态服务仅发布对应前端构建；未批准前不改代理/重启前端。
真实SSO付款ID、实例价格/状态、预算、迁移pending及候选路由均确认后，才申请一次完整实际验收；当前只读阶段未发资金、GPU、授权或配置写请求。

## 只读迁移审查与最小加载清单（未执行）

目标沿现有多租户验收链：VLS **8094**、`192.168.60.77:32443/oortcloud_workflowforms_vls`。
不把8080的 `_local` 库或生产/VM库当成相同目标。当前运行Flyway关闭，历史REST未暴露；现有工具无MySQL连接器，
唯一可见的已配置Navicat窗口最小化，窗口读取失败。未读取启动配置中的密码、进程环境或连接秘密，未另建/猜测DB身份，未执行SQL。
因此以下是精确脚本集合和作用审查，**不是对真实库pending状态的断言**。
纯SELECT历史与结构审计已准备于 `codex/billing-contract-20261023/migration-history-readonly-20261028.sql`，仅供本任务后续通过既有合法连接执行；不让用户手工运行。

| 版本 | 脚本作用 | 风险与回滚边界 |
| --- | --- | --- |
| 048 | usage增加billing_mode默认WHOLE_PERIOD；新建session、account_remainder、slice三个空表及唯一键 | ALTER需元数据锁；新列会为旧行提供默认语义。旧整周期记录不删除；回滚应用时保留表/列及审计记录 |
| 049 | session增加5个控制/停止/轮询字段，slice增加2个重试字段；新建reconciliation、ledger_seen | ADD COLUMN和建索引可能阻塞；不改原金额。不能在已有slice后通过删表“回滚” |
| 052 | 新建funding_intent，保存不可变原键、body/hash、HOLD/TOPUP/RELEASE意图；初始唯一键为usage+operation | 空表创建不发资金或授权；有记录后必须保留，不能丢失未知资金意图 |
| 053 | intent增加contract_version默认LEGACY_V2和hold_balance_version；旧唯一索引改为usage+operation+reference；session增加4个hold绑定字段；新建hold_observation | 含DROP INDEX再ADD UNIQUE，MySQL隐式提交，必须停相关写者；部分失败不能盲重跑/repair。出现同类多原单后，旧唯一键可能已无法恢复 |
| 054 | 新建corrective refund意图表及按纠正引用/原退款键去重的索引 | 不自动退款；有原单后不能删表或归零请求状态 |
| 056 | 新建paid_run报价/开机编排表；active_instance_key、usage、start_operation唯一 | 不种子账户、商户、费率或凭据；唯一键保证物理实例只有一个活跃付费原单。上线后保留历史审计 |
| 057（本地新候选） | 原paid_run增加start/stop发送状态和尝试时间4列；配置资源以订单保存电源原单 | 不创建供给记录、不发送HTTP、不存凭据；需056，实际历史/列先审计，不能重复执行或伪造Flyway历史 |

上列均无DELETE/TRUNCATE、删列、覆盖原请求或钱包数据更新；**不等于零风险或事务可回滚**。
当前脚本以正常Flyway顺序为前提，不普遍支持重复执行；若历史显示成功但实际列/索引不符，或表已手工存在但无历史，必须先查明差异，
按仓库规则新增更高版本的纠正迁移，不改旧文件、不造历史、不自动baseline/repair。
前置035/038的usage、039的public-instance占用、043的凭据身份和047的公司节点关联等基础结构也必须存在。

精确批准的前置条件：只读取得全部version/success及上述结构元数据，比较实际缺失集合D。
若D仅包含上表本计费版本，可只申请这些实际缺失版本按顺序迁移至057；若D还含050/051/055或更早无关版本、failed条目、结构漂移，
必须把它们的具体作用另外列出后重新形成范围，不跳过它们或把全部pending默认为获批。当前D未知，尚不能给出最终可执行集合。
不以打开全局Flyway开关代替这份准确批准，也不拿H2通过当真实MySQL已迁移。

准备一次明确批准时，应同时写明以下操作边界：

1. 经审计确认的准确迁移编号、目标8094 DEV库、执行前备份及验证；迁移期间暂停计费/训练写入，不触碰认证、角色分配或其他业务初始化。
2. 仅在源码/运行manifest一致且multi/auth拥有者协调窗口后，短暂停启**8094一个VLS后端**。
   基于其届时最新已批准class/配置叠加计费候选，保持认证最新class哈希、事件代码和独立MQTT身份；不从整个脏工作区重新覆盖主JAR。
   不停8080、WVP、MySQL、Redis、MinIO、NewAPI、GPU或VM，不改前端代理抢占视频验收。
3. 后台身份配置为现有DEV `vls` key的受控来源，加本轮明确额度/时长/实例/安全停止设置；不创建新key或变更trusted/regrant。
   若需保存secret，另明确批准**仓库外**专用受限configtree文件/等效安全存储、运行账户读取、使用期限和本地副本清理；目前没有保存。
   不把key写命令参数、日志、前端或证据；AutoDL账号级Token的实际权限尚未知，不冒称它只有实例级最小权限。
4. 开启新资源准入前验证当前self付款映射、精确新路由、配置哨兵遮蔽、目标数据库/迁移及实际供应商保护。资金和真实GPU操作仍按单独有限批准执行，旧5/4/4不自动扩围。

回滚首先关闭新准入，保留原资金与供应商恢复执行能力。若已有可能发送的冻结/扣费/开机，必须先按原引用查单、确认安全停机并完成原单对账/释放，
不能直接撤回新代码导致活跃资金/GPU失管。之后只回退到事先保存且包含multi/auth最新已批准修复的运行候选/配置；
新增表、列和请求日志保留，数据库结构纠正用新迁移，不能靠DROP或手工改Flyway历史。无资金活动时也须保留完整备份/manifest和健康/模式/认证回归证据。

本轮为文档和只读准备；业务流已复核未变化，没有DDL、身份读取、Flyway开关、服务加载或资金/GPU操作。

## 2026-10-29 恢复后的实时核查与源码回归

用户恢复任务并正常登录后，通过同一个 Edge 控制台页面重新核实：唯一 Pro 实例为西北B区
`pro-7920a22db144`，RTX 6000D 84GB ×1、25 vCPU、92GB 内存，当前已关机、健康正常、按量计费。
详情当前显示配置费用 **CNY6.46/小时**（划线原价6.80），日常费用 **CNY0.67/日**（划线原价0.70），
系统盘基础30GB、付费扩容50GB。这是本次页面实测，不再沿用历史报价；不是供应商锁价承诺。
基础镜像标签是 PyTorch2.8/Python3.12/Ubuntu22.04/CUDA12.8，不能据此推断历史专用训练环境已被替换或仍可用。

官方 Pro 计费规则再次核实：计算按开关机区间、精确到秒、最低0.01元；系统盘单独按日计费，关机不消除保留磁盘费用。
正常UI提供定时关机的服务器时间和绝对时间输入。本轮只打开查看并取消，未选择时间或提交，不把入口存在当作定时保障已生效。
未开机、变更配置、扩容、租新实例、释放或删除数据，未读取/提取登录令牌。

有限消费提案仍未获执行批准：仅此实例，训练最多600秒，**从开机请求开始到确认关机总窗口最多960秒，包含启动和停止等待**，
采用旧草案的计算单价上限8元/小时、磁盘不超过1元/当日，计算向上取分2.14元，加保守整日日费1元，窗口总预算申请4元。
按本次显示6.46及0.67估算为1.73+0.67=2.40元；估算不是结算保证。避免跨供应商自然日。
批准后的执行还须有经验证的停机/超时保护，不因批准预算就跳过能力缺口；异常立即停止本人训练，查询原操作并确认实例已关机，
保存本人产物后按原资金引用结算与释放。保留现有系统盘，后续仍有既有日费，4元不是整个资产未来磁盘费用的终身上限。

用户已明确官方算力的最新规则：用户售价来自本方部署YAML/环境变量，供应商报价或变价不参与用户扣费，成本差由平台承担。
官方供给不走算力管理CRUD或数据库供给回退；两台免费资源保持原行为。数据库只可保存订单、资金及电源操作状态，不能恢复被删除的供给记录作为配置来源。

本轮已实现固定价基础：`ComputeBillingRate`新增VLS_FIXED/OFFICIAL_CONFIG及明确的配置身份、币种/整实例小时固定价；
`BillingSnapshot`与用量账本按固定售价保存/计量，不读供方成本、不叠加原倍率、不声称供方锁价；配置更新不改变在途快照。
固定价必须绑定同一配置管理资源，旧数据库节点不能冒充官方配置供给。旧Aliyun价格与停机行为保留。

停止状态技术核实已有新依据：从正常Pro页面已加载的官方公开脚本
`https://www.autodl.com/assets/pay-type.f4f44ce2.js`（SHA256 0804e9613c876708aae5a7be2871520d3e6a44649ca8a2a1b1cac851acedb7a4）
直接读到starting=开机中、running=运行中、shutting_down=关机中、shutdown=已关机。
适配器仅当status与同一实例list记录一致时，将明确shutdown映射为物理STOPPED；其它错误/迁移状态仍保守UNKNOWN。
状态读取不获取含SSH/Jupyter秘密的snapshot。官方用户账本在确认物理关机时结束，不借用Aliyun StopCharging，也不把供应商预付或持续磁盘成本转嫁给用户。
这是公开实现证据加离线回归，未执行真实开发者API关机/开机调用，不能冒称真实链路已验证。

运行状态也有新证据，以下覆盖上节10月28日快照：本轮两个本地端口8080(single)、8094(multi)均指向
`192.168.60.77:32443/oortcloud_workflowforms_vls`，Flyway仍关闭；**8080现在不是隔离的_local库**。
8094的实时Actuator mappings已注册9条billing路由，包含capabilities/account/check/quote/runs/accept/stop。
只证明路由已加载，不证明完整产品可用。随后只读bean元数据定位到本地target/classes；磁盘class哈希另存验收记录，不能把磁盘文件自动等同已载入内存的精确字节。
逐项非秘密属性读取404并非开关值证明；后续实时conditions明确NewApiBillingClient的enabled=true条件未满足（did not find property 'enabled'），客户端未创建，因此外部计费通道未启用。
其余独立开关未取得实时值；本轮候选没有加载。未读取配置秘密、进程环境或登录凭据。
本地数据库历史/实际pending仍未知；VM部署记录中的056成功不能替代本地库审计。未执行SQL、迁移、配置或服务加载。

新YAML供给及前端静态规格改动保留。对比10月28日计费快照，ComputeMarketService/ComputeNodeService已变化，旧48类overlay不再适合直接加载。
以最新源码新建隔离构建副本、无共享target写入；应用运行配置与秘密不复制，只保留公开company-resources子树供绑定回归使用。
早期112项与防护阶段141项的记录保留在原日志；最终固定价基础回归见同一JSON记录及official-fixed-price-foundation-verified.log，未重放已净零的钱包测试。

本轮新增的本地防护：报价保存停止/轮询安全余量，绝对预算覆盖启动等待；预算耗尽的报价不能创建占用/冻结，调度器到期请求停止并拒绝新训练。
未向供应商发送的start以同一原操作CAS撤销后释放原hold；已发送但结果未知的start保持原操作和hold，不重发、不释放、不因时间流逝宣称失败。
迟到RUNNING立即进入原单停止流程；后续明确NO_STOCK等失败证据仍可清理原hold，未知stop保留待确认状态而非重复提交。
这些防护没有启用收费或执行迁移。归档未完成、身份变化、未决start和供应商不可用仍可能阻止物理关机，代码只提供停止请求边界，不能把960秒或4元提案当硬保证。

固定价基础阶段基于完整当时源码、无旧事件文件替换的12模块构建成功：149项报告中148通过、0失败/错误、1因未启用独立MySQL测试库而跳过。
包含13项付费原单集成、11项AutoDL协议、19项价格/停机策略及25项YAML集成；固定用户价不受供应商价/倍率影响、历史快照不重价及Pro关机映射均已离线验证。
MySQL库存集成未执行，不能声称真实开发库或供应商链路已验收。本轮只在隔离目录构建，未覆盖当前服务class、认证、算法、事件或VM文件。

后续本地候选已补齐官方配置供给：`ConfiguredComputeRegistry`保留两项免费self资源，新增`official-autodl`、规范ID `-6000`，
以相同YAML子树提供Pro实例、受控连接引用与固定售价。用户消息`Sentinel_76a4fa8bc6bc81919082b25de6a983d8`确定13元/整实例小时，
默认配置为13.00 CNY，可用`VLSTREAM_OFFICIAL_AUTODL_PRICE_CNY_PER_HOUR`覆盖；不从供方6.46或旧倍率推导。
公开市场/选择器只投影明确的VLS_FIXED、CNY、PER_INSTANCE_HOUR和字符串userHourlyPrice，缺价拒绝报价，不建立资源管理DB行。

`ConfiguredPaidPowerService`使用原paid_run的057新增列保存PREPARED/SUBMITTED/ACCEPTED/UNKNOWN/CANCELLED状态及首次尝试时间；
先提交SUBMITTED再发电源HTTP。重启或未知回执只查原实例，不重发开机/关机；控制身份、目标指纹和原公共占用仍严格核对。
仅当前同一实例的有效开关机事件时间可作为PROVIDER_EVENT，不支持的时间回执保留观察证据，不能用历史事件倒推本轮。
训练入队冻结配置目标，验证付款会话、本人/租户和可信模型来源；停止等待原任务退出及归档，再关机、结算尾段、释放未用冻结。
迟到READY不会覆盖已停机或待确认电源状态，健康探测不写不存在的compute-node行。

曾出现的正常循环STOPPING失败已定位为测试Wallet把任意业务引用映射到上一条回执，生产的业务引用冲突校验正确拒绝。
测试Wallet改为按原业务号/幂等键保存独立回执，重放同body不重复扣，改body冲突；未放宽真实回执校验。
补充短任务回归后另修复首次计量前已关机的边界：从已确认的原开机时间初始化计量游标，按有效停止证据封尾，不能从epoch零生成区间。
训练模拟夹具也补回真实表的QUEUED默认值，以检验取消/归档门禁，而不是让NULL状态绕过查询。

运行前仍缺的事实与批准：共享开发库实际Flyway历史及结构审计、准确缺失版本集合、仓库外受控身份配置、协调后的本地候选加载，
以及此实例的有限真实消费与停止/失败清理方案。官方SSH地址、主机钉住公钥、私钥文件引用/指纹和AutoDL API Token均未填入真实配置，
NewAPI key也未保留。不能把本地候选、临时fixture、页面截图或057 H2执行当作运行服务已更新。
供应商价格单位、变价或整次锁价不是固定用户价的准入条件；实际供应商成本和可靠关机仍独立验收。

官方配置原单集成的最终相关回归：12模块BUILD SUCCESS，160项中159通过、0失败/错误，1项独立MySQL库存测试因未配置而跳过。
其中官方11项涵盖配置供给无节点行、固定报价、冻结、电源原单、短任务尾账、训练入队/归档等待、未知停止跨worker恢复及释放；
旧付费原单13项和原YAML25项也通过。35份相关生产/测试/迁移源码与构建副本SHA256一致。
前端5项金额/报价回归、5文件显式Vue3 essential lint（0错误/警告）和`npm run build`通过；全仓默认lint未宣称通过。
实际Vue市场/账单组件在完全隔离API夹具完成13.00报价确认、原单显示、余额、运行及停止释放终态，截图保存在原证据目录。
这不是供应商、真实SSH训练或NewAPI本轮实测；页面训练路由是夹具占位，真实训练入队/归档门禁由后端H2组合测试覆盖。

证据：`codex/billing-contract-20261023/resume-readiness-20261029.json`、
`verify-current-product-20261029.ps1`、`build-product-20261029/official-final-regression.log` 和 `build-product-20261029/official-fixed-ui-final.png`。
完整付费GPU/训练产品实体验收仍未完成。057仅在H2测试执行，未改共享数据库、运行class、计费开关、凭据或供应商状态，
没有新增资金请求、GPU动作、服务重启或VM发布。不能以本轮mock闭环代替目标环境加载和真实开机训练验收。

### 真实加载准备审计（2026-10-29 10:20 UTC 快照，尚未加载）

8080(single)与8094(multi)实时health均UP，master都为`192.168.60.77:32443/oortcloud_workflowforms_vls`，Flyway=false。
两个服务均未创建NewApiBillingClient，conditions明确缺enabled=true；四个单项开关GET的404不当作false值证明。
两个服务只观察到`oortcloud-free-gpu-1`、`oortcloud-free-gpu-2`，未观察到第三项官方资源，也未创建新增配置电源/SSH身份bean。
这些事实证明当前官方收费通道尚不可用，不能证明磁盘上不存在既有商户或供应商秘密；本轮没有请求任何秘密属性、余额或供应商接口。

最初两个Actuator不暴露flyway端点，且无无需提取凭据的现成数据库工具；这不是数据库拒绝访问，未反复操作不可截图的Navicat。
随后用户消息`Sentinel_bf27a42ec3008191bf5f0491a6785e05`明确批准仅在程序内读取既有开发数据库配置并向对应库认证，执行原四条元数据SELECT。
独立Java程序直接读取当前classpath的开发配置，处理其多段YAML，使用当前8080非秘密URL属性校验准确目标；不启动Spring或调度，
凭据不经命令行、stdout或文件传递。初始classpath/模板解析失败均发生在连接前；修正后只有一次数据库认证与四条SELECT实际执行，
目标为`192.168.60.77:32443/oortcloud_workflowforms_vls`，readOnly=true、autoCommit=false，最后rollback并断开，进程正常退出。

实际结果见`database-metadata-readonly-20261029.json`及`database-metadata-summary-20261029.json`：65条历史全部success=1，无失败版本；
当前65个源码迁移中64个已执行，仅`1.2.0.057`未执行。额外历史`1.1.2`与`DATABASE_MIGRATIONS.md`规定的Flyway基线一致，
本次只读version/success，没有额外查询type/checksum，不冒称已经重新核验完整Flyway校验和。
035/038/039/043/047/048/049/050/051/052/053/054/055/056相关历史均成功；所查十张表共159列，
与源码迁移比较列名仅缺057的四列，其余无额外或缺失列名。完整类型、默认值的原始元数据已保存；自动差异比较只覆盖列名，不伪称完成全库结构审计。
付费原单的active_instance_key、usage_id、start_operation_id唯一索引均存在；funding的idempotency_key、merchant+business_reference、
usage+operation_kind+business_reference唯一索引也存在，原053三段去重结构已生效。

057只对`vls_compute_paid_run`新增4列：`power_start_state`/`power_stop_state`为VARCHAR(20)、NOT NULL、默认PREPARED；
`power_start_attempt_ms`/`power_stop_attempt_ms`为可空BIGINT。不新建表、不改金额、不含数据更新，前置为056。
旧8080/8094代码可忽略新增列，但ALTER有元数据锁与MySQL隐式提交风险。当前仅查元数据，活动订单未查；
此核查范围内最小迁移差异已收敛为057，不重跑旧版本，不改baseline/repair历史。
随后用户`Sentinel_70235ebcb2908191bba9196d60e6f306`明确批准备份/活动检查、057和仅8080候选加载。
活动付费原单为0、原单表0行，先保存原建表DDL、空表行快照及原迁移元数据备份，再使用现运行同版Flyway8.5.13，
target=1.2.0.057、validateOnMigrate=true、禁止baseline/clean/outOfOrder/default callbacks、lockRetryCount=0，
会话lock_wait_timeout与innodb_lock_wait_timeout均5秒。唯一pending为057，正规migrate仅执行1个版本；
历史登记success=1、checksum=1944722706，四列类型/可空性/默认值均已回读验证。
第一次Flyway辅助程序因指定console logger与该版本不兼容，在migrate调用之前失败；用无数据库连接guard定位并改回默认logger后才执行迁移，未盲重跑DDL。
最终证据和备份摘要在`migration-057-execution/migration-057-checkpoint.json`，不再次执行057，不删除已新增列回退。

已从159项通过的当前隔离构建生成`build-product-20261029/candidate-only/official-billing-candidate-only.jar`，74类，
SHA256`CA5F517A463C562C952F401EC3CC68F369386DF7FA117E6E6B8CA6EB516F5444`，精确内容见`candidate-readiness-manifest-20261029.json`。
包含计费包、官方配置/电源/SSH、必要市场/节点/健康/训练服务与计费controller家族；不含YAML、SQL、凭据、认证、算法或事件类。
6类为当前磁盘新增，34类与当前磁盘完全一致，其余34类不同；磁盘比较不冒称运行内存字节核验。
关键既有SSH/shared-queue依赖签名已核对，不把该检查当作完整运行启动验收。

算法7类overlay实时bean来源与保存清单一致，7文件hash匹配。本轮4个旧event-location class哈希读取被文件访问控制拒绝，
不绕过；不能宣称重新验证了它们的字节，但候选没有事件类，也不修改任何现有patch。
8080队列worker正在按用户批准独占重启/加载队列delta，本快照的queue路由计数为0，不作为其最终状态；
后续必须从它的最新启动清单保留algorithm+queue+event三层，不能从本轮旧快照重建参数或回退它的加载。
8094保留独立event-status patch。本轮未触碰共享target、两个服务或免费资源/前端静态规格。

后续操作应分清已授权与仍需批准的对象：

| 对象 | 精确最小范围与当前边界 |
| --- | --- |
| 本地候选加载 | 用户已明确批准，仅对8080叠加74类候选，8094不动；队列窗口已交还，最新PID38436，必须保留queue-readonly-observation-v2.jar SHA25603819F0E…及algorithm/event。加载前7类活动计数均0。实际exec在CreateProcess前被自动审批拒绝，理由认为旧禁止重启仍有效、不接受后续批准；命令未执行、未停机、无load-state。本轮不换方式绕过或重试。待审批层认可该明确范围后，仍保持原参数、Flyway关闭及四资金开关显式false，不整包覆盖 |
| 数据库 | 057已按新增明确授权完成并验证；四列存在且成功登记，无新表/索引，不重跑048/049/052/053/054/056/057。应用退回旧代码时保留新增列和历史，不能删列或repair记录 |
| 官方资源配置 | 合并当前实际生效的两项免费资源与第三项official-autodl/-6000；原免费资产、别名、目录、环境和静态规格保留。Spring列表整体替换，不能只写index2覆盖现有列表。13.00用户价已获明确业务决定，不等于允许资金动作 |
| 持久身份配置 | 优先复用既有DEV vls商户key、AutoDL既有API身份及SSH私钥引用/指纹/主机公钥，不创建新商户或新key。当前可用性未确认，客户端缺失不能推断秘密不存在。若需持久保存，先批准仓库外`C:/Users/oort/AppData/Local/VLStream/dev-billing/`受限configtree或等效安全存储、仅运行账户可读、仅DEV使用及移除期限；不得写repo、普通参数、日志或前端。AutoDL账号级API权限未验证，不能谎称实例级最小权限；代码目标仍限定此Pro实例 |
| 真GPU与资金 | 预算、时限、停机/失败处置与账本额度在前置齐备后另作一次精确批准。此前4元仍是未批准草案；本轮不启用计费、不调用余额/开关机/资金，也不重放旧净零流水 |

本轮所有自身工具均已结束，未发出任何结果未知的SQL写、资金或供应商动作。数据库元数据读取阻塞已经解除；
057执行也已完成；剩余是已批准8080加载的自动审批阻塞、持久身份配置与真实消费等后续独立授权，非数据库连接拒绝或真实交易失败。
当前8080实际PID38436仍UP，queue v2和algorithm bean来源已再次只读确认，NewApiBillingClient未创建、计费候选未加载；
8094 PID67960仍UP且未变。本轮不重试拒绝、不使用旧PID54196/65276回退，不重启或发布VM。
拒绝后的真实状态证据在`billing-load-denied-state-20261029.json`；没有结果未知的SQL/资金/供应商/服务重启动作。

随后父级通过原始用户消息读取工具核验`Sentinel_70235ebcb2908191bba9196d60e6f306`的用户作者、
11:13:48时间与精确回复关系。针对该新来源证据，原`exec_command(require_escalated)`复审一次已获放行。
加载脚本先补齐8094启动时间/监听归属/健康核对、回退完整复验与四资金开关关闭；26项无网络mock检查通过。
本次执行在停机前的共享工件哈希门禁退出：2980项共享基线中52项变化或缺失，集中在11:48–11:49的target重建，
包括application.yml、计费类及事件/模型下发类；两个旧文件缺失。未进入活动数据库查询、未创建load-state、未停机或加载，
8080/PID38436与8094/PID67960仍UP。当前阻塞已从审批变为并行构建工件协调，不放宽哈希门禁、不覆盖其他任务产物，
057不重跑。具体清单见`runtime-8080-billing-20261029/baseline-drift.json`；资金证据区分关闭开关/无客户端与独立账本审计。
后续只读归因确认共享target的Maven编译清单在11:48:51写入、testCompile在11:49写入；当前未发现运行中的Java/Maven构建。
本任务隔离回归日志结束于10:11、候选包生成于10:26，构建脚本工作目录在隔离目录，未找到可将这次共享编译归属于本任务的证据，
也不能仅凭时间戳断言是其他用户或任务。已查旧构建只能找回40个类差异中的21个原SHA，另19个类及原application.yml未找到精确来源；
original-classpath.jar不含类字节。未创建不完整快照、未覆盖共享产物；需要确认11:48左右是否有本地Maven/IDE构建，之后再确定可加载基线。
