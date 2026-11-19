# 中转站计费契约：已答与待实测清单

## 当前产品与验收结论（2026-11-06）

算力租用 → 平台训练 → 停止 → 结算为本仓库正式常驻业务，单／多租户共用同一市场与计费接口。配置不再选择 `OFF`／`READ_ONLY`／`PAID`，也不再使用外部计费四个布尔开关或官方 AutoDL 专用控制开关。真实商户、目标实例和安全政策决定是否具备执行条件，缺少哪一项就明确提示哪一项；没有“尚未开放”的额外产品门禁。正常配置见 [计费接入说明](EXTERNAL_BILLING_STAGE1.md)。

常驻功能仍要求当前可信平台 `self.id`、本地本人和可信租户、市场权限、余额查询授权、明确接受服务端报价、预冻结及运行／停机预算；页面加载不自动查询余额、冻结或开机。缺身份不能付费，本地管理员或商户凭据不能替代个人付款身份。既有订单保留查询、本人停止及原引用恢复，原 UNKNOWN／冲突、归档和资金审计不因准入配置变化而丢失。本地停机预算是计划和预留，须保留有效轮询余量、正数运行／额度上限及固定绝对截止；可选 `verified-provider-stop-seconds` 仅记录已有观测，不是首次使用必填项。填写时仍须为正且加轮询余量不超出预算。无观测不伪造 SLA；预算或截止不是供应商保证和费用硬封顶，真实物理停机与资金状态仍须独立确认。

**完整真实产品闭环仍未通过外部验收。** 本地加载及隔离单元／页面回归只证明对应代码行为。目标环境仍需核验可信动态连接、真实平台训练与产物归档、物理停机和最终供应商账单、原冻结尾账与释放、异常及并发恢复；不能由功能常驻推导这些验收已完成，也不能为通过校验猜测停机观测证据。

### 已核实的原单与历史证据

以下为原实测的持久结论，不是新的执行步骤或重新消费许可：

| 原单／阶段 | 已核实结论与边界 |
| --- | --- |
| 2026-10-28 隔离 DEV 钱包最小循环 | hold／topup／原键 capture／refund／release 已实测；模型与 GPU 混合并发、完整重放字段和 Java＋GPU 产品闭环仍不能由脚本结果代替 |
| run `24263603-fa7f-481c-b979-01db16bb62e7`、hold4 | 页面确认后发生冻结、API 开关机；训练未启动，三笔查单流水 ID 矛盾触发保护。原扣费及原退款各 54,166 quota，第四尾段未发，订单为失败回滚 `CANCELLED`，不能当作正常 `COMPLETED` |
| hold4 后续修复 | 两类 capture 查询已统一对应 ledger 3／4／5，退款关联 ledger 6／7／8。维护者修复实际冻结 18,055 后，仅一次原单释放，hold4 remaining0、钱包 held0；旧差异已修复核验，不再作为当前开发阻塞或再次释放依据 |
| run `81688526-2366-443c-9808-84dd4f896d42`、hold5 | 开机为 `UNKNOWN`，未重发、未训练、未 capture；厂商后续为 shutdown。原 hold5 过期仍有真实冻结，核对原归属、原请求和钱包后一次 RELEASE 成功，钱包 held0。之后的 BadRequest 停机回执不能倒推历史开机响应或停止请求受理成功 |
| 2026-11-04 交接 | hold5／hold6 已由用户正常管理员入口核销，原失败／冲突审计保留，不再退款、释放或重开旧单。历史钱包 4,999,971,278,890／held0／version25 仅为当时基线，本轮未重新核对 |
| 供应商成本 | 2026-10-30 API 运行费实结 ¥0.07、此前手动运行费 ¥0.41，与磁盘保留费独立；钱包净零不抵销供应商费用。后续失败试跑费用仍缺独立最终账单证据 |

原 UNKNOWN 恢复修复保留：同订单可在原开机未知时请求安全停止，不重发开机；自然到期仅在原身份、金额、计数、版本与到期一致时不视作资金冲突；资金异常不挡必要停止，但继续阻止无证据扣款或释放。`NO_STOCK` 只接受经过验证的供应商明确契约，不能把任意 BadRequest、文字相似错误或网络未知解释为占用。未发送取消、明确拒绝和未知结果均保留原订单、用量和资金证据，直到原释放确认才解除对应占用。

