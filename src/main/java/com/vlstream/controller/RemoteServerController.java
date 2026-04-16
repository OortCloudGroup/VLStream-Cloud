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
 * Remote Server Management Controller
 */
@Api(tags = "Remote Server Management")
@RestController
@RequestMapping("/api/remote-servers")
public class RemoteServerController extends BaseController {
    
    @Autowired
    private RemoteServerService remoteServerService;
    
    @Autowired
    private SSHService sshService;
    
    /**
     * Query remote server configuration list
     */
    @GetMapping("/list")
    public TableDataInfo list(RemoteServer remoteServer) {
        List<RemoteServer> list = remoteServerService.selectRemoteServerList(remoteServer);
        return new TableDataInfo(list, list.size());
    }
    
    /**
     * Get remote server configuration details
     */
    @GetMapping(value = "/{id}")
    public Result getInfo(@PathVariable("id") Long id) {
        return success(remoteServerService.selectRemoteServerById(id));
    }
    
    /**
     * Add remote server configuration
     */
    @PostMapping
    public Result add(@RequestBody RemoteServer remoteServer) {
        return toAjax(remoteServerService.insertRemoteServer(remoteServer));
    }
    
    /**
     * Update remote server configuration
     */
    @PutMapping
    public Result edit(@RequestBody RemoteServer remoteServer) {
        return toAjax(remoteServerService.updateRemoteServer(remoteServer));
    }
    
    /**
     * Delete remote server configuration
     */
    @DeleteMapping("/{ids}")
    public Result remove(@PathVariable Long[] ids) {
        return toAjax(remoteServerService.deleteRemoteServerByIds(ids));
    }
    
    /**
     * Test server connection
     */
    @PostMapping("/{id}/test")
    public Result testConnection(@PathVariable("id") Long id) {
        RemoteServer server = remoteServerService.selectRemoteServerById(id);
        if (server == null) {
            return error("Server configuration does not exist");
        }
        
        boolean success = sshService.testConnection(
            server.getServerIp(),
            server.getServerPort(),
            server.getUsername(),
            server.getPassword()
        );
        
        if (success) {
            return success("Connection test successful");
        } else {
            return error("Connection test failed, please check server configuration");
        }
    }
} 