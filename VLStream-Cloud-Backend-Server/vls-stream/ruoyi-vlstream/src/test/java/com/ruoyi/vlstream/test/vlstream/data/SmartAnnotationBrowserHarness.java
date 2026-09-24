package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.controller.*;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.mockito.Mockito.mock;

/** Real HTTP/controllers/transactions with synthetic predictions, exclusively on the disposable test schema. */
public class SmartAnnotationBrowserHarness {
    static Path artifacts() { return DataTestConfiguration.root().resolve("codex/smart-annotation"); }
    public static void main(String[] args) throws Exception {
        if (!System.getenv("VLS_DATA_TEST_JDBC").startsWith("jdbc:mysql://127.0.0.1:33316/vls_data_test_smart?")) throw new IllegalStateException("Dedicated test database required");
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        Files.createDirectories(artifacts().resolve("test-objects"));
        Tomcat tomcat = new Tomcat(); tomcat.setPort(38082); tomcat.setHostname("127.0.0.1"); tomcat.setBaseDir(artifacts().resolve("tomcat").toString()); tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context servlet = tomcat.addContext("", artifacts().toString());
        AnnotationConfigWebApplicationContext web = new AnnotationConfigWebApplicationContext(); web.register(WebConfig.class);
        Tomcat.addServlet(servlet, "dispatcher", new DispatcherServlet(web)).setLoadOnStartup(1); servlet.addServletMappingDecoded("/", "dispatcher"); tomcat.start();
        DataTestConfiguration.initialize(web.getBean(DataSource.class));
        JdbcTemplate jdbc = new JdbcTemplate(web.getBean(DataSource.class));
        for (String table : Arrays.asList("vls_smart_annotation_candidate", "vls_smart_annotation_round", "vls_smart_annotation_task")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        String migration = new String(Files.readAllBytes(DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_022__smart_annotation.sql")), StandardCharsets.UTF_8);
        for (String statement : migration.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        SmartAnnotationIntegrationTest.applyFourTypeMigration(jdbc);
        seed(web, jdbc); seedFour(web, jdbc); web.getBean(SmartAnnotationConfirmationWorker.class).start();
        System.out.println("SMART_BROWSER_READY http://127.0.0.1:38082"); tomcat.getServer().await();
    }

    static void seed(AnnotationConfigWebApplicationContext web, JdbcTemplate jdbc) throws Exception {
        TenantContextHolder.setTenantId("tenant-a"); DataManagementService data = web.getBean(DataManagementService.class); SmartAnnotationService smart = web.getBean(SmartAnnotationService.class); SqlSessionTemplate sql = web.getBean(SqlSessionTemplate.class);
        DataRequests.Project request = new DataRequests.Project(); request.setProjectCode("SMART-BROWSER"); request.setAnnotationName("道路车辆 · 智能标注验收"); Long dataset = data.saveProject(null, request).getId();
        AnnotationLabel label = new AnnotationLabel(); label.setAnnotationId(dataset); label.setName("车辆"); label.setColor("#409eff"); label.setTenantId("tenant-a"); label.setIsDeleted(0); label.setStatus(1); sql.getMapper(VlsAnnotationLabelMapper.class).insert(label);
        for (int index = 1; index <= 3; index++) {
            java.awt.image.BufferedImage bitmap = new java.awt.image.BufferedImage(640, 400, java.awt.image.BufferedImage.TYPE_INT_RGB); java.awt.Graphics2D g = bitmap.createGraphics();
            g.setColor(new java.awt.Color(179, 210, 228)); g.fillRect(0, 0, 640, 400); g.setColor(new java.awt.Color(66, 83, 94)); g.fillRect(0, 260, 640, 140);
            g.setColor(new java.awt.Color(43, 121, 173)); g.fillRoundRect(110 + index * 10, 190, 250, 105, 20, 20); g.setColor(java.awt.Color.BLACK); g.fillOval(150 + index * 10, 270, 40, 40); g.fillOval(280 + index * 10, 270, 40, 40); g.dispose();
            String key = "road-" + index + ".png"; javax.imageio.ImageIO.write(bitmap, "png", artifacts().resolve("test-objects").resolve(key).toFile());
            AnnotationImage image = new AnnotationImage(); image.setAnnotationId(dataset); image.setImageName(key); image.setOriginalName(key); image.setLocalPath(key); image.setMediaType("image"); image.setMediaWidth(640); image.setMediaHeight(400); image.setQualityStatus("accepted"); image.setDatasetSplit("unassigned"); image.setIsDeleted(0); image.setStatus(1); sql.getMapper(DataSampleMapper.class).insert(image);
        }
        SmartAnnotationRequests.Create create = new SmartAnnotationRequests.Create(); create.setTaskName("道路车辆预标注验收"); create.setDatasetId(dataset); create.setMode("model"); create.setSourceType("model"); create.setSourceId(100L);
        SmartAnnotationTask task = smart.create(create); SmartAnnotationRound round = smart.current(task);
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='REVIEW' WHERE id=?", task.getId()); jdbc.update("UPDATE vls_smart_annotation_round SET round_state='REVIEW',predicted_count=3 WHERE id=?", round.getId());
        int index = 0;
        for (AnnotationImage image : data.snapshot(dataset).getSamples()) {
            SmartAnnotationRequests.Box box = new SmartAnnotationRequests.Box(); box.setLabelId(index == 1 ? null : label.getId()); box.setClassName(index == 1 ? "car" : "车辆"); box.setX(120d); box.setY(180d); box.setWidth(260d); box.setHeight(140d); box.setConfidence(.86);
            SmartAnnotationCandidate candidate = new SmartAnnotationCandidate(); candidate.setTaskId(task.getId()); candidate.setRoundId(round.getId()); candidate.setImageId(image.getId()); candidate.setImageWidth(640); candidate.setImageHeight(400); candidate.setReviewState("PENDING"); candidate.setUncertainty(.8 - index * .2); candidate.setHardExample(0); candidate.setBoxesJson(data.writeJson(index == 2 ? Collections.emptyList() : Collections.singletonList(box))); candidate.setIsDeleted(0); candidate.setStatus(1); sql.getMapper(SmartAnnotationCandidateMapper.class).insert(candidate); index++;
        }
        Files.write(artifacts().resolve("browser-task.json"), data.writeJson(DataManagementService.map("taskId", task.getId().toString(), "datasetId", dataset.toString())).getBytes(StandardCharsets.UTF_8)); TenantContextHolder.clear();
    }

    static void seedFour(AnnotationConfigWebApplicationContext web, JdbcTemplate jdbc) throws Exception {
        TenantContextHolder.setTenantId("tenant-a"); DataManagementService data = web.getBean(DataManagementService.class);
        SmartAnnotationService smart = web.getBean(SmartAnnotationService.class); SqlSessionTemplate sql = web.getBean(SqlSessionTemplate.class);
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (AnnotationTaskType type : AnnotationTaskType.values()) {
            DataRequests.Project request = new DataRequests.Project(); request.setProjectCode("FOUR-" + type.name()); request.setAnnotationName(type.getLabel() + "验收"); request.setAnnotationType(type.getCode());
            Long dataset = data.saveProject(null, request).getId(); List<AnnotationLabel> labels = new ArrayList<>();
            for (String name : Arrays.asList("车辆", "背景")) {
                AnnotationLabel label = new AnnotationLabel(); label.setAnnotationId(dataset); label.setName(name); label.setColor(labels.isEmpty() ? "#409eff" : "#f59e42"); label.setTenantId("tenant-a"); label.setIsDeleted(0); label.setStatus(1); sql.getMapper(VlsAnnotationLabelMapper.class).insert(label); labels.add(label);
            }
            for (int index = 1; index <= 2; index++) {
                AnnotationImage image = new AnnotationImage(); image.setAnnotationId(dataset); image.setImageName("road-" + index + ".png"); image.setOriginalName(image.getImageName()); image.setLocalPath(image.getImageName()); image.setMediaType("image"); image.setMediaWidth(640); image.setMediaHeight(400); image.setQualityStatus("accepted"); image.setDatasetSplit("unassigned"); image.setIsDeleted(0); image.setStatus(1); sql.getMapper(DataSampleMapper.class).insert(image);
            }
            SmartAnnotationRequests.Create create = new SmartAnnotationRequests.Create(); create.setTaskName(type.getLabel() + "智能标注验收"); create.setDatasetId(dataset); create.setMode("model"); create.setSourceType("system"); create.setSourceId(0L);
            SmartAnnotationTask task = smart.create(create); SmartAnnotationRound round = smart.current(task);
            jdbc.update("UPDATE vls_smart_annotation_task SET task_state='REVIEW' WHERE id=?", task.getId()); jdbc.update("UPDATE vls_smart_annotation_round SET round_state='REVIEW',predicted_count=2 WHERE id=?", round.getId());
            for (AnnotationImage image : data.snapshot(dataset).getSamples()) {
                List<SmartAnnotationRequests.Box> regions = new ArrayList<>(); SmartAnnotationRequests.Box region = new SmartAnnotationRequests.Box(); region.setLabelId(labels.get(0).getId()); region.setClassName("车辆"); region.setConfidence(.95);
                if (type != AnnotationTaskType.CLASSIFICATION) { region.setX(120d); region.setY(180d); region.setWidth(260d); region.setHeight(140d); }
                if (type.isMask()) { BitSet foreground = new BitSet(260 * 140); foreground.set(0, 260 * 140); foreground.clear(30 * 260 + 30, 30 * 260 + 60); region.setMaskData(AnnotationMask.encode(foreground, 260, 140)); }
                regions.add(region);
                if (type == AnnotationTaskType.SEMANTIC_SEGMENTATION) {
                    BitSet background = new BitSet(640 * 400); background.set(0, 640 * 400); BitSet foreground = AnnotationMask.decode(region.getMaskData(), 260, 140);
                    for (int p = foreground.nextSetBit(0); p >= 0; p = foreground.nextSetBit(p + 1)) background.clear((180 + p / 260) * 640 + 120 + p % 260);
                    SmartAnnotationRequests.Box back = new SmartAnnotationRequests.Box(); back.setLabelId(labels.get(1).getId()); back.setClassName("背景"); back.setConfidence(.94); back.setX(0d); back.setY(0d); back.setWidth(640d); back.setHeight(400d); back.setMaskData(AnnotationMask.encode(background, 640, 400)); regions.add(back);
                }
                SmartAnnotationCandidate candidate = new SmartAnnotationCandidate(); candidate.setTaskId(task.getId()); candidate.setRoundId(round.getId()); candidate.setImageId(image.getId()); candidate.setImageWidth(640); candidate.setImageHeight(400); candidate.setReviewState("PENDING"); candidate.setUncertainty(.05); candidate.setHardExample(0); candidate.setBoxesJson(data.writeJson(regions)); candidate.setSummaryJson(data.writeJson(AnnotationPayloads.summary(regions))); candidate.setIsDeleted(0); candidate.setStatus(1); sql.getMapper(SmartAnnotationCandidateMapper.class).insert(candidate);
            }
            evidence.add(DataManagementService.map("annotationType", type.getCode(), "taskId", task.getId().toString(), "datasetId", dataset.toString()));
        }
        Files.write(DataTestConfiguration.root().resolve("codex/smart-four/browser-tasks.json"), data.writeJson(evidence).getBytes(StandardCharsets.UTF_8)); TenantContextHolder.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration
    @EnableWebMvc
    public static class WebConfig extends SmartAnnotationIntegrationTest.Config implements WebMvcConfigurer {
        @org.springframework.beans.factory.annotation.Autowired ObjectMapper mapper;
        @Override @Bean DataMediaStorage storage() { return new DataMediaStorage() {
            @Override public String preview(String key) { return "http://127.0.0.1:38082/test-media/" + key; }
        }; }
        @Bean VlsSmartAnnotationController smartController(SmartAnnotationService service) { return new VlsSmartAnnotationController(service, mock(SmartAnnotationWorker.class)); }
        @Bean VlsDatasetAnnotationController annotationController(DatasetAnnotationEditor editor) { return new VlsDatasetAnnotationController(editor); }
        @Bean TestMediaController testMediaController() { return new TestMediaController(); }
        @Bean VlsDataManagementController dataController(DataManagementService data, DataTransferService transfer) { return new VlsDataManagementController(data, transfer); }
        @Override public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) { TenantContextHolder.setTenantId("tenant-a"); return true; }
                public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) { TenantContextHolder.clear(); }
            });
        }
        @Override public void extendMessageConverters(List<HttpMessageConverter<?>> converters) { converters.stream().filter(MappingJackson2HttpMessageConverter.class::isInstance).forEach(converter -> ((MappingJackson2HttpMessageConverter) converter).setObjectMapper(mapper)); }
        @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { registry.addResourceHandler("/test-media/**").addResourceLocations(artifacts().resolve("test-objects").toUri().toString() + "/"); }
    }

    @org.springframework.web.bind.annotation.RestController
    public static class TestMediaController {
        @org.springframework.web.bind.annotation.GetMapping(value = "/test-media/{name:road-[1-3]\\.png}", produces = "image/png")
        public byte[] image(@org.springframework.web.bind.annotation.PathVariable String name) throws IOException {
            return Files.readAllBytes(artifacts().resolve("test-objects").resolve(name));
        }
    }
}