证据索引：

- `codex/runtime-20261030-manual-current/`：`original-remote-evidence-complete.json`、`original-journal-proof-recovery.json`、`original-refund-cleanup-final.json`、`actual-refund-frozen-release-intent.json`、`actual-refund-frozen-release-receipt.json`。
- `codex/runtime-20261030-final-cleanup/`：`verification.json`、`physical-state.json`、`supplier-final-cost.json`。
- `codex/billing-contract-20261023/`：`new-trial-safe-cleanup-summary-20261102.json`、`new-trial-original-release-result-20261102.json`、`runtime-recovery-fix-20261102/final-verification.json`；隔离资金实测位于其 `isolated-test-runs/vls-lab-48ea817b-f4d3-42a7-a1be-190283d5f895.json`。
- `codex/runtime-paid-trial-20261104/checkpoint.json`：原单核销、mainflow 实现及旧模式加载交接。旧 `Start-ApprovedProTrialManual.ps1`、旧进程号、旧模式加载与单次许可仅是历史证据，不是当前产品部署说明，不能复用已耗用原单或保护许可。

下文保留契约和按日期的实测细节；历史“当前”“资金关闭”或开关表述不覆盖本节。契约中“已答”表示已提供实现依据，不表示目标服务已通过全部真实钱包验收。

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
当前用户价取官方服务端 YAML／环境配置的 **13.00 CNY／整实例小时**，每次任务保存报价快照；
供应商报价或变价不改变已接受的用户价格，官方供给不走算力管理数据库 CRUD。

## 响应差异与当前 Java 恢复实现（2026-10-30）

早期附件没有给出全部查询响应，但后续真实脱敏回执已提供可用的最小形状；
当前已按这些证据实现以下恢复，不能继续把“文档缺完整响应”等同于“VLS 尚无恢复实现”。
未出现过、缺必要关联字段或相互矛盾的响应仍不能当作成功。

1. **hold-operation：原操作证据、原请求重放与 fresh hold 联合核验。** 已保存的 RELEASE
   查询只有 `idempotency_key/status/hold_id/user_id/created_at`，不包含实际释放额。
   Java 先按原类型／业务号查询，检查其自身 key、hold、用户关联和 fresh 原 hold 明细；
   仅在最小查询明确为原操作 `SUCCEEDED`，且 `HOLDS_V3`、原请求 path/body/hash/归属
   和到期／风险门禁都成立时，允许 TOPUP／RELEASE 以原 key、原业务号、原 body 每轮
   重放至多一次，取得原操作 POST 回执，再用新发起的 hold 查询核对。HOLD 创建未知时
   只查原业务号恢复，未定义的 NOT_FOUND 形状不能作为重新创建的依据。
   TOPUP／RELEASE 原始回执与新明细通过后标为 `OPERATION_CONFIRMED`；订单不能仅因
   remaining=0 就跳过未知的 release 子单。重放可产生多个 HTTP 尝试，资金副作用依赖上游
   同键同体幂等；没有改键、延长到期或从聚合余额推算某个操作成功。明确拒绝、冲突、
   处理中和证据不足仍阻断；用户撤权继续拒绝新增冻结、补冻和 capture，原单 release 的
   受限善后例外保留，失效商户认证不绕过。完整查询 schema 未补齐不再阻塞这一受限路径。
2. **capture：by-reference 与原 by-key 交叉证明 hold 关联。** 真实
   `/charges/by-reference` 成功回执没有 `hold_id`，但原 `/operations/by-key` 的 CHARGE
   回执包含该字段。Java 先核对 reference 回执的用户、业务号、用量、计量、报价、兑换版本
   和金额，再以原 key 的成功操作核对商户、用户、用量、hold，并要求两者的
   `charge_id/ledger_entry_id` 相同。这里是查单回执中的流水 ID 关联，不声称该分支另发了
   ledger 查询。原 key 未决时不再次 POST；矛盾保持阻断。`CONCURRENT_BALANCE` 按
   v2.2 白名单先查后以原键原体恢复，解决提交未知后仅由 `SUBMISSION_UNCERTAIN`
   造成的尾段阻塞时也不清除其他风险或撤权状态。
