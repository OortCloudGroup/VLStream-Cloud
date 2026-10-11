package com.ruoyi.vlstream.test.vlstream.controller;

import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.service.RemoteTrainingService;
import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/vlsCompute")
@RequiredArgsConstructor
public class VlsComputeController {
    private final ComputeNodeService nodes;
    private final CloudTrainingService training;
    private final ComputeMarketService market;
    private final MyTrainingTasksService myTasks;
    private final ComputeProviderSettingsService providerSettings;
    private final ComputeProviderStateService providerState;
    private final ComputeCostObservationService costObservation;
    private final ComputeHealthRefreshService healthRefresh;
    private final ComputeProviderInventoryService providerInventory;
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:configure")
    @GetMapping("/nodes/{id}/provider-inventory") public R<List<ComputeProviderInventoryService.Draft>> inventory(@PathVariable Long id){return R.data(providerInventory.list(id));}
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:configure")
    @PostMapping("/nodes/{id}/provider-inventory/sync") public R<String> syncInventory(@PathVariable Long id){return R.data(providerInventory.sync(id));}
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:configure")
    @PostMapping("/nodes/{id}/provider-inventory/{key}/refresh") public R<String> refreshInventory(@PathVariable Long id,@PathVariable String key){return R.data(providerInventory.refresh(id,key));}
    @GetMapping("/platform-authorization") public R<ComputePlatformAuthorization.Context> platformAuthorization() { return R.data(nodes.platformAuthorization()); }
    @cn.dev33.satoken.annotation.SaCheckLogin
    @PostMapping("/nodes/{id}/health-refresh") public R<String> refreshHealth(@PathVariable Long id) { return R.data(healthRefresh.refresh(id)); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:configure")
    @PostMapping("/nodes/{id}/cost-observation") public R<ComputeCostProvider.Observation> costObservation(@PathVariable Long id) { return R.data(costObservation.query(id)); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:configure")
    @PutMapping("/nodes/{id}/provider-settings") public R<String> providerSettings(@PathVariable Long id, @Valid @RequestBody ComputeRequests.ProviderSettings request) { providerSettings.save(id,request); return R.data("厂商配置已保存；未执行云端调用，总开关默认关闭"); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:power")
    @PostMapping("/nodes/{id}/provider-state") public R<String> providerState(@PathVariable Long id, @RequestParam(defaultValue="false") boolean recover) { return R.data(providerState.refresh(id,recover)); }
    @GetMapping("/my-trainings") public R<MyTrainingTasksService.Page> myTrainings(@RequestParam(defaultValue="1") int current,
        @RequestParam(defaultValue="20") int size, @RequestParam(required=false) String name, @RequestParam(required=false) String state,
        @RequestParam(required=false) String start, @RequestParam(required=false) String end) { return R.data(myTasks.page(current,size,name,state,start,end)); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:observe")
    @PutMapping("/nodes/{id}/observation") public R<String> observe(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Observation request) { market.observe(id, request); return R.data("已登记人工观察，请重新检查训练环境"); }
    @GetMapping("/market") public R<List<ComputeMarketService.Resource>> market() { return R.data(market.list(false)); }
    @GetMapping("/selection") public R<List<ComputeMarketService.Resource>> selection() { return R.data(market.list(true)); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:publish")
    @PutMapping("/nodes/{id}/public-identity") public R<String> publicIdentity(@PathVariable Long id,@Valid @RequestBody ComputeRequests.PublicIdentity request) { market.publicIdentity(id,request); return R.data("公共实例身份已登记，历史工作区无清理证据将隔离"); }
    @cn.dev33.satoken.annotation.SaCheckLogin
    @PostMapping("/nodes/{id}/claim") public R<String> claim(@PathVariable Long id) { return R.data(market.claim(id)); }
    @cn.dev33.satoken.annotation.SaCheckLogin
    @PostMapping("/nodes/{id}/release-claim") public R<String> releaseClaim(@PathVariable Long id) { return R.data(market.release(id)); }
    @cn.dev33.satoken.annotation.SaCheckLogin
    @PostMapping("/public/nodes/{id}/power/{action}") public R<String> publicPower(@PathVariable Long id,@PathVariable String action) { if (!"PLATFORM".equals(nodes.get(id).getResourceScope())) throw new com.ruoyi.common.exception.ServiceException("此入口仅操作公共算力"); return R.data(market.power(id,action)); }
    @cn.dev33.satoken.annotation.SaCheckLogin
    @PostMapping("/public/nodes/{id}/provider-state") public R<String> publicProviderState(@PathVariable Long id,@RequestParam(defaultValue="false") boolean recover) { if (!"PLATFORM".equals(nodes.get(id).getResourceScope())) throw new com.ruoyi.common.exception.ServiceException("此入口仅查询公共算力"); return R.data(providerState.refresh(id,recover)); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:publish")
    @PutMapping("/nodes/{id}/offer") public R<String> publish(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Offer request) { market.publish(id, request); return R.data("算力供给已更新"); }
    @cn.dev33.satoken.annotation.SaCheckPermission("vls:compute:power")
    @PostMapping("/nodes/{id}/power/{action}") public R<String> power(@PathVariable Long id, @PathVariable String action) { if ("start".equals(action)) nodes.authorizePaidOperation(id); return R.data(market.power(id, action)); }
    @GetMapping("/nodes") public R<List<ComputeNode>> list() { return R.data(nodes.list()); }
    @PostMapping("/nodes") public R<ComputeNode> create(@Valid @RequestBody ComputeRequests.Node request) { return R.data(nodes.save(null, request)); }
    @PutMapping("/nodes/{id}") public R<ComputeNode> update(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Node request) { return R.data(nodes.save(id, request)); }
    @PostMapping("/nodes/{id}/probe") public R<ComputeNode> probe(@PathVariable Long id) { return R.data(nodes.probe(id)); }
    @DeleteMapping("/nodes/{id}") public R<String> delete(@PathVariable Long id) { nodes.delete(id); return R.data("实例接入记录已移除"); }
    @PostMapping("/trainings/{id}/start") public R<RemoteTrainingService.StartResult> start(@PathVariable Long id, @Valid @RequestBody ComputeRequests.Start request) { nodes.authorizePaidOperation(request.getNodeId()); return R.data(training.start(id, request)); }
}
