package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/** Bounded, tenant-scoped request progress. Immutable versions/artifacts remain the durable source of truth. */
@Service
public class DatasetGenerationJobs {
    private final DataManagementService data;
    private final DataTrainingPublisher publisher;
    private final DataMediaStorage media;
    private final TrainingDatasetArtifactService artifacts;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(8), task -> {
        Thread thread = new Thread(task,"dataset-generation-worker"); thread.setDaemon(true); return thread;
    });
    public DatasetGenerationJobs(DataManagementService data, DataTrainingPublisher publisher, DataMediaStorage media, TrainingDatasetArtifactService artifacts) {
        this.data=data; this.publisher=publisher; this.media=media; this.artifacts=artifacts;
    }
    public synchronized DatasetGenerationReport submit(Long datasetId) {
        data.project(datasetId); String tenant = data.tenant();
        for (Job job : jobs.values()) if (job.tenant.equals(tenant) && job.dataset.equals(datasetId) && job.completed == 0 && !job.reviewOnly) return job.report;
        long now = System.currentTimeMillis();
        jobs.values().removeIf(job -> job.completed != 0 && now-job.completed > TimeUnit.HOURS.toMillis(1));
        if (jobs.size() >= 200) jobs.values().stream().filter(job -> job.completed != 0).min(Comparator.comparingLong(job -> job.completed)).ifPresent(job -> jobs.remove(job.id));
        Job job = new Job(tenant,datasetId); jobs.put(job.id,job);
        try { executor.execute(() -> execute(job)); }
        catch (RejectedExecutionException ex) { jobs.remove(job.id); throw new ServiceException("生成队列已满，请稍后重试"); }
        return job.report;
    }

    public synchronized DatasetGenerationReport review(Long datasetId) {
        data.project(datasetId); String tenant = data.tenant();
        Optional<Job> cached = jobs.values().stream().filter(job -> job.tenant.equals(tenant) && job.dataset.equals(datasetId)
            && job.completed != 0 && Arrays.asList("READY", "BLOCKED").contains(job.report.getStatus()))
            .max(Comparator.comparingLong(job -> job.completed));
        if (cached.isPresent()) return get(datasetId, cached.get().id);
        for (Job job : jobs.values()) if (job.reviewOnly && job.tenant.equals(tenant) && job.dataset.equals(datasetId) && job.completed == 0) return job.report;
        TrainingDatasetArtifact saved = artifacts.latestReady(datasetId);
        jobs.values().removeIf(job -> job.completed != 0 && System.currentTimeMillis()-job.completed > TimeUnit.HOURS.toMillis(1));
        if (jobs.size() >= 200) jobs.values().stream().filter(job -> job.completed != 0).min(Comparator.comparingLong(job -> job.completed)).ifPresent(job -> jobs.remove(job.id));
        Job job = new Job(tenant, datasetId); job.reviewOnly = true; job.reference = saved.getReference(); jobs.put(job.id,job);
        try { executor.execute(() -> execute(job)); }
        catch (RejectedExecutionException ex) { jobs.remove(job.id); throw new ServiceException("检查清单读取队列已满，请稍后重试"); }
        return job.report;
    }
    private List<Map<String, Object>> previews(Long datasetId, List<Map<String, Object>> issues) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            Map<String, Object> row = new LinkedHashMap<>(issue);
            try { row.put("previewUrl",media.preview(data.sample(datasetId,Long.valueOf(row.get("imageId").toString())).getLocalPath())); }
            catch (Exception ignored) { row.remove("previewUrl"); }
            result.add(row);
        }
        return result;
    }
    public DatasetGenerationReport get(Long datasetId, String id) {
        data.project(datasetId); Job job = jobs.get(id);
        if (job == null || !job.tenant.equals(data.tenant()) || !job.dataset.equals(datasetId))
            throw new ServiceException("生成进度已过期、服务已重启或无权访问，请刷新数据集查看已生成版本");
        DatasetGenerationReport response = new DatasetGenerationReport();
        org.springframework.beans.BeanUtils.copyProperties(job.report, response);
        response.setCorrections(previews(datasetId, response.getCorrections()));
        response.setErrors(previews(datasetId, response.getErrors()));
        return response;
    }
    private void execute(Job job) {
        try {
            TenantContextHolder.setTenantId(job.tenant);
            DatasetGenerationReport running = new DatasetGenerationReport(); running.setStatus("RUNNING"); running.setJobId(job.id); running.setDatasetId(job.dataset.toString()); job.report=running;
            DatasetGenerationReport result = job.reviewOnly ? artifacts.readGenerationReport(job.dataset, job.reference) : publisher.generate(job.dataset);
            result.setJobId(job.id); result.setOriginalJson(null); result.setPrepared(null); job.report=result;
        } catch (Exception ex) {
            DatasetGenerationReport failed = new DatasetGenerationReport(); failed.setStatus("FAILED"); failed.setJobId(job.id); failed.setDatasetId(job.dataset.toString());
            failed.setMessage(ex instanceof ServiceException ? ex.getMessage() : job.reviewOnly ? "生成检查清单读取失败，请稍后重试；原训练包未改动" : "训练数据包生成失败，请稍后重试；原有已生成版本未覆盖"); job.report=failed;
        } finally { job.completed=System.currentTimeMillis(); TenantContextHolder.clear(); }
    }
    @PreDestroy public void close() { executor.shutdown(); }
    private static class Job {
        final String id=UUID.randomUUID().toString(); final String tenant; final Long dataset;
        volatile DatasetGenerationReport report; volatile long completed;
        boolean reviewOnly; String reference;
        Job(String tenant, Long dataset) {
            this.tenant=tenant; this.dataset=dataset; report=new DatasetGenerationReport(); report.setStatus("QUEUED"); report.setJobId(id); report.setDatasetId(dataset.toString());
        }
    }
}