3. **退款：原 key 与有界原单流水联合证据。** 真实 REFUND by-key 只给出
   `idempotency_key/operation_type/status/refund_id`，不能补造缺失的 POST operation ID、
   可用余额或版本。显式只读 `reconcile` 通过原 key、原 CAPTURE 流水及关联的
   CAPTURE_REFUND 流水核对退款 ID、原流水 ID、业务号、付款人、金额及余额算式。
   查询限原扣费／退款创建时间附近的最多两个 24 小时窗口、总计最多 10 页，每页最多
   100 条；必须完整分页，缺页、游标循环、超额、窗口外迟到记录或矛盾不能确认。
   成功保存独立的 `LEDGER_RECONCILED` 证据，不自动扫描全商户历史，不重发未知退款，
   也不把正常消费自动退款。POST 与流水证据任一先到，关联一致时保留先存证据及观察
   时间，实际矛盾进入粘滞冲突。之后仍须重新发起原 hold 查询，并用独立释放业务号
   处理退款返回的剩余冻结额度；旧 final-release 不改写为新的释放。

以上 Java 路径已通过隔离回归并加载本地资金关闭的候选；尚未用该 Java 客户端在真实
NewAPI 上验证上述故障注入和并发恢复。原真实脚本回执位于
`codex/billing-contract-20261023/isolated-test-runs/vls-lab-48ea817b-f4d3-42a7-a1be-190283d5f895.json`。

## 待实测，一次性交接

| 验证项 | 需要的证据 |
| --- | --- |
| 共享钱包原子性 | 目标数据库中模型预扣/流式差额、GPU capture、缓存/批量及管理减额混合并发；quota、held、available 非负且流水、幂等和业务单同事务，注入故障整体回滚 |
| hold 并发与重放 | 同 key/业务号同参不重复，异用户/金额/hold 冲突；capture/release/topup 并发不超剩余；不足补冻后原 key/body 成功，无“先释放后扣款”窗口 |
| 到期与授权 | topup 保持原 expires_at；到期 capture 拒绝而 release 可执行；确认无自动解冻假设；撤权与 capture/release/refund 并发先后顺序及商户失效拒绝 |
| 恢复与退款 | 提交后丢响应、5xx/503、进程重启，恢复原快照；部分退款及上限、回 held 的最终剩余查询与新 release、旧 release 重放不释放新退款 |
| 权威查询 | self.id/scopes、hold 详情及操作查单完整响应、capture hold 关联、历史 conversion_version 的受理/拒绝，ledger 分页和不可变流水逐条对账 |
| VLS 安全生命周期 | 真实供应商实际停机证据、停止前尾账、未知区间和未归档任务保护；请求停机或模拟 STOPPED 不代替真实完成 |

**2026-10-23 历史准备状态（已由后续实测补充）：** 当时未执行真实资金联调。
当时附件仅给出占位配置，未给可用测试 host、指定 merchant/user、
现成受限身份或无现金账本与金额/次数范围的证据。后续由父任务统一交接，通过批准的安全配置渠道提供，
不在本文或聊天粘贴 key、token、密码，不开户、不采购 GPU，不直接联系第三方。

本地 fake HTTP/H2 与 Maven 通过始终只证明 VLS 代码的隔离行为，不能证明模型与 GPU 的
真实共享钱包并发验收完成。当时未部署、重启或迁移；本地历史候选加载证据不能代替当前源码加载与目标验收；本轮没有新的外部动作。

## 2026-10-28 隔离 DEV 实测：补冻后原 capture 仍被拒绝

本节补充后续真实测试结果；上面的“本次没有执行”是 2026-10-23 历史状态。
当前只使用用户明确确认的隔离虚拟账本，不涉及生产余额或真实费用。
目标为 `https://workup-dev.myoumuamua.com:6433/bus/apaas-newapi`，调用方 `vls`，
专用测试账号 `hjk1`，已核实权威 NewAPI `self.data.id=13`。没有开 GPU，
没有修改角色、用户授权、trusted 开关或部署。密钥只从本机隐藏输入临时进入进程，本文不包含密钥。

