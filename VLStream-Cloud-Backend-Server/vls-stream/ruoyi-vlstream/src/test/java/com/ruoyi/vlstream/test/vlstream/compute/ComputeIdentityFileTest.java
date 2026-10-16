package com.ruoyi.vlstream.test.vlstream.compute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("dev")
class ComputeIdentityFileTest {
    @TempDir Path directory;
    @BeforeEach void windowsNtfs() throws Exception {
        assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows"));
        assumeTrue("NTFS".equalsIgnoreCase(Files.getFileStore(directory).type()));
    }
    private BasicFileAttributes attributes(Path path) throws Exception {
        return Files.readAttributes(path,BasicFileAttributes.class);
    }
    @Test void actualWindowsIdentityRemainsStableAndBlocksWritesUntilClosed() throws Exception {
        Path file=directory.resolve("fixture.txt");Files.write(file,new byte[]{1,2,3});
        try(ComputeIdentityFile identity=ComputeIdentityFile.open(file,attributes(file))) {
            assertNotNull(identity.key());assertTrue(identity.matches(file,attributes(file)));
            if(attributes(file).fileKey()==null)assertThrows(IOException.class,()->Files.write(file,new byte[]{4,5,6}));
        }
        Files.write(file,new byte[]{4,5,6});assertArrayEquals(new byte[]{4,5,6},Files.readAllBytes(file));
    }
    @Test void pathReplacementCannotReuseAnEarlierFileIdentity() throws Exception {
        Path file=directory.resolve("fixture.txt");Files.write(file,new byte[]{1,2,3});Object previous;
        try(ComputeIdentityFile identity=ComputeIdentityFile.open(file,attributes(file))){previous=identity.key();}
        Files.move(file,directory.resolve("retained-original.txt"));Files.write(file,new byte[]{1,2,3});
        try(ComputeIdentityFile identity=ComputeIdentityFile.open(file,attributes(file))){assertNotEquals(previous,identity.key());}
    }
    @Test void windowsFallbackRejectsHardLinkedIdentityFiles() throws Exception {
        Path file=directory.resolve("fixture.txt");Files.write(file,new byte[]{1,2,3});
        Files.createLink(directory.resolve("retained-alias.txt"),file);
        if(attributes(file).fileKey()==null)assertThrows(IOException.class,()->ComputeIdentityFile.open(file,attributes(file)));
    }
}
