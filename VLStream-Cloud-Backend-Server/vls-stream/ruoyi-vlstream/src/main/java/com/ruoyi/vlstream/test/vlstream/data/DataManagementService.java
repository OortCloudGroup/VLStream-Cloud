package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** All project/sample/version access is explicitly scoped to the trusted tenant context. */
@Service
@RequiredArgsConstructor
public class DataManagementService {
    static final int MAX_SNAPSHOT_SAMPLES = 10000;
    private final VlsAlgorithmAnnotationMapper projects;
    private final DataSampleMapper samples;
    private final VlsAnnotationLabelMapper labels;
    private final VlsAnnotationInstanceMapper instances;
    private final DatasetVersionMapper versions;
    private final ObjectMapper json;
    private final DataMediaStorage storage;

    public String tenant() {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty()) throw new ServiceException("缺少可信租户上下文，请重新登录");
        return tenant;
    }

    public IPage<AlgorithmAnnotation> projects(String keyword, int page, int size) {
        QueryWrapper<AlgorithmAnnotation> query = new QueryWrapper<AlgorithmAnnotation>().eq("tenant_id", tenant());
        if (hasText(keyword)) query.and(q -> q.like("annotation_name", keyword).or().like("project_code", keyword));
        return projects.selectPage(new Page<>(Math.max(1, page), Math.max(1, Math.min(100, size))), query.orderByDesc("id"));
    }

    public AlgorithmAnnotation project(Long id) {
        AlgorithmAnnotation project = projects.selectOne(new QueryWrapper<AlgorithmAnnotation>().eq("id", id).eq("tenant_id", tenant()));
        if (project == null) throw new ServiceException("数据集不存在或无权访问");
        return project;
    }

    private AlgorithmAnnotation lock(Long id) {
        AlgorithmAnnotation project = versions.lockProject(id, tenant());
        if (project == null) throw new ServiceException("数据集不存在或无权访问");
        return project;
    }

    @Transactional(rollbackFor = Exception.class)
    public void beginAnnotationEdit(Long projectId) {
        lock(projectId);
        invalidate(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void lockDatasetForTask(Long datasetId) {
        lock(datasetId);
    }

    public void validateAnnotationOwner(Long projectId, Long sampleId, Collection<Long> labelIds) {
        sample(projectId, sampleId);
        Set<Long> unique = new HashSet<>(labelIds);
        if (unique.contains(null) || (!unique.isEmpty() && labels.selectCount(this.<AnnotationLabel>scope(projectId).in("id", unique)) != unique.size()))
            throw new ServiceException("标注类别不属于当前数据集");
    }

    @Transactional(rollbackFor = Exception.class)
    public AlgorithmAnnotation saveProject(Long id, DataRequests.Project request) {
        if (!Arrays.asList("object_detection", "image_classification", "instance_segmentation", "semantic_segmentation").contains(request.getAnnotationType()))
            throw new ServiceException("不支持的标注类型");
        AlgorithmAnnotation project = id == null ? new AlgorithmAnnotation() : lock(id);
        QueryWrapper<AlgorithmAnnotation> duplicate = new QueryWrapper<AlgorithmAnnotation>().eq("tenant_id", tenant())
            .eq("project_code", request.getProjectCode().trim()).ne(id != null, "id", id);
        if (projects.selectCount(duplicate) > 0) throw new ServiceException("数据集编号已存在");
        if (id != null && !Objects.equals(project.getAnnotationType(), request.getAnnotationType())
            && instances.selectCount(scope(id)) > 0) throw new ServiceException("已有标注的数据集不能直接更换标注类型");
        project.setAnnotationName(request.getAnnotationName().trim());
        project.setProjectCode(request.getProjectCode().trim());
        project.setProjectType(request.getProjectType().trim());
        project.setAnnotationType(request.getAnnotationType());
        project.setAnnotationRules(request.getAnnotationRules());
        project.setRemark(request.getRemark());
        project.setTenantId(tenant());
        if (id == null) {
            project.setTotalCount(0); project.setAnnotatedCount(0); project.setProgress(0);
            project.setAnnotationStatus(AlgorithmAnnotationStatusEnum.none);
            project.setIsDeleted(0); project.setStatus(1);
            projects.insert(project);
        } else {
            projects.updateById(project);
            invalidate(id);
        }
        return project;
    }

    public IPage<Map<String, Object>> samples(Long projectId, DataRequests.SampleQuery request) {
        project(projectId);
        IPage<AnnotationImage> page = samples.selectPage(new Page<>(request.getPage(), request.getSize()), sampleQuery(projectId, request).orderByDesc("id"));
        List<AnnotationInstance> annotations = instances.selectList(scope(projectId));
        List<AnnotationLabel> projectLabels = labels.selectList(scope(projectId));
        IPage<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(s -> sampleView(s, annotations, projectLabels)).collect(Collectors.toList()));
        return result;
    }

    private QueryWrapper<AnnotationImage> sampleQuery(Long projectId, DataRequests.SampleQuery request) {
        QueryWrapper<AnnotationImage> query = scope(projectId);
        query.like(hasText(request.getKeyword()), "image_name", request.getKeyword());
        query.eq(hasText(request.getSource()), "sample_source", request.getSource());
        query.eq(hasText(request.getMediaType()), "media_type", request.getMediaType());
        query.eq(hasText(request.getQualityStatus()), "quality_status", request.getQualityStatus());
        query.eq(hasText(request.getDatasetSplit()), "dataset_split", request.getDatasetSplit());
        String annotated = "EXISTS (SELECT 1 FROM vls_annotation_instance ai WHERE ai.image_id = vls_annotation_image.id "
            + "AND ai.annotation_id = vls_annotation_image.annotation_id AND ai.tenant_id = {0} AND ai.is_deleted = 0)";
        if ("annotated".equals(request.getAnnotationStatus())) query.apply(annotated, tenant());
        if ("unannotated".equals(request.getAnnotationStatus())) query.apply("NOT " + annotated, tenant());
        if (hasText(request.getTag())) query.apply("(JSON_CONTAINS(COALESCE(sample_tags, '[]'), JSON_QUOTE({0})) OR EXISTS "
            + "(SELECT 1 FROM vls_annotation_instance ai JOIN vls_annotation_label al ON al.id = ai.label_id "
            + "AND al.tenant_id = ai.tenant_id AND al.is_deleted = 0 WHERE ai.image_id = vls_annotation_image.id "
            + "AND ai.annotation_id = vls_annotation_image.annotation_id AND ai.tenant_id = {1} AND ai.is_deleted = 0 AND al.name = {0}))",
            request.getTag(), tenant());
        return query;
    }

    public AnnotationImage sample(Long projectId, Long id) {
        project(projectId);
        AnnotationImage sample = samples.selectOne(this.<AnnotationImage>scope(projectId).eq("id", id));
        if (sample == null) throw new ServiceException("样本不存在或不属于当前数据集");
        return sample;
    }

    public Map<String, Object> detail(Long projectId, Long id) {
        return sampleView(sample(projectId, id), instances.selectList(scope(projectId)), labels.selectList(scope(projectId)));
    }

    private Map<String, Object> sampleView(AnnotationImage sample, List<AnnotationInstance> annotations, List<AnnotationLabel> projectLabels) {
        Map<String, Object> result = json.convertValue(sample, new TypeReference<Map<String, Object>>() { });
        result.put("id", String.valueOf(sample.getId()));
        result.remove("localPath");
        result.put("tags", tags(sample));
        List<AnnotationInstance> own = annotations.stream().filter(a -> Objects.equals(a.getImageId(), sample.getId())).collect(Collectors.toList());
        Set<Long> labelIds = own.stream().map(AnnotationInstance::getLabelId).collect(Collectors.toSet());
        result.put("labels", projectLabels.stream().filter(l -> labelIds.contains(l.getId())).map(AnnotationLabel::getName).distinct().collect(Collectors.toList()));
        result.put("annotationCount", own.size());
        result.put("annotationStatus", own.isEmpty() ? "unannotated" : "annotated");
        result.put("instances", own);
        try {
            result.put("previewUrl", storage.preview(sample.getLocalPath()));
        } catch (RuntimeException e) {
            result.put("previewError", "暂时无法访问对象存储，请检查存储配置后重试");
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void editSample(Long projectId, Long id, DataRequests.SampleEdit request) {
        lock(projectId);
        AnnotationImage sample = sample(projectId, id);
        String name = request.getImageName().trim();
        if (name.contains("/") || name.contains("\\") || name.contains("..")) throw new ServiceException("样本名称不能包含路径");
        sample.setImageName(name);
        sample.setSampleSource(request.getSampleSource().trim());
        sample.setSampleTags(writeJson(request.getTags().stream().map(String::trim).distinct().collect(Collectors.toList())));
        samples.updateById(sample);
        invalidate(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(Long projectId, String filename, String key, long size, String source, SampleMediaInspector.Inspection inspection) {
        lock(projectId);
        List<AnnotationImage> duplicate = samples.selectList(this.<AnnotationImage>scope(projectId).eq("content_sha256", inspection.getSha256()).last("LIMIT 1"));
        if (!duplicate.isEmpty()) return map("id", String.valueOf(duplicate.get(0).getId()), "duplicate", true);
        if (samples.selectCount(scope(projectId)) >= MAX_SNAPSHOT_SAMPLES) throw new ServiceException("单数据集最多支持 10000 个样本，请另建数据集");
        AnnotationImage sample = newImportedSample(projectId, filename, key, size, source, inspection);
        samples.insert(sample);
        invalidate(projectId); refreshProgress(projectId);
        return map("id", String.valueOf(sample.getId()), "duplicate", false);
    }

    private AnnotationImage newImportedSample(Long projectId, String filename, String key, long size, String source, SampleMediaInspector.Inspection inspection) {
        AnnotationImage sample = new AnnotationImage();
        sample.setTenantId(tenant()); sample.setAnnotationId(projectId); sample.setImageName(filename); sample.setOriginalName(filename);
        sample.setLocalPath(key); sample.setFileSize(size); sample.setIsImported(1); sample.setImportTime(new Date());
        sample.setMediaType(inspection.getMediaType()); sample.setSampleSource(source); sample.setSampleTags("[]");
        sample.setQualityStatus("image".equals(inspection.getMediaType()) && hasText(inspection.getIssues()) ? "low" : "pending");
        sample.setQualityNote(""); sample.setQualityReviewedBy(""); sample.setDatasetSplit("unassigned");
        applyInspection(sample, inspection);
        sample.setIsDeleted(0); sample.setStatus(1);
        return sample;
    }

    @Transactional(rollbackFor = Exception.class)
    public void batch(Long projectId, DataRequests.Batch request) {
        lock(projectId);
        if (!Arrays.asList("accepted", "low", "excluded", "pending", "delete").contains(request.getAction())) throw new ServiceException("不支持的样本操作");
        List<AnnotationImage> selected = requireSelection(projectId, request.getIds());
        for (AnnotationImage sample : selected) {
            if ("delete".equals(request.getAction())) {
                samples.deleteById(sample.getId());
                instances.delete(this.<AnnotationInstance>scope(projectId).eq("image_id", sample.getId()));
            } else {
                sample.setQualityStatus(request.getAction()); sample.setQualityNote(request.getNote());
                sample.setQualityReviewedBy(LoginHelper.getUsername());
                if (Arrays.asList("low", "excluded").contains(request.getAction())) sample.setDatasetSplit("unassigned");
                samples.updateById(sample);
            }
        }
        invalidate(projectId); refreshProgress(projectId);
    }

    public List<AnnotationImage> requireSelection(Long projectId, List<Long> ids) {
        project(projectId);
        if (ids == null || ids.isEmpty() || ids.size() > 500) throw new ServiceException("每次请选择 1 至 500 个样本");
        Set<Long> unique = new HashSet<>(ids);
        List<AnnotationImage> selected = samples.selectList(this.<AnnotationImage>scope(projectId).in("id", unique));
        if (selected.size() != unique.size()) throw new ServiceException("所选样本包含不存在或不属于当前数据集的记录");
        return selected;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerArchive(Long projectId, List<PendingSampleImport> files, DatasetSnapshot manifest) {
        return registerArchiveFromSource(projectId, files, manifest, "archive");
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> registerArchiveFromSource(Long projectId, List<PendingSampleImport> files, DatasetSnapshot manifest, String sourceName) {
        lock(projectId);
        Map<String, Map<String, Object>> imported = new LinkedHashMap<>();
        List<AnnotationImage> existing = samples.selectList(this.<AnnotationImage>scope(projectId).select("id", "content_sha256"));
        Map<String, Long> byHash = new HashMap<>();
        existing.forEach(sample -> { if (hasText(sample.getContentSha256())) byHash.put(sample.getContentSha256(), sample.getId()); });
        int sampleCount = existing.size();
        for (PendingSampleImport file : files) {
            String hash = file.getInspection().getSha256();
            Long duplicateId = byHash.get(hash);
            Map<String, Object> result;
            if (duplicateId != null) result = map("id", String.valueOf(duplicateId), "duplicate", true);
            else {
                if (++sampleCount > MAX_SNAPSHOT_SAMPLES) throw new ServiceException("单数据集最多支持 10000 个样本，请拆分数据集");
                AnnotationImage sample = newImportedSample(projectId, file.getFilename(), file.getObjectKey(), file.getSize(), sourceName, file.getInspection());
                samples.insert(sample); byHash.put(hash, sample.getId());
                result = map("id", String.valueOf(sample.getId()), "duplicate", false);
            }
            result.put("filename", file.getFilename()); result.put("success", true);
            imported.put(file.getArchivePath(), result);
        }
        if (manifest != null) {
            Map<Long, Long> ids = new HashMap<>();
            for (AnnotationImage sample : manifest.getSamples()) {
                Map<String, Object> result = imported.get(sample.getLocalPath());
                if (!Boolean.TRUE.equals(result.get("duplicate"))) ids.put(sample.getId(), Long.valueOf(result.get("id").toString()));
            }
            importMetadata(projectId, manifest, ids);
        } else { invalidate(projectId); refreshProgress(projectId); }
        return map("results", new ArrayList<>(imported.values()), "successCount", imported.size());
    }

    @Transactional(rollbackFor = Exception.class)
    public void importMetadata(Long projectId, DatasetSnapshot manifest, Map<Long, Long> importedIds) {
        lock(projectId);
        List<AnnotationLabel> existingLabels = labels.selectList(scope(projectId));
        Map<Long, Long> labelIds = new HashMap<>();
        for (AnnotationLabel source : manifest.getLabels()) {
            AnnotationLabel target = existingLabels.stream().filter(l -> l.getName().equals(source.getName())).findFirst().orElse(null);
            if (target == null) {
                target = new AnnotationLabel(); target.setAnnotationId(projectId); target.setTenantId(tenant());
                target.setName(source.getName()); target.setColor(source.getColor()); target.setDescription(source.getDescription());
                target.setSortOrder(source.getSortOrder()); target.setUsageCount(0); target.setIsDeleted(0); target.setStatus(1);
                labels.insert(target); existingLabels.add(target);
            }
            labelIds.put(source.getId(), target.getId());
        }
        for (AnnotationImage source : manifest.getSamples()) {
            Long targetId = importedIds.get(source.getId());
            if (targetId == null) continue;
            AnnotationImage target = sample(projectId, targetId);
            target.setImageName(source.getImageName());
            target.setSampleTags(writeJson(tags(source)));
            target.setSampleSource(hasText(source.getSampleSource()) ? source.getSampleSource() : "archive");
            // Quality and partition are re-evaluated locally after importing.
            samples.updateById(target);
        }
        for (AnnotationInstance source : manifest.getInstances()) {
            Long targetId = importedIds.get(source.getImageId());
            if (targetId == null) continue;
            Long labelId = labelIds.get(source.getLabelId());
            if (labelId == null) throw new ServiceException("导入标注引用了不存在的类别");
            AnnotationInstance target = new AnnotationInstance(); target.setAnnotationId(projectId); target.setTenantId(tenant());
            target.setImageId(targetId); target.setLabelId(labelId); target.setAnnotationType(source.getAnnotationType());
            target.setAnnotationData(source.getAnnotationData()); target.setConfidence(source.getConfidence()); target.setVerified(0);
            target.setIsDeleted(0); target.setStatus(1); instances.insert(target);
        }
        invalidate(projectId); refreshProgress(projectId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordPublishedDataset(Long projectId, Long versionId, String path) {
        lock(projectId);
        version(projectId, versionId);
        // Do not make a stale, concurrently changed working set look ready for training.
        Map<String, Object> differences = compare(projectId, versionId, null);
        boolean changed = !((List<?>) differences.get("added")).isEmpty() || !((List<?>) differences.get("removed")).isEmpty()
            || !((List<?>) differences.get("changed")).isEmpty() || Boolean.TRUE.equals(differences.get("configurationChanged"))
            || Boolean.TRUE.equals(differences.get("labelsChanged"));
        if (changed) throw new ServiceException("生成期间样本或标注发生变化，版本文件已保留，请重新生成");
        projects.update(null, new UpdateWrapper<AlgorithmAnnotation>().eq("id", projectId).eq("tenant_id", tenant()).set("dataset_path", path));
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveInspection(Long projectId, Long id, SampleMediaInspector.Inspection inspection, String failure) {
        lock(projectId);
        AnnotationImage sample = sample(projectId, id);
        if (inspection != null) applyInspection(sample, inspection);
        else { sample.setQualityIssues(failure); sample.setQualityCheckedAt(new Date()); }
        if (!"excluded".equals(sample.getQualityStatus()) && (inspection == null
            || ("image".equals(sample.getMediaType()) && hasText(inspection.getIssues())))) {
            sample.setQualityStatus("low"); sample.setDatasetSplit("unassigned");
        }
        samples.updateById(sample);
        invalidate(projectId);
    }

    private void applyInspection(AnnotationImage sample, SampleMediaInspector.Inspection inspection) {
        sample.setContentSha256(inspection.getSha256()); sample.setMediaWidth(inspection.getWidth());
        sample.setMediaHeight(inspection.getHeight()); sample.setFocusScore(inspection.getFocusScore());
        sample.setQualityIssues(inspection.getIssues()); sample.setQualityCheckedAt(new Date());
    }

    public List<Map<String, Object>> review(Long projectId, int count) {
        project(projectId);
        List<AnnotationImage> selected = samples.selectList(this.<AnnotationImage>scope(projectId).ne("quality_status", "excluded").last("ORDER BY RAND() LIMIT " + Math.max(1, Math.min(50, count))));
        List<AnnotationInstance> annotations = instances.selectList(scope(projectId));
        List<AnnotationLabel> projectLabels = labels.selectList(scope(projectId));
        return selected.stream().map(s -> sampleView(s, annotations, projectLabels)).collect(Collectors.toList());
    }

    public Map<String, Object> statistics(Long projectId) {
        return statistics(snapshot(projectId));
    }

    public Map<String, Object> statistics(DatasetSnapshot snapshot) {
        Map<Long, AnnotationImage> byId = snapshot.getSamples().stream().collect(Collectors.toMap(AnnotationImage::getId, Function.identity()));
        Set<Long> annotated = snapshot.getInstances().stream().map(AnnotationInstance::getImageId).collect(Collectors.toSet());
        List<Map<String, Object>> distribution = new ArrayList<>();
        for (AnnotationLabel label : snapshot.getLabels()) {
            Set<Long> ids = snapshot.getInstances().stream().filter(i -> Objects.equals(label.getId(), i.getLabelId()))
                .map(AnnotationInstance::getImageId).filter(byId::containsKey).collect(Collectors.toSet());
            distribution.add(map("name", label.getName(), "total", ids.size(),
                "train", ids.stream().filter(id -> "train".equals(byId.get(id).getDatasetSplit())).count(),
                "val", ids.stream().filter(id -> "val".equals(byId.get(id).getDatasetSplit())).count()));
        }
        return map("total", byId.size(), "images", snapshot.getSamples().stream().filter(s -> !"video".equals(s.getMediaType())).count(),
            "videos", snapshot.getSamples().stream().filter(s -> "video".equals(s.getMediaType())).count(),
            "annotated", annotated.stream().filter(byId::containsKey).count(),
            "eligible", snapshot.getSamples().stream().filter(DatasetPartitioner::usable).filter(s -> annotated.contains(s.getId())).count(),
            "quality", snapshot.getSamples().stream().collect(Collectors.groupingBy(s -> Objects.toString(s.getQualityStatus(), "pending"), TreeMap::new, Collectors.counting())),
            "splits", snapshot.getSamples().stream().collect(Collectors.groupingBy(s -> Objects.toString(s.getDatasetSplit(), "unassigned"), TreeMap::new, Collectors.counting())),
            "sources", snapshot.getSamples().stream().map(AnnotationImage::getSampleSource).filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList()),
            "distribution", distribution);
    }

    @Transactional(rollbackFor = Exception.class)
    public DatasetVersion split(Long projectId, DataRequests.Split request) {
        lock(projectId);
        DatasetSnapshot snapshot = snapshot(projectId);
        Map<Long, String> classes = new HashMap<>();
        Map<Long, String> labelNames = snapshot.getLabels().stream().collect(Collectors.toMap(AnnotationLabel::getId, AnnotationLabel::getName));
        snapshot.getInstances().stream().sorted(Comparator.comparing(i -> labelNames.getOrDefault(i.getLabelId(), "")))
            .forEach(i -> classes.putIfAbsent(i.getImageId(), labelNames.getOrDefault(i.getLabelId(), "")));
        List<AnnotationImage> eligible = snapshot.getSamples().stream().filter(DatasetPartitioner::usable)
            .filter(s -> classes.containsKey(s.getId())).collect(Collectors.toList());
        Map<Long, String> partition = DatasetPartitioner.partition(eligible, classes, request);
        // Preserve the state before replacing an existing partition.
        if (snapshot.getSamples().stream().anyMatch(s -> Arrays.asList("train", "val").contains(s.getDatasetSplit())))
            saveVersion(projectId, "划分前自动备份", "重新划分前保留当前数据集", snapshot);
        for (AnnotationImage sample : snapshot.getSamples()) {
            sample.setDatasetSplit(partition.getOrDefault(sample.getId(), "unassigned"));
            samples.updateById(sample);
        }
        invalidate(projectId);
        return saveVersion(projectId, request.getVersionName(), "mode=" + request.getMode() + ", train=" + request.getTrainPercent() + "%, seed=" + request.getSeed(), snapshot);
    }

    @Transactional(rollbackFor = Exception.class)
    public DatasetVersion saveVersion(Long projectId, DataRequests.Version request) {
        lock(projectId);
        return saveVersion(projectId, request.getName(), request.getDescription(), snapshot(projectId));
    }

    private DatasetVersion saveVersion(Long projectId, String name, String description, DatasetSnapshot snapshot) {
        DatasetVersion latest = versions.selectOne(this.<DatasetVersion>scope(projectId).orderByDesc("version_number").last("LIMIT 1"));
        DatasetVersion version = new DatasetVersion();
        version.setAnnotationId(projectId); version.setTenantId(tenant());
        version.setVersionNumber(latest == null ? 1 : latest.getVersionNumber() + 1);
        version.setVersionName(name); version.setDescription(description == null ? "" : description);
        version.setSnapshotJson(writeJson(snapshot)); version.setSampleCount(snapshot.getSamples().size());
        version.setTrainCount((int) snapshot.getSamples().stream().filter(s -> "train".equals(s.getDatasetSplit())).count());
        version.setValidationCount((int) snapshot.getSamples().stream().filter(s -> "val".equals(s.getDatasetSplit())).count());
        version.setIsDeleted(0); version.setStatus(1);
        versions.insert(version);
        return version;
    }

    public List<DatasetVersion> versions(Long projectId) {
        project(projectId);
        return versions.selectList(this.<DatasetVersion>scope(projectId).select("id", "tenant_id", "annotation_id", "version_number", "version_name",
            "description", "sample_count", "train_count", "validation_count", "create_time", "create_user").orderByDesc("version_number"));
    }

    public DatasetVersion version(Long projectId, Long versionId) {
        project(projectId);
        DatasetVersion version = versions.selectOne(this.<DatasetVersion>scope(projectId).eq("id", versionId));
        if (version == null) throw new ServiceException("数据集版本不存在或不属于当前数据集");
        return version;
    }

    public Map<String, Object> versionDetail(Long projectId, Long versionId) {
        DatasetVersion version = version(projectId, versionId);
        DatasetSnapshot snapshot = readSnapshot(version.getSnapshotJson());
        List<Map<String, Object>> members = snapshot.getSamples().stream().map(s -> map("id", String.valueOf(s.getId()), "name", s.getImageName(),
            "mediaType", s.getMediaType(), "split", s.getDatasetSplit(), "quality", s.getQualityStatus(), "tags", tags(s))).collect(Collectors.toList());
        return map("version", version, "statistics", statistics(snapshot), "samples", members,
            "annotationType", snapshot.getAnnotationType(), "annotationRules", snapshot.getAnnotationRules());
    }

    public Map<String, Object> compare(Long projectId, Long from, Long to) {
        DatasetSnapshot before = readSnapshot(version(projectId, from).getSnapshotJson());
        DatasetSnapshot after = to == null ? snapshot(projectId) : readSnapshot(version(projectId, to).getSnapshotJson());
        Map<Long, AnnotationImage> left = before.getSamples().stream().collect(Collectors.toMap(AnnotationImage::getId, Function.identity()));
        Map<Long, AnnotationImage> right = after.getSamples().stream().collect(Collectors.toMap(AnnotationImage::getId, Function.identity()));
        List<Map<String, Object>> added = new ArrayList<>(), removed = new ArrayList<>(), changed = new ArrayList<>();
        for (AnnotationImage sample : after.getSamples()) {
            if (!left.containsKey(sample.getId())) added.add(map("id", String.valueOf(sample.getId()), "name", sample.getImageName()));
            else {
                List<String> fields = changedFields(left.get(sample.getId()), sample, before, after);
                if (!fields.isEmpty()) changed.add(map("id", String.valueOf(sample.getId()), "name", sample.getImageName(), "fields", fields,
                    "beforeSplit", left.get(sample.getId()).getDatasetSplit(), "afterSplit", sample.getDatasetSplit()));
            }
        }
        before.getSamples().stream().filter(s -> !right.containsKey(s.getId())).forEach(s -> removed.add(map("id", String.valueOf(s.getId()), "name", s.getImageName())));
        return map("added", added, "removed", removed, "changed", changed,
            "configurationChanged", !Objects.equals(before.getAnnotationRules(), after.getAnnotationRules()) || !Objects.equals(before.getAnnotationType(), after.getAnnotationType()),
            "labelsChanged", !semanticLabels(before).equals(semanticLabels(after)), "before", statistics(before), "after", statistics(after));
    }

    private List<String> changedFields(AnnotationImage before, AnnotationImage after, DatasetSnapshot left, DatasetSnapshot right) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(before.getImageName(), after.getImageName())) changes.add("名称");
        if (!Objects.equals(before.getSampleSource(), after.getSampleSource())) changes.add("来源");
        if (!tags(before).equals(tags(after))) changes.add("标签");
        if (!Objects.equals(before.getQualityStatus(), after.getQualityStatus()) || !Objects.equals(before.getQualityNote(), after.getQualityNote())
            || !Objects.equals(before.getQualityIssues(), after.getQualityIssues())) changes.add("质量");
        if (!Objects.equals(before.getDatasetSplit(), after.getDatasetSplit())) changes.add("数据集划分");
        if (!Objects.equals(before.getContentSha256(), after.getContentSha256()) || !Objects.equals(before.getLocalPath(), after.getLocalPath())) changes.add("文件");
        if (!semanticInstances(left, before.getId()).equals(semanticInstances(right, after.getId()))) changes.add("标注内容");
        return changes;
    }

    private List<String> semanticInstances(DatasetSnapshot snapshot, Long id) {
        return snapshot.getInstances().stream().filter(i -> Objects.equals(id, i.getImageId()))
            .map(i -> writeJson(Arrays.asList(i.getLabelId(), i.getAnnotationType(), i.getAnnotationData(), i.getVerified(), i.getConfidence())))
            .sorted().collect(Collectors.toList());
    }

    private List<String> semanticLabels(DatasetSnapshot snapshot) {
        return snapshot.getLabels().stream().map(l -> writeJson(Arrays.asList(l.getId(), l.getName(), l.getColor(), l.getDescription(), l.getSortOrder())))
            .sorted().collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public DatasetVersion restore(Long projectId, Long versionId) {
        lock(projectId);
        DatasetVersion target = version(projectId, versionId);
        DatasetSnapshot snapshot = readSnapshot(target.getSnapshotJson());
        saveVersion(projectId, "回退前自动备份", "回退至 V" + target.getVersionNumber() + " 前保留当前状态", snapshot(projectId));
        samples.delete(scope(projectId)); labels.delete(scope(projectId)); instances.delete(scope(projectId));
        for (AnnotationImage sample : snapshot.getSamples()) {
            sample.setTenantId(tenant()); sample.setAnnotationId(projectId); sample.setIsDeleted(0);
            if (samples.revive(sample.getId(), projectId, tenant()) == 0) samples.insert(sample);
            else restoreSample(projectId, sample);
        }
        for (AnnotationLabel label : snapshot.getLabels()) {
            label.setTenantId(tenant()); label.setAnnotationId(projectId); label.setIsDeleted(0);
            if (versions.reviveLabel(label.getId(), projectId, tenant()) == 0) labels.insert(label);
            else labels.update(null, new UpdateWrapper<AnnotationLabel>().eq("id", label.getId()).eq("tenant_id", tenant()).eq("annotation_id", projectId)
                .set("name", label.getName()).set("color", label.getColor()).set("description", label.getDescription())
                .set("sort_order", label.getSortOrder()).set("usage_count", label.getUsageCount()).set("update_time", new Date()));
        }
        for (AnnotationInstance instance : snapshot.getInstances()) {
            instance.setTenantId(tenant()); instance.setAnnotationId(projectId); instance.setIsDeleted(0);
            if (versions.reviveInstance(instance.getId(), projectId, tenant()) == 0) instances.insert(instance);
            else instances.update(null, new UpdateWrapper<AnnotationInstance>().eq("id", instance.getId()).eq("tenant_id", tenant()).eq("annotation_id", projectId)
                .set("label_id", instance.getLabelId()).set("image_id", instance.getImageId()).set("annotation_type", instance.getAnnotationType())
                .set("annotation_data", instance.getAnnotationData()).set("confidence", instance.getConfidence()).set("verified", instance.getVerified()).set("update_time", new Date()));
        }
        projects.update(null, new UpdateWrapper<AlgorithmAnnotation>().eq("id", projectId).eq("tenant_id", tenant())
            .set("annotation_type", snapshot.getAnnotationType()).set("annotation_rules", snapshot.getAnnotationRules()));
        invalidate(projectId); refreshProgress(projectId);
        return saveVersion(projectId, "回退至 V" + target.getVersionNumber(), "恢复自：" + target.getVersionName(), snapshot(projectId));
    }

    private void restoreSample(Long projectId, AnnotationImage sample) {
        // Set nullable fields explicitly; generated updateById otherwise preserves values from the newer version.
        samples.update(null, new UpdateWrapper<AnnotationImage>().eq("id", sample.getId()).eq("tenant_id", tenant()).eq("annotation_id", projectId)
            .set("image_name", sample.getImageName()).set("original_name", sample.getOriginalName()).set("local_path", sample.getLocalPath())
            .set("file_size", sample.getFileSize()).set("last_modified", sample.getLastModified()).set("is_imported", sample.getIsImported()).set("import_time", sample.getImportTime())
            .set("media_type", sample.getMediaType()).set("sample_source", sample.getSampleSource()).set("sample_tags", sample.getSampleTags())
            .set("quality_status", sample.getQualityStatus()).set("quality_note", sample.getQualityNote()).set("quality_issues", sample.getQualityIssues())
            .set("quality_checked_at", sample.getQualityCheckedAt()).set("quality_reviewed_by", sample.getQualityReviewedBy())
            .set("media_width", sample.getMediaWidth()).set("media_height", sample.getMediaHeight()).set("focus_score", sample.getFocusScore())
            .set("content_sha256", sample.getContentSha256()).set("dataset_split", sample.getDatasetSplit()).set("update_time", new Date()));
    }

    public DatasetSnapshot snapshot(Long projectId) {
        AlgorithmAnnotation project = project(projectId);
        List<AnnotationImage> members = samples.selectList(this.<AnnotationImage>scope(projectId).orderByAsc("id").last("LIMIT " + (MAX_SNAPSHOT_SAMPLES + 1)));
        if (members.size() > MAX_SNAPSHOT_SAMPLES) throw new ServiceException("单数据集版本最多支持 10000 个样本，请按业务拆分数据集");
        DatasetSnapshot snapshot = new DatasetSnapshot();
        snapshot.setAnnotationType(project.getAnnotationType()); snapshot.setAnnotationRules(project.getAnnotationRules()); snapshot.setSamples(members);
        snapshot.setLabels(labels.selectList(this.<AnnotationLabel>scope(projectId).orderByAsc("id")));
        Set<Long> ids = members.stream().map(AnnotationImage::getId).collect(Collectors.toSet());
        snapshot.setInstances(instances.selectList(this.<AnnotationInstance>scope(projectId).orderByAsc("id")).stream()
            .filter(i -> ids.contains(i.getImageId())).collect(Collectors.toList()));
        return snapshot;
    }

    public DatasetSnapshot exportSnapshot(Long projectId, DataRequests.SampleQuery request, List<Long> ids) {
        DatasetSnapshot snapshot = snapshot(projectId);
        Set<Long> selected = ids == null || ids.isEmpty() ? samples.selectList(sampleQuery(projectId, request).select("id")).stream()
            .map(AnnotationImage::getId).collect(Collectors.toSet()) : requireSelection(projectId, ids).stream().map(AnnotationImage::getId).collect(Collectors.toSet());
        snapshot.setSamples(snapshot.getSamples().stream().filter(s -> selected.contains(s.getId())).collect(Collectors.toList()));
        snapshot.setInstances(snapshot.getInstances().stream().filter(i -> selected.contains(i.getImageId())).collect(Collectors.toList()));
        if (snapshot.getSamples().isEmpty()) throw new ServiceException("没有可导出的样本");
        return snapshot;
    }

    private void invalidate(Long projectId) {
        projects.update(null, new UpdateWrapper<AlgorithmAnnotation>().eq("id", projectId).eq("tenant_id", tenant()).set("dataset_path", null));
    }

    private void refreshProgress(Long projectId) {
        List<AnnotationImage> active = samples.selectList(this.<AnnotationImage>scope(projectId).ne("media_type", "video").ne("quality_status", "excluded"));
        Set<Long> ids = active.stream().map(AnnotationImage::getId).collect(Collectors.toSet());
        int annotated = (int) instances.selectList(scope(projectId)).stream().map(AnnotationInstance::getImageId).filter(ids::contains).distinct().count();
        int progress = active.isEmpty() ? 0 : annotated * 100 / active.size();
        projects.update(null, new UpdateWrapper<AlgorithmAnnotation>().eq("id", projectId).eq("tenant_id", tenant())
            .set("total_count", active.size()).set("annotated_count", annotated).set("progress", progress)
            .set("annotation_status", progress == 100 ? "completed" : progress == 0 ? "none" : "partial"));
    }

    private <T> QueryWrapper<T> scope(Long projectId) {
        return new QueryWrapper<T>().eq("tenant_id", tenant()).eq("annotation_id", projectId);
    }

    List<String> tags(AnnotationImage sample) {
        if (!hasText(sample.getSampleTags())) return Collections.emptyList();
        try { return json.readValue(sample.getSampleTags(), new TypeReference<List<String>>() { }); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    public String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception e) { throw new ServiceException("数据序列化失败"); }
    }

    public DatasetSnapshot readSnapshot(String value) {
        try {
            DatasetSnapshot snapshot = json.readValue(value, DatasetSnapshot.class);
            if (snapshot.getSchemaVersion() != 1 || snapshot.getSamples() == null || snapshot.getLabels() == null || snapshot.getInstances() == null)
                throw new IllegalArgumentException();
            return snapshot;
        } catch (Exception e) { throw new ServiceException("数据集快照格式无效"); }
    }

    static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }

    public static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put(pairs[i].toString(), pairs[i + 1]);
        return result;
    }
}