### 实际顺序和证据边界

所有路径相对上述目标。商户认证头名称为 `Authorization`，认证值完全不记录；
写请求另带 `Idempotency-Key`，以下列出的 key 都是非秘密业务幂等标识。
本轮前缀为 `vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05`，共 23 次请求后停止。

| 顺序 | 请求/结果 |
| --- | --- |
| 1 | `GET /api/v1/external-billing/config` 和 `/users/13/balance` 被接受；起始 `balance=4999971278890, held=0, available=4999971278890, version=2`；兑换版本 `v1-1792399815` |
| 2 | `POST /api/v1/external-billing/holds`，`merchant_hold_id=<前缀>-hold`、`hold_quota="3"`、`user_id=13`、`expires_at="1793156620976"`；返回 hold `2`；按业务号、ID 和余额核对，再同键同体重放，冻结仍为 3 |
| 3 | `POST /api/v1/external-billing/charges`，`Idempotency-Key: <前缀>-charge-key`，`merchant_charge_id=<前缀>-charge`、`hold_id=2`、`charge_quota="4"`。实际响应 **HTTP 422，业务码 `INSUFFICIENT_AVAILABLE_QUOTA`** |
| 4 | `GET /api/v1/external-billing/operations/by-key/<前缀>-charge-key`：200、`REJECTED / INSUFFICIENT_HELD_QUOTA`；`GET /api/v1/external-billing/charges/by-reference/<前缀>-charge`：404、`CHARGE_NOT_FOUND`。fresh hold 和余额证明未扣费、remaining=3 |
| 5 | `POST /api/v1/external-billing/holds/2/topup`，`merchant_topup_id=<前缀>-topup`、`user_id=13`、`topup_quota="2"`，并以原 topup key/body 重放；fresh hold 确认 `hold=5, captured=0, released=0, remaining=5`、原到期不变；余额 `balance=4999971278890, held=5, available=4999971278885` |
| 6 | 对原 capture **原键、原业务号、原请求体逐字重试一次**，仍返回非 200，脚本停止 `POST_REJECTED`。这次 POST 的具体 HTTP、业务码和响应体未被当时版本保存，均为**未知**，不得补写为 422 或沿用首次错误 |
| 7 | 再查原 operation：200、`REJECTED / INSUFFICIENT_HELD_QUOTA`；再查原 charge：404、`CHARGE_NOT_FOUND`。未发送退款或释放；第二次 capture 之后没有再读 hold/余额，因此上面的 5 quota 是最后实测快照，不能称为停止后的再次核验或已自动解冻 |

原 capture 的不可变请求体如下，尝试次数为 2。原始字节和意图在脱敏 journal 中保留；
没有换 key、换单号、换 hold、移除 hold_id、改成普通扣款或增加请求金额。

```json
{"merchant_id":"vls","merchant_charge_id":"vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05-charge","user_id":13,"hold_id":2,"business_type":"GPU_COMPUTE","usage_session_id":"vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05","charge_quota":"4","metering_ref":"vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05-synthetic","price_snapshot_id":"vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05-price","conversion_version":"v1-1792399815"}
```

### 与契约的差异及服务端定位点

`external-billing-vls-integration-prompt(4).md` 第 115–124 行、256–263 行明确：
hold 不足应返回 `422 INSUFFICIENT_HELD_QUOTA`；对同一 hold 补足后，
用原幂等键、原业务号、原 body 可重试。该文件 Library ID 为
`libfile_601ef0df27988191b1d22850dc85c32c`。

实际有两处差异：首次响应错误码与 operation 查单错误码不同；
fresh remaining=5 后原 capture4 仍被拒绝。可确认契约不符，不能据此确定服务端具体代码根因。
请部署方按上述 merchant/user/hold/charge/key 核对：

