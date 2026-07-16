package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.RemoteServer;
import com.ruoyi.vlstream.service.IVlsRemoteServersService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/** Management API over the same remote-server table consumed by training jobs. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsRemoteServers")
public class VlsRemoteServersController extends VlsControllerSupport {

    private final IVlsRemoteServersService remoteServersService;

    /** Return one matching remote training server. */
    @GetMapping("/detail")
    public BladeResult<RemoteServer> detail(RemoteServer filter) {
        return BladeResult.success(remoteServersService.getOne(query(filter).last("limit 1")));
    }

    /** Return a SpringBlade-compatible page of remote servers. */
    @GetMapping("/list")
    public BladeResult<BladePage<RemoteServer>> list(RemoteServer filter,
                                                     @RequestParam(required = false) Long current,
                                                     @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Return the source custom-page route over the real server table. */
    @GetMapping("/page")
    public BladeResult<BladePage<RemoteServer>> page(RemoteServer filter,
                                                     @RequestParam(required = false) Long current,
                                                     @RequestParam(required = false) Long size) {
        return pageResult(filter, current, size);
    }

    /** Add a remote server that can immediately be selected by the training chain. */
    @PostMapping("/save")
    public BladeResult<RemoteServer> save(@RequestBody RemoteServer remoteServer) {
        return persist(remoteServer, false);
    }

    /** Modify a remote training server. */
    @PostMapping("/update")
    public BladeResult<RemoteServer> update(@RequestBody RemoteServer remoteServer) {
        try {
            requireId(remoteServer);
            return remoteServersService.updateById(remoteServer)
                ? BladeResult.success(remoteServersService.getById(remoteServer.getId()))
                : BladeResult.<RemoteServer>fail("Remote server update affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Insert or update a remote training server. */
    @PostMapping("/submit")
    public BladeResult<RemoteServer> submit(@RequestBody RemoteServer remoteServer) {
        return persist(remoteServer, true);
    }

    /** Delete remote-server rows by primary key from the non-logical source table. */
    @GetMapping("/remove")
    public BladeResult<Boolean> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.<Boolean>fail("ids is required")
                : BladeResult.success(remoteServersService.removeByIds(parsed));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export the actual matching server rows as an Excel workbook. */
    @GetMapping("/export-vlsRemoteServers")
    public void exportVlsRemoteServers(RemoteServer filter, HttpServletResponse response) {
        List<RemoteServer> rows = remoteServersService.list(query(filter).orderByDesc(RemoteServer::getCreateTime));
        ExcelUtil.exportExcel(rows, "Remote Server Configuration", RemoteServer.class, response);
    }

    /** Execute the shared real-database paging query. */
    private BladeResult<BladePage<RemoteServer>> pageResult(RemoteServer filter, Long current, Long size) {
        Page<RemoteServer> result = remoteServersService.page(
            new Page<RemoteServer>(current(current), size(size)),
            query(filter).orderByDesc(RemoteServer::getCreateTime).orderByDesc(RemoteServer::getId));
        return BladeResult.success(BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent()));
    }

    /** Persist a validated remote-server row. */
    private BladeResult<RemoteServer> persist(RemoteServer remoteServer, boolean upsert) {
        try {
            boolean stored = upsert ? remoteServersService.saveOrUpdate(remoteServer) : remoteServersService.save(remoteServer);
            return stored ? BladeResult.success(remoteServersService.getById(remoteServer.getId()))
                : BladeResult.<RemoteServer>fail("Remote server was not persisted");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Build a safe typed query from the source-compatible server filter. */
    private LambdaQueryWrapper<RemoteServer> query(RemoteServer filter) {
        LambdaQueryWrapper<RemoteServer> query = new LambdaQueryWrapper<RemoteServer>();
        if (filter == null) {
            return query;
        }
        if (filter.getId() != null) {
            query.eq(RemoteServer::getId, filter.getId());
        }
        if (StringUtils.hasText(filter.getServerName())) {
            query.like(RemoteServer::getServerName, filter.getServerName().trim());
        }
        if (StringUtils.hasText(filter.getServerIp())) {
            query.eq(RemoteServer::getServerIp, filter.getServerIp().trim());
        }
        if (filter.getStatus() != null) {
            query.eq(RemoteServer::getStatus, filter.getStatus());
        }
        return query;
    }

    /** Require an ID for the explicit update route. */
    private void requireId(RemoteServer remoteServer) {
        if (remoteServer == null || remoteServer.getId() == null) {
            throw new IllegalArgumentException("Remote server ID is required");
        }
    }
}
