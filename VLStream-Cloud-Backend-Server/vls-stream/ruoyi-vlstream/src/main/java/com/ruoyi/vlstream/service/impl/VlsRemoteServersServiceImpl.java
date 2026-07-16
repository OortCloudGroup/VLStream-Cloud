package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.vlstream.domain.RemoteServer;
import com.ruoyi.vlstream.mapper.VlsRemoteServerMapper;
import com.ruoyi.vlstream.service.IVlsRemoteServersService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** Real database service over the same remote-server table used by training. */
@Service
public class VlsRemoteServersServiceImpl
    extends ServiceImpl<VlsRemoteServerMapper, RemoteServer>
    implements IVlsRemoteServersService {

    /** Persist a remote server with stable operational defaults. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(RemoteServer entity) {
        validate(entity);
        Date now = new Date();
        if (entity.getServerPort() == null) {
            entity.setServerPort(Integer.valueOf(22));
        }
        if (entity.getStatus() == null) {
            entity.setStatus(Integer.valueOf(1));
        }
        if (entity.getCreateTime() == null) {
            entity.setCreateTime(now);
        }
        entity.setUpdateTime(now);
        return super.save(entity);
    }

    /** Update an existing remote training server. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(RemoteServer entity) {
        validate(entity);
        entity.setUpdateTime(new Date());
        return super.updateById(entity);
    }

    /** Insert or update a remote server according to its primary key. */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(RemoteServer entity) {
        return entity != null && entity.getId() != null && getById(entity.getId()) != null
            ? updateById(entity)
            : save(entity);
    }

    /** Reject incomplete SSH configurations before they reach the training chain. */
    private void validate(RemoteServer entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Remote server is required");
        }
        if (entity.getServerName() == null || entity.getServerName().trim().isEmpty()
            || entity.getServerIp() == null || entity.getServerIp().trim().isEmpty()
            || entity.getUsername() == null || entity.getUsername().trim().isEmpty()
            || entity.getPassword() == null || entity.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Server name, IP, username and password are required");
        }
    }
}