- 实际部署修订和对应请求日志；`hold_id=2` 是否被正确解析并进入 capture 分支。
- REJECTED 可重试白名单是否同时包含 `INSUFFICIENT_HELD_QUOTA` 和 `INSUFFICIENT_AVAILABLE_QUOTA`，
  原键/原请求指纹是否进入重新执行，或直接返回先前拒绝。
- capture 事务是否读取 topup 已提交的当前 hold `remaining=5`，以及失败分支的原始机器码是否被控制器错误映射。
- 商户绑定和操作查单是否都落到 `merchant_id=vls,user_id=13,hold_id=2` 的相同记录。

不通过换键/换业务号绕过；本轮没有证据支持继续盲目重试该 capture。
VLS 侧已补全今后每次 POST 的脱敏 HTTP/机器码和停止后 fresh hold/余额记录；
对应原键持续拒绝的离线回归已覆盖。

### 原单清理状态与证据

本轮成功 capture、退款及完整流水闭环尚未完成；旧 hold1 曾净零不能代表本 hold2。
**2026-10-28 03:10:07 UTC，hold2 已实际释放并核对净零**，不是推断到期自动解冻：

1. 原 operation 再查为 200 / `REJECTED / INSUFFICIENT_HELD_QUOTA`，原 charge 再查为
   404 / `CHARGE_NOT_FOUND`；fresh hold2 为 `hold=5,captured=0,released=0,remaining=5`；
   fresh 余额 `Q=4999971278890,H=5,A=4999971278885,version=4`。确认原单未扣后才释放。
2. 只发送一次 `POST /api/v1/external-billing/holds/2/release`，
   `Idempotency-Key: <前缀>-original-cleanup-key`，请求体为
   `{"merchant_id":"vls","merchant_release_id":"<前缀>-original-cleanup","user_id":13,"release_quota":"5"}`。
   HTTP 200，响应中的 `hold_id=2,released_quota=5` 经精确校验；未发送 capture/refund 或新建 hold。
3. `GET /api/v1/external-billing/hold-operations/RELEASE/<前缀>-original-cleanup`
   为 200 / `SUCCEEDED`。该查询完整操作回执字段尚未单独核全，释放金额证据来自上一步 POST 回执，
   不从单独的 HTTP 200 推断金额。
4. 再查原 operation、charge、hold 和余额：原扣费仍为 REJECTED，charge 仍 404；
   `hold=5,captured=0,released=5,remaining=0`；最终
   `Q=4999971278890,H=0,A=4999971278890,version=5`，与起始 Q/H/A 一致，净变动 0。

此轮累计 35 请求（原测试 23、清理 12），清理 release POST 只有一次。
journal 终态 `ORIGINAL_TEST_HOLD_CLEANED_NET_ZERO_CAPTURE_NOT_COMPLETED`；
没有把原键重试拒绝改写为通过，也没有新开测试单。
专用清理的 14 项离线回归覆盖已知拒绝、未知/PROCESSING、存在扣费单、错误归属、释放失败、
释放丢响应、原请求预算以及不重复 POST。隐藏输入进程已正常退出 0，没有保存密钥。

实际输入链：测试窗口结束后，原进程中的凭据已不可用；随后由本任务启动
`VLS DEV Billing - Original hold cleanup` 可见窗口，用户再次在本机隐藏输入既有密钥，
脚本自动读取原 journal 完成清理，用户无需运行命令或复制输出。
不是从聊天历史、日志或文件读取/转交密钥。输入用 SecureString，传给子进程时仅走匿名 stdin；
BSTR 已按脚本清零释放，SecureString 已释放，子进程凭据引用已清理且相关进程均退出。
凭据没有长期保存，当前无法继续复用；这不宣称做过内存取证或能保证运行时所有历史内存副本被物理擦除。

本说明已做敏感信息检查：没有真实 API key、Bearer 值、Cookie、浏览器会话标识或密码。
`Idempotency-Key`、`merchant_*_id`、`usage_session_id` 是为查单保留的非秘密业务引用，
其中 usage_session_id 表示本次合成计量周期，不是登录 session。

