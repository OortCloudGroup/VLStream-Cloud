package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class OmConversionServiceTest {
    private Path directory;
    @BeforeEach void setup() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        Path test = root.resolve("codex/om-converter/tests"); Files.createDirectories(test);
        directory = Files.createTempDirectory(test, "calibration-");
    }
    @AfterEach void cleanup() throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
            for (Path p : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
        }
    }
    @Test void selectsOnlyFrozenTrainingImagesAndCapsCalibrationCount() throws Exception {
        Path zip = directory.resolve("data.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (int i=0; i<25; i++) { out.putNextEntry(new ZipEntry("images/train/"+i+".png")); out.write(i); out.closeEntry(); }
            out.putNextEntry(new ZipEntry("images/val/held-out.png")); out.write(100); out.closeEntry();
        }
        Map<String,String> result = OmConversionService.calibration(zip, directory.resolve("output"));
        assertEquals(20, result.size());
        assertEquals(OmConversionService.sha(directory.resolve("output/0000.png")), result.get("0000.png"));
        assertEquals(19, Files.readAllBytes(directory.resolve("output/0019.png"))[0]);
    }
    @Test void rejectsMissingTrainingCalibrationAndDoesNotExtractTraversalEntries() throws Exception {
        Path zip = directory.resolve("bad.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("images/train/../../escaped.png")); out.write(1); out.closeEntry();
            out.putNextEntry(new ZipEntry("images/val/1.png")); out.write(2); out.closeEntry();
        }
        assertThrows(ServiceException.class, () -> OmConversionService.calibration(zip, directory.resolve("output")));
        assertFalse(Files.exists(directory.resolve("escaped.png")));
    }
    @Test void disabledOptionalServiceDoesNotTouchBusinessData() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        OmConversionService service = new OmConversionService(new OmConversionProperties(), jdbc, mock(PlatformTransactionManager.class),
            mock(IVlsAlgorithmTrainingService.class), mock(DataManagementService.class), mock(DatasetConversionGuard.class),
            mock(TrainingDatasetArtifactService.class), mock(ModelArtifactObjectStore.class), new ObjectMapper());
        assertThrows(ServiceException.class, () -> service.enqueue(1L, true));
        verifyNoInteractions(jdbc);
        service.scan(); verifyNoInteractions(jdbc); service.shutdown();
    }
}
