package com.ruoyi.vlstream.test.vlstream.compute;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ComputeWorkspaceCleanupServiceTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final ComputeSsh ssh=mock(ComputeSsh.class);
    private final ComputeWorkspaceCleanupService service=new ComputeWorkspaceCleanupService(jdbc,ssh);

    @Test void deletesOnlyRecordedRunDirectoriesInsideRegisteredWorkRoots() throws Exception {
        ComputeNode node=new ComputeNode();node.setWorkDir("/data/work");
        when(jdbc.queryForList(contains("work_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.singletonList("/data/work"));
        when(jdbc.queryForList(contains("run_dir"),eq(String.class),eq("instance"))).thenReturn(Arrays.asList("/data/work/tenant/job-a","/data/work/tenant/job-b"));
        ComputeSsh.Connection connection=mock(ComputeSsh.Connection.class);
        when(ssh.open(node)).thenReturn(connection);
        assertEquals(2,service.clean(node,"instance"));
        verify(connection).execute("rm -rf '/data/work/tenant/job-a' && test ! -e '/data/work/tenant/job-a'",60);
        verify(connection).execute("rm -rf '/data/work/tenant/job-b' && test ! -e '/data/work/tenant/job-b'",60);
        verify(connection).close();
    }

    @Test void refusesDirectoryOutsideRegisteredWorkRoots() throws Exception {
        ComputeNode node=new ComputeNode();node.setWorkDir("/data/work");
        when(jdbc.queryForList(contains("work_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.singletonList("/data/work"));
        when(jdbc.queryForList(contains("run_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.singletonList("/etc/passwd"));
        assertThrows(IOException.class,()->service.clean(node,"instance"));
        verify(ssh,never()).open(any());
    }

    @Test void refusesParentTraversalAndBlankDirectories() throws Exception {
        ComputeNode node=new ComputeNode();node.setWorkDir("/data/work");
        when(jdbc.queryForList(contains("work_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.singletonList("/data/work"));
        when(jdbc.queryForList(contains("run_dir"),eq(String.class),eq("instance"))).thenReturn(Arrays.asList("/data/work/../etc"," "));
        assertThrows(IOException.class,()->service.clean(node,"instance"));
        verify(ssh,never()).open(any());
    }

    @Test void skipsConnectionWhenNoRunDirectoryWasRecorded() throws Exception {
        ComputeNode node=new ComputeNode();node.setWorkDir("/data/work");
        when(jdbc.queryForList(contains("work_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.singletonList("/data/work"));
        when(jdbc.queryForList(contains("run_dir"),eq(String.class),eq("instance"))).thenReturn(Collections.emptyList());
        assertEquals(0,service.clean(node,"instance"));
        verify(ssh,never()).open(any());
    }
}