证据位于 `codex/billing-contract-20261023/`：
`isolated-test-runs/vls-lab-e7b8f843-b1ae-4291-a307-0a33cec2cd05.json`、
`live-retest-topup-capture-review-20261028.json`。journal 只包含业务意图、余额和脱敏状态，
不含认证头、API key 或密码。资金清理完成与完整 capture/refund 验收必须分别报告。

## 2026-10-28 后续附件复核：同事声明 v2.2 已修复

已完整读取并与前版逐行比较两份附件：

- `external-billing-implementation-plan(4).md`：文内 **v2.2，2026-10-28**，共 797 行；
  Library ID `libfile_e6845c9a2374819180f982d586c7c461`。
- `external-billing-vls-integration-prompt(5).md`：共 678 行，顶端更新标题仍为 v2.1，
  第 122 行新增“2026-10-28 已验证通过”声明；Library ID `libfile_9068b0927f98819195b3d37527bf2310`。

方案第 7–9、749–750 行明确声称两项修复：`AtomicChargeUserQuota` 原可重试拒绝白名单漏掉
`INSUFFICIENT_HELD_QUOTA`，导致同 hold topup 后原 key/body 不重新执行；
`mapModelError` 统一映射为 available 错误，现拆分 held/available 两类错误，
并声称增加 `TestCaptureRetryAfterHoldTopup` 回归。这能解释前述两处实测差异，
但属于部署方提供的诊断/修复声明，未附对应源码修订、构建摘要、发布记录或原 DEV 实例请求日志。
不能把“已验证通过”当作本任务对目标站点完成的新验收；此前第二 POST 的实际 HTTP 仍未知。

接入提示词第 120–125、294–295、344 行明确三项重试码：
`INSUFFICIENT_AVAILABLE_QUOTA`（充值后）、`INSUFFICIENT_HELD_QUOTA`（同 hold topup 后）、
`CONCURRENT_BALANCE`（原键原体）。其他拒绝不可重试；原 key、原业务号、原 body 不变，禁止换单。
认证、权威 self.id、trusted/REVOKED 规则、capture/退款/释放语义以及接口前缀未改变；
示例 host/user/百万额度均未变成新的环境或金额授权。本任务仍只限原 DEV、vls、hjk1/id13，
最多冻结 5 / 实扣 4 / 退款 4 quota、单次运行最多 60 请求，无 GPU 或权限变更。
用户此前明确确认的隔离虚拟账本及限额仍保留，无需因附件的旧“管理员确认”句子重复询问。

残留文档问题不冒充新业务要求：方案第 749 行“原只含前两者”与开头对旧白名单的描述不一致；
提示词恢复伪代码仍未列出 CONCURRENT_BALANCE；后面的无条件手动授权句子、scope JSON/逗号格式、
方案第 11 节只按总 held 管理的旧文字仍保留。应按具体 trusted/逐 hold 契约解释，
不据此新增用户配置、恢复 REVOKED 或扩大权限。

本轮只更新本地测试准备：44 项离线回归通过，补齐精确 held 错误与原键 capture 后重放验证，
保留原键仍拒绝的负向回归；资金虽净零但 wire 错误码不符时标为
`CYCLE_RECONCILED_WITH_CONTRACT_DIFFERENCES`，不能冒称完整契约通过。
hold2 清理终态须凭原拒绝、charge404、单次释放回执和最终 Q/H/A 证据核验，不能仅凭状态标签放行。
未请求 NewAPI，未启动新窗口，旧 hold1/2 仍仅代表已净零，成功 capture/退款仍未实测通过。

后续可沿原最小流程准备复测；实际执行前需确认原 DEV 实例已包含 v2.2 两处修复，
临时凭据输入由父任务协调。当前没有保留可复用密钥，不重试已释放/过期的 hold1/2 或旧 capture，
不以附件内容作为新增资金或权限动作的授权。

## 2026-10-28 10:00 UTC 实际资金闭环与尚未完成项

