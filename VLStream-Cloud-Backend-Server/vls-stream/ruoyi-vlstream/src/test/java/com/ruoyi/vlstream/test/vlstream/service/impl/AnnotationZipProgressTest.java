package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.DataTrainingPublisher;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationImageService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationLabelService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("dev")
class AnnotationZipProgressTest {
    @Test
    void severalBoxesOnOneOfTwoImagesMeansHalfComplete() throws Exception {
        verifyImport("0 .5 .5 .2 .2\n0 .2 .2 .1 .1\n0 .7 .7 .1 .1\n", "", 3, 1, 50);
    }

    @Test
    void severalBoxesPerImageStillMeansTwoAnnotatedImages() throws Exception {
        verifyImport("0 .5 .5 .2 .2\n0 .2 .2 .1 .1\n", "0 .5 .5 .2 .2\n", 3, 2, 100);
    }

    @Test
    void emptyLabelsMeanZeroProgress() throws Exception {
        verifyImport("", "", 0, 0, 0);
    }

    private void verifyImport(String first, String second, int boxes, int annotated, int progress) throws Exception {
        IFileUploadService uploads = mock(IFileUploadService.class);
        IVlsAnnotationLabelService labels = mock(IVlsAnnotationLabelService.class);
        FileResponseDto uploaded = new FileResponseDto();
        uploaded.setUrl("http://localhost/image.png");
        uploaded.setPath("image.png");
        when(uploads.uploadFile(anyString(), anyString(), any(java.io.File.class))).thenReturn(uploaded);
        when(labels.save(any(AnnotationLabel.class))).thenAnswer(call -> {
            ((AnnotationLabel) call.getArgument(0)).setId(1L);
            return true;
        });
        VlsAlgorithmAnnotationServiceImpl service = spy(new VlsAlgorithmAnnotationServiceImpl(
            mock(VlsAlgorithmAnnotationMapper.class), mock(IVlsAnnotationImageService.class),
            mock(IVlsAnnotationInstanceService.class), labels, uploads, new VlsSshProperties(),
            mock(DataTrainingPublisher.class), mock(DataManagementService.class)));
        AlgorithmAnnotation project = new AlgorithmAnnotation();
        project.setId(1L);
        doReturn(project).when(service).getById(1L);
        doAnswer(call -> {
            UpdateWrapper<?> update = call.getArgument(1);
            String sql = update.getSqlSet();
            Map<String, Object> params = update.getParamNameValuePairs();
            assertEquals(2, value(sql, params, "total_count"));
            assertEquals(annotated, value(sql, params, "annotated_count"));
            assertEquals(progress, value(sql, params, "progress"));
            return true;
        }).when(service).update(any(AlgorithmAnnotation.class), any(UpdateWrapper.class));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "png", png);
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            entry(zip, "dataset.yaml", "nc: 1\nnames: [helmet]\n".getBytes(StandardCharsets.UTF_8));
            entry(zip, "images/a.png", png.toByteArray());
            entry(zip, "images/b.png", png.toByteArray());
            entry(zip, "labels/a.txt", first.getBytes(StandardCharsets.UTF_8));
            entry(zip, "labels/b.txt", second.getBytes(StandardCharsets.UTF_8));
        }
        Map<String, Object> result = service.importAnnotationDatasetZip(1L,
            new MockMultipartFile("file", "dataset.zip", "application/zip", bytes.toByteArray()));
        assertEquals(boxes, result.get("totalInstances"));
        assertEquals(2, result.get("totalImages"));
        verify(service).update(any(AlgorithmAnnotation.class), any(UpdateWrapper.class));
    }

    private Object value(String sql, Map<String, Object> params, String column) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
            "(?:^|,)" + column + "=.#\\{ew.paramNameValuePairs\\.([^}]+)}".replace("=.#", "=#")).matcher(sql);
        if (!matcher.find()) throw new AssertionError(sql);
        return params.get(matcher.group(1));
    }

    private void entry(ZipOutputStream zip, String name, byte[] bytes) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}
