package com.ruoyi.vlstream.test.vlstream.data;
import com.jcraft.jsch.*;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import org.junit.jupiter.api.*;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class DatasetRemoteCleanupTest {
    final DatasetRemoteCleanup cleanup = new DatasetRemoteCleanup(new VlsSshProperties());
    ChannelSftp sftp;
    String root = DatasetRemoteCleanup.root(1L);
    @BeforeEach void setup() throws Exception {
        sftp=mock(ChannelSftp.class);
        when(sftp.lstat(root)).thenReturn(mock(SftpATTRS.class));
        when(sftp.realpath(root)).thenReturn(root);
    }
    ChannelSftp.LsEntry file(String name) {
        ChannelSftp.LsEntry entry=mock(ChannelSftp.LsEntry.class);
        SftpATTRS attrs=mock(SftpATTRS.class);
        when(entry.getFilename()).thenReturn(name); when(entry.getAttrs()).thenReturn(attrs); when(attrs.isReg()).thenReturn(true);
        return entry;
    }
    @Test void inventoriesAllFilesBeforeDeletingAnything() throws Exception {
        Vector<ChannelSftp.LsEntry> list=new Vector<>(); list.add(file("image.png")); list.add(file("best.pt")); when(sftp.ls(root)).thenReturn(list);
        assertThrows(IllegalStateException.class,()->cleanup.removeDirectory(sftp,1L)); verify(sftp,never()).rm(anyString()); verify(sftp,never()).rmdir(anyString());
    }
    @Test void rejectsSymlinkedRoot() throws Exception {
        when(sftp.realpath(root)).thenReturn("/models");
        assertThrows(IllegalStateException.class,()->cleanup.removeDirectory(sftp,1L)); verify(sftp,never()).rm(anyString());
    }
    @Test void removesOnlyInventoriedDatasetFiles() throws Exception {
        Vector<ChannelSftp.LsEntry> list=new Vector<>(); list.add(file("image.png")); list.add(file("dataset.yaml")); when(sftp.ls(root)).thenReturn(list);
        cleanup.removeDirectory(sftp,1L); verify(sftp).rm(root+"/image.png"); verify(sftp).rm(root+"/dataset.yaml"); verify(sftp).rmdir(root);
    }
    @Test void missingRootIsSafeToRetry() throws Exception {
        when(sftp.lstat(root)).thenThrow(new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE,"gone"));
        cleanup.removeDirectory(sftp,1L); verify(sftp,never()).rm(anyString()); verify(sftp,never()).rmdir(anyString());
    }
}
