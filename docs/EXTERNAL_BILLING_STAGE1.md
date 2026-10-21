# VLS 计费接入：本地执行链路与验证

2026-10-21续开发更新。已补完整适用Maven构建、周期执行器、正式身份绑定、安全取消/停机衔接及可恢复对账。**收费继续关闭，未部署、未重启服务、未运行共享数据库迁移**。

## 已实现

代码位于 `ruoyi-vlstream/src/main/java/com/ruoyi/vlstream/test/vlstream/compute/billing/`：

| 文件 | 实际行为 |
| --- | --- |
| ExactQuota | BigInteger 精确有理数；有限十进制报价×实际毫秒×N/D，整数向下结算，余数持久保留；校验int64边界 |
| BillingPayer | 从后端已验证的本地/SSO actor及权威 `/api/user/self` 响应取NewAPI user.id；无前端payer入口，不保存token |
| BillingSnapshot | 固化既有 ComputeBillingRate 和权威兑换版本/N/D；成本来源、币种、整实例口径、锁价证据沿用原校验，不新增定价授权 |
| ExternalBillingGateway | 类型化查单/拒绝/处理中/未知/授权失败；原子capture独立能力边界 |
| NewApiBillingClient | 适配现有config/self/balance/按key与业务号查单；金额精确字符串，拒绝一般代理404、错用户/余额算术；不跟随重定向、限制响应大小、错误不泄漏token/响应内容 |
| ExternalBillingProperties | `vlstream.compute.external-billing.enabled=false`默认关闭，无地址/密钥默认值；未配置真实身份 |
| ComputeBillingSettlementService | 使用现有可信 usage；持久会话、连续区间/序号、业务号、独立固定key、完整请求JSON与哈希；个人账户余数/游标/指令同事务；租约/CAS领取；所有外部I/O在本地事务外；查询优先的恢复状态机 |
| ComputeBillingCoordinator | 默认关闭的周期执行器，按运行证据封区间；持久退避、长期未知保护、确认停止后封尾账，轮询顺序避免完成会话占满批次 |
| ComputeBillingReconciliationService | 固定窗口、持久游标及领取租约；原流水ID去重、不可变哈希、本地匹配；失败不推进游标，差异/退款留待核验，不自动改资金 |
| BillingLifecycleGateway / compute.ComputeBillingLifecycleService | 验证物理实例、供应商控制身份及持久占用代次；给本人当前任务设置既有cancel_requested，等待终态与归档后写入已有ComputePowerWorker启停outbox；不直接杀进程/删数据/释放占用 |

新增 Flyway `V1_2_0_048__compute_billing_slices.sql`：只加 usage.billing_mode、会话/区间/个人余数表，不种用户、授权、余额、凭据或GPU资源。旧整周期意图及按租户余数保留，不自动合并历史尾差。旧 `ComputeBillingLedgerService.prepareSettlement` 增加 SLICES 模式守卫；新绑定同样拒绝已有旧意图，防两套路径重复结算同一次用量。

现已接默认关闭的周期协调器及正式身份绑定入口，仍没有购买或扣款HTTP入口。admin新增ComputeBillingBindingController：GET `/vlsCompute/billing/identity` 复用既有ComputePlatformAuthorizationService验证当前本地actor/tenant与独立SSO，再用同一请求已验证proof读取self.id；POST `/vlsCompute/billing/sessions/{usageId}/bind` 只接受已有usage路径，不接客户端付款人、金额或报价，服务端读取持久费率/权威config并校验当前整实例占用。不会创建中转站授权或开机，外部适配器未启用时拒绝。

续增 `V1_2_0_049__compute_billing_coordination.sql` 保存控制快照、安全状态、轮询/重试时间、对账游标及已见流水。048/049均只在临时H2验证；下次启动其他测试实例时也不得自动应用到共享数据库。

## 最终接口语义

