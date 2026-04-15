package com.vlstream.controller;

import com.vlstream.common.controller.BaseController;
import com.vlstream.common.Result;
import com.vlstream.common.page.TableDataInfo;
import com.vlstream.entity.RemoteServer;
import com.vlstream.service.RemoteServerService;
import com.vlstream.service.SSHService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 远程服务器管理Controller
 */
@Api(tags = "远程服务器管理")
@RestController
@RequestMapping("/api/remote-servers")
public class RemoteServerController extends BaseController {
    
    @Autowired
    private RemoteServerService remoteServerService;
    
    @Autowired
    private SSHService sshService;
    
    /**
     * 查询远程服务器配置列表
     */
    @GetMapping("/list")
    public TableDataInfo list(RemoteServer remoteServer) {
        List<RemoteServer> list = remoteServerService.selectRemoteServerList(remoteServer);
        return new TableDataInfo(list, list.size());
    }
    
    /**
     * 获取远程服务器配置详细信息
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id) {
        return success(remoteServerService.selectRemoteServerById(id));
    }
    
    /**
     * 新增远程服务器配置
     */
    @PostMapping
    public Result add(@RequestBody RemoteServer remoteServer) {
        return toAjax(remoteServerService.insertRemoteServer(remoteServer));
    }
    
    /**
     * 修改远程服务器配置
     */
    @PutMapping
    public Result edit(@RequestBody RemoteServer remoteServer) {
        return toAjax(remoteServerService.updateRemoteServer(remoteServer));
    }
    
    /**
     * 删除远程服务器配置
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids) {
        return toAjax(remoteServerService.deleteRemoteServerByIds(ids));
    }
    
    /**
     * 测试服务器连接
     */
    @PostMapping("/{id}/test")
    public Result testConnection(@PathVariable("id") Long id) {
        RemoteServer server = remoteServerService.selectRemoteServerById(id);
        if (server == null) {
            return error("服务器配置不存在");
        }
        
        boolean success = sshService.testConnection(
            server.getServerIp(),
            server.getServerPort(),
            server.getUsername(),
            server.getPassword()
        );
        
        if (success) {
            return success("连接测试成功");
        } else {
            return error("连接测试失败，请检查服务器配置");
        }
    }
} 