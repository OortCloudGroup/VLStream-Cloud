package com.ruoyi.vlstream.test.vlstream.compute;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.win32.StdCallLibrary;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Stable file identity; NTFS fallback also holds a read-only, non-replaceable handle. */
final class ComputeIdentityFile implements AutoCloseable {
    private final Object key;
    private final Pointer handle;
    private ComputeIdentityFile(Object key, Pointer handle) { this.key=key;this.handle=handle; }

    static ComputeIdentityFile open(Path path, BasicFileAttributes attributes) throws IOException {
        if(attributes.fileKey()!=null)return new ComputeIdentityFile(attributes.fileKey(),null);
        if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows")
            || !"NTFS".equalsIgnoreCase(Files.getFileStore(path).type()))
            throw new IOException("无法取得稳定身份文件标识");
        Pointer opened=null;
        try {
            // GENERIC_READ, FILE_SHARE_READ only, OPEN_EXISTING, OPEN_REPARSE_POINT.
            // Deny writes/deletes while the Java channel reads the same approved path.
            opened=Kernel32Api.INSTANCE.CreateFileW(new WString(path.toString()),0x80000000,1,null,3,0x00200000,null);
            if(opened==null || Pointer.nativeValue(opened)==-1L)throw new IOException("无法锁定身份文件读取");
            FileInformation information=new FileInformation();
            if(!Kernel32Api.INSTANCE.GetFileInformationByHandle(opened,information))
                throw new IOException("无法取得身份文件标识");
            if((information.attributes & (0x400|0x10))!=0 || information.links!=1
                || (information.indexHigh==0 && information.indexLow==0))
                throw new IOException("身份文件不能为重解析路径、目录或硬链接");
            Object key=information.volume+":"+information.indexHigh+":"+information.indexLow;
            return new ComputeIdentityFile(key,opened);
        } catch(IOException error) {
            if(opened!=null && Pointer.nativeValue(opened)!=-1L)Kernel32Api.INSTANCE.CloseHandle(opened);
            throw error;
        } catch(RuntimeException | LinkageError error) {
            if(opened!=null && Pointer.nativeValue(opened)!=-1L)Kernel32Api.INSTANCE.CloseHandle(opened);
            throw new IOException("本机稳定身份文件检查不可用");
        }
    }
    Object key(){return key;}
    boolean matches(Path path, BasicFileAttributes attributes) throws IOException {
        try(ComputeIdentityFile current=open(path,attributes)){return key.equals(current.key);}
    }
    @Override public void close() throws IOException {
        if(handle!=null && !Kernel32Api.INSTANCE.CloseHandle(handle))throw new IOException("身份文件读取句柄未释放");
    }
    public interface Kernel32Api extends StdCallLibrary {
        Kernel32Api INSTANCE=Native.load("kernel32",Kernel32Api.class);
        Pointer CreateFileW(WString path,int access,int share,Pointer security,int disposition,int flags,Pointer template);
        boolean GetFileInformationByHandle(Pointer handle,FileInformation information);
        boolean CloseHandle(Pointer handle);
    }
    public static class FileInformation extends Structure {
        public int attributes,creationLow,creationHigh,accessLow,accessHigh,writeLow,writeHigh;
        public int volume,sizeHigh,sizeLow,links,indexHigh,indexLow;
        @Override protected List<String> getFieldOrder(){return Arrays.asList("attributes","creationLow","creationHigh",
            "accessLow","accessHigh","writeLow","writeHigh","volume","sizeHigh","sizeLow","links","indexHigh","indexLow");}
    }
}
