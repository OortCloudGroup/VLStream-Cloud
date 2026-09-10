package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VideoFrameOriginMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class VideoFrameRegistry {
    private final DataManagementService data;
    private final VideoFrameOriginMapper origins;
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> register(DatasetImportJob job, AnnotationImage video, List<PendingSampleImport> files, List<VideoFrameExtractor.Extracted> frames) {
        Map<String,Object> report = data.registerArchiveFromSource(job.getDatasetId(), files, null, "video:" + video.getId());
        List<?> results = (List<?>) report.get("results");
        for (int i=0;i<results.size();i++) {
            Map<String,Object> row = (Map<String,Object>) results.get(i);
            Long sampleId = Long.valueOf(row.get("id").toString());
            long timestamp = frames.get(i).getTimestampMs();
            if (origins.selectCount(new QueryWrapper<VideoFrameOrigin>().eq("tenant_id",data.tenant()).eq("dataset_id",job.getDatasetId())
                .eq("video_id",video.getId()).eq("sample_id",sampleId).eq("timestamp_ms",timestamp)) == 0) {
                VideoFrameOrigin origin = new VideoFrameOrigin(); origin.setTenantId(data.tenant()); origin.setDatasetId(job.getDatasetId());
                origin.setVideoId(video.getId()); origin.setVideoName(video.getImageName()); origin.setSampleId(sampleId);
                origin.setTimestampMs(timestamp); origin.setRequestedMs(frames.get(i).getRequestedMs()); origin.setJobId(job.getId());
                origin.setStatus(1); origin.setIsDeleted(0); origins.insert(origin);
            }
            row.put("videoId",String.valueOf(video.getId())); row.put("timestampMs",timestamp);
        }
        return report;
    }
    public List<VideoFrameOrigin> origins(Long datasetId, Long sampleId) {
        data.sample(datasetId,sampleId);
        return origins.selectList(new QueryWrapper<VideoFrameOrigin>().eq("tenant_id",data.tenant()).eq("dataset_id",datasetId).eq("sample_id",sampleId).orderByAsc("timestamp_ms"));
    }
}