后续在原隔离 DEV、vls/hjk1-id13 完成 prefix
`vls-lab-48ea817b-f4d3-42a7-a1be-190283d5f895`：hold3，charge1，refund1。
冻结3及重放、补冻2及重放均保持正确额度和原到期；首次 capture4 在 hold3 额度不足时返回
422/INSUFFICIENT_HELD_QUOTA，原 operation REJECTED、charge404且无扣费；
补至5后同键同体 capture4 为200/SUCCEEDED，账面和held同减4、available不变。
同键再次重放200/SUCCEEDED，之后 fresh余额仍只扣4，没有重复扣费。

脚本的重放回执正整数校验发生 `INTEGER_RANGE`，没有保存具体失败字段。
旧函数只在 `charged_quota/charge_id/ledger_entry_id/operation_id` 的正整数值为0或超过int64时产生这个码；
不能把它等同于余额的32位溢出、缺失 operation_id 或确认的 NewAPI 代码错误。
后续真实原 key/charge 查询均确认 charge1、原流水1、user13、charged4；operation查询没有operation_id，
但这些 GET 不能还原那次 POST 重放的确切字段。未为定位它再发 capture。

退款清理先查原操作/charge/hold/balance，确认 capture4/refund0、hold remaining1。
第一次 `POST /charges/1/refunds` 因临时脚本 ledger ID 数值编码返回400/INVALID_REQUEST；
原退款操作404且fresh refunded仍0。按照最新契约的字符串ID编码修正为 `"original_ledger_entry_id":"1"`，
保留旧拒绝body、原退款号/key/金额/charge/ledger语义；在同一已输入临时会话中成功继续：

| 证据 | 实际结果 |
| --- | --- |
| 退款 POST | `/api/v1/external-billing/charges/1/refunds` HTTP200；operation8、refund1、ledger2、refunded_quota="4" |
| 退款 by-key | 原 `<前缀>-cleanup-refund4-key` HTTP200、REFUND/SUCCEEDED/refund1 |
| 退款后 fresh hold/余额 | captured0、remaining5、Q4999971278890、H5、A4999971278885，证明退款先回held，尚不能把它视为可用余额 |
| release POST/查单 | 原 `<前缀>-cleanup-release` 只发一次，HTTP200，实际released5；原RELEASE查询200/SUCCEEDED |
| 最终 fresh状态 | hold5/captured0/released5/remaining0；Q=A4999971278890/H0/version10，与起始Q/H/A相同 |
| 原流水1 | CAPTURE，delta=-4，before4999971278890→after4999971278886，原charge业务号 |
| 退款流水2 | CAPTURE_REFUND，delta=+4，before4999971278886→after4999971278890，related_entry_id=1，原cleanup-refund4业务号 |
| 请求及终态 | 共59次请求，`ORIGINAL_CAPTURE_REFUNDED_RELEASED_RECONCILED_WITH_REPLAY_RECEIPT_ISSUE`，资金净变动0；临时进程正常清理退出 |

首个退款的400是已确认未形成操作的DTO拒绝；只更正JSON ID编码，未换 key、退款号或额度，
未盲目重发未知结果。本地 Java RefundContracts 原来即生成字符串；此次增加HTTP边界拒numeric输入，
临时主脚本也修正编码并补字段级回执诊断，不静默改写历史资金快照。

已通过的真实范围是本次核心API最小资金循环及原单查单恢复、hold/topup重放和净零对账。
尚未验收：capture重放的全部回执字段、退款/释放重复回执、分笔1+3退款、零remaining退款分支、
模型与GPU混合并发、撤权/失效身份竞态，以及VLS产品内注册充值→余额→开机训练→停止归档结算。
在本节 2026-10-28 实测时，后者仍有付费准入硬性拒绝、部署与开关未变。2026-10-30
Java 产品候选和恢复能力已实现并加载本地，当前仍因运行配置和资金／准入开关关闭而禁用，
真实全链待有限预算和运行身份准备；不能把历史 API 脚本或今天的隔离测试叫作产品全部完成。

最终证据：`codex/billing-contract-20261023/v22-live-cycle-final-20261028.json` 与同前缀 journal。
2026-10-28 该轮结束后没有再次请求 NewAPI 或重新打开密钥输入进程；2026-10-30 的新增
工作为本地代码、隔离回归和资金关闭的 8080 候选加载，未重放上述已净零原单。
