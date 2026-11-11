package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudRuntimeProofTest {
    @Test void missingInMemoryProofIsRecheckedAndReloaded() {
        ComputeSharedQueueService queue = mock(ComputeSharedQueueService.class);
        ComputeNodeService nodes = spy(new ComputeNodeService(mock(JdbcTemplate.class), mock(ComputeCredentialCipher.class), mock(ComputeSsh.class), new ObjectMapper()));
        ReflectionTestUtils.setField(nodes, "sharedQueue", queue);
        ComputeNode before = new ComputeNode(); before.setId(7L);
        ComputeNode after = new ComputeNode(); after.setId(7L);
        when(queue.isShared(before)).thenReturn(true);
        when(queue.proof(same(before))).thenThrow(new ServiceException("missing"));
        doReturn(true).when(nodes).probeSnapshot(before);
        doReturn(after).when(nodes).get(7L);
        assertSame(after, nodes.restoreSharedRuntimeProof(before));
        verify(queue).requireTarget(same(before)); verify(queue).requireTarget(same(after));
        verify(queue).proof(same(after));
    }
    @Test void failedRefreshCannotProceedWithStaleEvidence() {
        ComputeSharedQueueService queue = mock(ComputeSharedQueueService.class);
        ComputeNodeService nodes = spy(new ComputeNodeService(mock(JdbcTemplate.class), mock(ComputeCredentialCipher.class), mock(ComputeSsh.class), new ObjectMapper()));
        ReflectionTestUtils.setField(nodes, "sharedQueue", queue);
        ComputeNode node = new ComputeNode(); node.setId(7L);
        when(queue.isShared(node)).thenReturn(true);
        when(queue.proof(node)).thenThrow(new ServiceException("missing"));
        doReturn(false).when(nodes).probeSnapshot(node);
        assertThrows(ServiceException.class, () -> nodes.restoreSharedRuntimeProof(node));
        verify(nodes, never()).get(7L);
    }
}
