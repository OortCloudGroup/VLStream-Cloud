package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import javax.sql.DataSource;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named = "VLS_DATA_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:33316/vls_data_test.*")
class DataManagementIntegrationTest {
    private static AnnotationConfigApplicationContext context;
    private DataManagementService data;
    private DataTransferService transfer;
    private SqlSessionTemplate sql;

    @BeforeAll static void open() {
        System.setProperty("vlstream.data-management.temp-directory", DataTestConfiguration.artifacts().resolve("temporary").toString());
        context = new AnnotationConfigApplicationContext(DataTestConfiguration.class);
        try {
            java.util.Properties properties = new java.util.Properties(); properties.setProperty("classpath", System.getProperty("java.class.path"));
            Files.createDirectories(DataTestConfiguration.artifacts());
            Files.write(DataTestConfiguration.artifacts().resolve("runtime-classpath.txt"), System.getProperty("java.class.path").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @AfterAll static void close() { if (context != null) context.close(); }
    @BeforeEach void prepare() throws Exception {
        DataTestConfiguration.initialize(context.getBean(DataSource.class)); TenantContextHolder.setTenantId("tenant-a");
        data = context.getBean(DataManagementService.class); transfer = context.getBean(DataTransferService.class); sql = context.getBean(SqlSessionTemplate.class);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void migrationPreservesLegacyProjectAndJsonRules() {
        AlgorithmAnnotation project = data.project(10L);
        assertEquals("PRJ-10", project.getProjectCode()); assertTrue(project.getAnnotationRules().contains("legacy"));
        assertEquals("tenant-a", data.sample(10L, 15L).getTenantId());
        DataRequests.Project edit = request("UPDATED"); edit.setAnnotationRules("框住完整目标，不包含背景");
        data.saveProject(10L, edit); assertEquals(edit.getAnnotationRules(), data.project(10L).getAnnotationRules());
    }

    @Test void rejectsCrossTenantProjectsSamplesAndVersions() {
        Long project = data.saveProject(null, request("A")).getId();
        DataRequests.Version request = new DataRequests.Version(); request.setName("empty"); Long version = data.saveVersion(project, request).getId();
        TenantContextHolder.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> data.project(project));
        assertThrows(ServiceException.class, () -> data.version(project, version));
        assertEquals(0, data.projects(null, 1, 20).getTotal());
        TenantContextHolder.clear(); assertThrows(ServiceException.class, () -> data.projects(null, 1, 20));
    }

    @Test void importSearchSplitCompareAndRestoreRecoverDeletedFileAndAnnotations() throws Exception {
        Long project = data.saveProject(null, request("ROUNDTRIP")).getId();
        for (int i = 1; i <= 4; i++) upload(project, i);
        AnnotationLabel label = new AnnotationLabel(); label.setAnnotationId(project); label.setName("车辆"); label.setColor("#409eff"); label.setIsDeleted(0); label.setStatus(1); sql.getMapper(VlsAnnotationLabelMapper.class).insert(label);
        List<AnnotationImage> members = data.snapshot(project).getSamples();
        for (AnnotationImage sample : members) annotate(project, sample.getId(), label.getId());
        DataRequests.SampleEdit edit = new DataRequests.SampleEdit(); edit.setImageName("车辆样本.png"); edit.setSampleSource("line-a"); edit.setTags(Arrays.asList("白天", "道路"));
        data.editSample(project, members.get(0).getId(), edit);
        DataRequests.SampleQuery query = new DataRequests.SampleQuery(); query.setTag("车辆"); query.setAnnotationStatus("annotated"); assertEquals(4, data.samples(project, query).getTotal());
        query.setTag("白天"); query.setSource("line-a"); assertEquals(1, data.samples(project, query).getTotal());
        query.setTag("' OR 1=1 --"); assertEquals(0, data.samples(project, query).getTotal());
        DatasetVersion version = data.split(project, new DataRequests.Split()); assertEquals(3, version.getTrainCount()); assertEquals(1, version.getValidationCount());
        DataRequests.Batch batch = new DataRequests.Batch(); batch.setIds(Collections.singletonList(members.get(0).getId())); batch.setAction("delete"); data.batch(project, batch);
        assertEquals(3, data.snapshot(project).getSamples().size());
        assertEquals(1, ((List<?>) data.compare(project, version.getId(), null).get("removed")).size());
        data.restore(project, version.getId());
        assertEquals(4, data.snapshot(project).getSamples().size()); assertEquals(4, data.snapshot(project).getInstances().size());
        assertEquals(0, ((List<?>) data.compare(project, version.getId(), null).get("changed")).size());
        assertEquals(3, data.versions(project).size());
        assertNull(data.project(project).getDatasetPath());
    }

    @Test void rawImportReportsCorruptionAndDuplicateWithoutCreatingExtraRows() throws Exception {
        Long project = data.saveProject(null, request("IMPORT")).getId();
        upload(project, 1); Map<String, Object> duplicate = upload(project, 1);
        assertEquals(Boolean.TRUE, ((List<Map<String, Object>>) duplicate.get("results")).get(0).get("duplicate"));
        Map<String, Object> invalid = transfer.upload(project, new MockMultipartFile[]{new MockMultipartFile("files", "fake.jpg", "image/jpeg", new byte[]{1, 2, 3})}, "test");
        assertEquals(0L, invalid.get("successCount")); assertEquals(1, data.snapshot(project).getSamples().size());
        assertTrue(((List<?>) transfer.inspect(project, Collections.singletonList(data.snapshot(project).getSamples().get(0).getId())).get("results")).size() == 1);
    }

    @Test void exportedZipContainsActualFilesAndCanImportIntoAnotherProject() throws Exception {
        Long project = data.saveProject(null, request("EXPORT")).getId(); upload(project, 3);
        AnnotationImage sample = data.snapshot(project).getSamples().get(0);
        AnnotationLabel label = new AnnotationLabel(); label.setAnnotationId(project); label.setName("car"); label.setIsDeleted(0); label.setStatus(1); sql.getMapper(VlsAnnotationLabelMapper.class).insert(label);
        annotate(project, sample.getId(), label.getId());
        Path archive = transfer.exportArchive(data.snapshot(project));
        try {
            try (ZipFile zip = new ZipFile(archive.toFile())) { assertEquals(2, zip.size()); assertNotNull(zip.getEntry("vls-samples.json")); }
            Long target = data.saveProject(null, request("TARGET")).getId();
            transfer.importArchive(target, new MockMultipartFile("file", "samples.zip", "application/zip", Files.readAllBytes(archive)));
            assertEquals(1, data.snapshot(target).getSamples().size()); assertEquals(1, data.snapshot(target).getInstances().size());
            assertEquals(sample.getContentSha256(), data.snapshot(target).getSamples().get(0).getContentSha256());
        } finally { Files.deleteIfExists(archive); }
    }

    @Test void zipTraversalRejectedAndMissingObjectsDoNotProduceSuccessArchive() throws Exception {
        Long project = data.saveProject(null, request("ZIP")).getId();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) { zip.putNextEntry(new ZipEntry("../escape.png")); zip.write(new byte[]{1}); zip.closeEntry(); }
        assertThrows(ServiceException.class, () -> transfer.importArchive(project, new MockMultipartFile("file", "bad.zip", "application/zip", bytes.toByteArray())));
        upload(project, 1); DatasetSnapshot snapshot = data.snapshot(project); snapshot.getSamples().get(0).setLocalPath("missing.png");
        assertThrows(RuntimeException.class, () -> transfer.exportArchive(snapshot));
    }

    @Test void foreignSampleInBatchRollsBackWholeOperation() throws Exception {
        Long first = data.saveProject(null, request("FIRST")).getId(), second = data.saveProject(null, request("SECOND")).getId();
        upload(first, 1); upload(second, 2);
        Long a = data.snapshot(first).getSamples().get(0).getId(), b = data.snapshot(second).getSamples().get(0).getId();
        DataRequests.Batch batch = new DataRequests.Batch(); batch.setAction("delete"); batch.setIds(Arrays.asList(a, b));
        assertThrows(ServiceException.class, () -> data.batch(first, batch)); assertNotNull(data.sample(first, a));
    }

    @Test void restoreClearsFieldsThatWereNullInHistoricalVersion() throws Exception {
        Long project = data.saveProject(null, request("NULL-RESTORE")).getId(); upload(project, 11);
        AnnotationImage sample = data.snapshot(project).getSamples().get(0);
        sql.getMapper(DataSampleMapper.class).update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<AnnotationImage>().eq("id", sample.getId()).set("sample_tags", null).set("quality_checked_at", null));
        DataRequests.Version request = new DataRequests.Version(); request.setName("before"); DatasetVersion version = data.saveVersion(project, request);
        DataRequests.SampleEdit edit = new DataRequests.SampleEdit(); edit.setImageName("new.png"); edit.setSampleSource("updated"); edit.setTags(Collections.singletonList("changed")); data.editSample(project, sample.getId(), edit);
        data.restore(project, version.getId()); AnnotationImage restored = data.sample(project, sample.getId());
        assertNull(restored.getSampleTags()); assertNull(restored.getQualityCheckedAt()); assertEquals(sample.getImageName(), restored.getImageName());
    }

    @Test void failedArchiveRegistrationRollsBackAllDatabaseRows() throws Exception {
        Long project = data.saveProject(null, request("ATOMIC-ZIP")).getId();
        PendingSampleImport staged = new PendingSampleImport("a.png", "a.png", "test/a.png", 100, new SampleMediaInspector().inspect("a.png", SampleMediaInspectorTest.image(true, 50)));
        DatasetSnapshot manifest = new DatasetSnapshot(); manifest.setSamples(Collections.emptyList()); manifest.setInstances(Collections.emptyList());
        AnnotationLabel invalid = new AnnotationLabel(); invalid.setId(123L); invalid.setName(null); manifest.setLabels(Collections.singletonList(invalid));
        assertThrows(RuntimeException.class, () -> data.registerArchive(project, Collections.singletonList(staged), manifest));
        assertEquals(0, data.snapshot(project).getSamples().size());
    }

    private DataRequests.Project request(String code) { DataRequests.Project request = new DataRequests.Project(); request.setProjectCode(code); request.setAnnotationName(code); request.setAnnotationRules("有效规则"); return request; }
    private Map<String, Object> upload(Long project, int seed) throws Exception { return transfer.upload(project, new MockMultipartFile[]{new MockMultipartFile("files", "sample" + seed + ".png", "image/png", SampleMediaInspectorTest.image(true, seed))}, "line-a"); }
    private void annotate(Long project, Long sampleId, Long labelId) {
        AnnotationInstance annotation = new AnnotationInstance(); annotation.setAnnotationId(project); annotation.setImageId(sampleId); annotation.setLabelId(labelId); annotation.setAnnotationType(AlgorithmAnnotationTypeEnum.rect);
        annotation.setAnnotationData("{\"x\":10,\"y\":10,\"width\":20,\"height\":20}"); annotation.setIsDeleted(0); annotation.setStatus(1);
        sql.getMapper(VlsAnnotationInstanceMapper.class).insert(annotation);
    }
}
