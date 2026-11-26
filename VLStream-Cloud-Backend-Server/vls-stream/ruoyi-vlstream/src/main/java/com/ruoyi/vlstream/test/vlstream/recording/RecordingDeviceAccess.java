package com.ruoyi.vlstream.test.vlstream.recording;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.AuthorizedVideoCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reuses WVP's authorized, tenant-scoped protocol catalog, including offline devices.
 * Shared cross-tenant preview grants are deliberately not a recording entitlement.
 */
@Service
@RequiredArgsConstructor
public class RecordingDeviceAccess {
    private final AuthorizedVideoCatalog catalog;
    public void require(String protocol, String rowId) {
        for (int page = 1; page <= 2000; page++) {
            JsonNode data = catalog.page(page);
            for (JsonNode device : data.path("rows")) for (JsonNode source : device.path("sources")) {
                if (protocol.equals(source.path("protocol").asText()) && rowId.equals(source.path("rowId").asText())) return;
            }
            if (data.path("rows").size() == 0 || (long) page * AuthorizedVideoCatalog.PAGE_SIZE >= data.path("total").asLong()) break;
        }
        throw new ServiceException("设备不存在或无权访问其录像", 403);
    }
}