- scope 对接新版附件的 `QUERY_BALANCE` / `CHARGE_GPU`；用户授权路径为 `/api/user/external-billing/authorizations`。它们是第三方声明，不代表本次已实测上线。
- 充值重试固定：**原 Idempotency-Key + 原 merchant_charge_id + 原完整请求体**；不换key、不换业务号。此前默认换操作键的设计已撤销。
- 超时/5xx（包括503）全部先按业务号和key查单；PROCESSING等待；成功详情逐字段核对；双明确查无才可原样再发。余额不足明确拒绝才进入WAITING_FUNDS，余额恢复也仍使用原键原体。
- 授权失败、未知结果、缺capture等会记录风险和持久 `stop_requested`，阻止付费准入并触发现有取消/停机流程。安全执行另有默认false的safetyStopEnabled。运行计量不因拒绝结束；取消请求、停机排队和STOPPING都不能当成确认停机。
- 仅供应商STOPPED观察才能确认终点。缺精确供应商事件时保守计至最后证实运行的样本，另存稍后停止观察时间和不确定区间，不把轮询时刻冒充实际切换秒点；已有可信PROVIDER_EVENT终点保留。控制身份/占用代次变化或存在其他用户活动任务则拒绝取消/停机；ARCHIVING等待完成，不转交实例或自动重启。
- 计量必须有运行观察边界或已确认终点，不能只按调度器当前时间收费。尾账是最后短区间，不重算先前UNKNOWN区间。`audit`提供本地待结算/已结算/尾账明细，不等于外部ledger或供应商对账已完成。
- 报价沿用项目已确定的 ComputeBillingRate 及其原有倍率校验；附件中的成本×2不构成新的改价授权，没有修改现有市场售价、成本适配器或记录。

## 安全门禁与未完成部分

真实适配器 `supportsAtomicCapture()` 固定false，`capture()`直接拒绝，无release→charge替代。`requirePaidAdmission`即使面对成功fake也继续拒绝新付费任务：完整资金保护尚不可验证。既有付费开机/训练门禁保持。

正式绑定入口、周期协调、持久退避、现有取消/停机桥接和外部ledger可恢复导入已实现。仍依赖外部补齐的部分是可核验授权状态、稳定hold/原子capture/释放及受限纠错退款；精确供应商事件与费用核对需要对应真实能力。完整资金保护仍未验收，缺口统一见 [中转站待补清单](NEWAPI_BILLING_BLOCKERS.md)。

## 本次验证

续开发在独立源码副本完成完整适用Maven reactor（12模块，含全部测试源码编译及admin打包），**90项测试全部通过，0失败/错误/跳过**：29结算/精度、11只读HTTP契约、9周期/对账、9安全生命周期（含1条实际协调器与实际桥接服务连通的隔离链路）、4正式身份绑定，以及28项既有计费策略/SSO/认证守卫。新增H2依赖仅test scope，每项为随机临时内存库；HTTP仅本机假服务。

覆盖固定区间、余数事务、并发领取、回滚/重启/丢响应、原键充值、未知/冲突、SSO替换拒绝、控制身份及占用代次变化、其他用户任务和ARCHIVING保护。连通链路使用实际VLS绑定、协调、取消/停机桥接代码和临时H2：运行计量→授权失败→请求本人任务取消→模拟任务终态→已有启停outbox→mock供应商STOPPED→尾账→可恢复对账。供应商及任务终态为模拟，无真实GPU操作；不代表目标MySQL、真实共享钱包、供应商成本或实际停机耗时验收。

完整证据在 `C:/Users/oort/Documents/Codex/2026-10-20/task-4/billing-stage/build/maven-stage2-e2e-final.log`、`stage2-summary.json`；JUnit XML位于独立副本billing-fullbuild内两个模块的target/surefire-reports。完整构建未覆盖共享target。可在独立副本复现：`mvn -o -ntp -Pdev -pl ruoyi-admin -am '-Dtest=ComputeBillingSettlementTest,NewApiBillingClientTest,ComputeBillingCoordinatorTest,ComputeBillingLifecycleTest,ComputeBillingBindingTest,ComputeBillingPolicyTest,ComputePlatformAuthorizationServiceTest,ComputePlatformAuthorizationFilterChainTest' '-Dsurefire.failIfNoSpecifiedTests=false' package`。

此前纯Node契约模型按原键语义更新后45/45通过，与上述90项分开计数，不累加重复执行结果，不能当作真实共享余额并发安全证据。

配置只做存在性检查：应用资源application*.yml未见external-billing段，当前进程未见非空EXTERNAL_BILLING环境变量。未扫描用户会话/私有凭据；这不证明其他服务实例没有配置。新版附件只有身份占位符，无可直接使用的测试商户身份。

真实认证及资金测试仍待明确中转站地址、现成受限商户/用户身份、虚拟隔离账本证据和限额范围。本次不使用聊天明文密码、不创建真实授权或密钥、不扣款/冻结/退款/充值、不调用收费模型或GPU，不联系中转站。
