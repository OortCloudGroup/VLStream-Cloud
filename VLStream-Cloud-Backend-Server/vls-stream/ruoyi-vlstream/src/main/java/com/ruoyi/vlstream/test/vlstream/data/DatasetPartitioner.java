package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import java.util.*;
import java.util.stream.Collectors;

/** Deterministic, disjoint partitions. Identical file content must stay in the same partition. */
public final class DatasetPartitioner {
    private DatasetPartitioner() { }

    public static boolean usable(AnnotationImage sample) {
        return !"video".equals(sample.getMediaType())
            && !"low".equals(sample.getQualityStatus()) && !"excluded".equals(sample.getQualityStatus());
    }

    public static Map<Long, String> partition(List<AnnotationImage> samples, Map<Long, String> classes,
                                             DataRequests.Split request) {
        if (samples.size() < 2) throw new ServiceException("至少需要两张有标注且未标记低质量/剔除的图片");
        if (request.getTrainPercent() < 1 || request.getTrainPercent() > 99)
            throw new ServiceException("训练集比例必须在 1% 到 99% 之间");
        Map<String, List<AnnotationImage>> groups = new TreeMap<>();
        samples.stream().sorted(Comparator.comparing(AnnotationImage::getId)).forEach(sample -> {
            String hash = sample.getContentSha256();
            String key = hash == null || hash.isEmpty() ? "id:" + sample.getId() : "sha:" + hash;
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(sample);
        });
        if (groups.size() < 2) throw new ServiceException("样本内容相同，无法划分互不重复的训练集和验证集");
        Set<Long> validation = new HashSet<>();
        if ("manual".equals(request.getMode())) {
            validation.addAll(request.getValidationIds());
            Set<Long> eligible = samples.stream().map(AnnotationImage::getId).collect(Collectors.toSet());
            if (!eligible.containsAll(validation)) throw new ServiceException("指定的验证样本包含不存在或不符合质量/标注要求的样本");
            for (List<AnnotationImage> group : groups.values()) {
                long selected = group.stream().filter(s -> validation.contains(s.getId())).count();
                if (selected > 0 && selected < group.size()) throw new ServiceException("相同文件内容不能同时分入训练集和验证集");
            }
        } else if ("random".equals(request.getMode()) || "stratified".equals(request.getMode())) {
            Map<String, List<List<AnnotationImage>>> strata = new TreeMap<>();
            for (List<AnnotationImage> group : groups.values()) {
                String label = "stratified".equals(request.getMode()) ? classes.getOrDefault(group.get(0).getId(), "") : "";
                strata.computeIfAbsent(label, ignored -> new ArrayList<>()).add(group);
            }
            Random random = new Random(request.getSeed());
            for (List<List<AnnotationImage>> stratum : strata.values()) {
                Collections.shuffle(stratum, random);
                int count = (int) Math.round(stratum.size() * (100 - request.getTrainPercent()) / 100.0);
                if (stratum.size() > 1) count = Math.max(1, Math.min(stratum.size() - 1, count));
                for (int i = 0; i < count; i++) stratum.get(i).forEach(s -> validation.add(s.getId()));
            }
            if (validation.isEmpty()) groups.values().iterator().next().forEach(s -> validation.add(s.getId()));
            if (validation.size() == samples.size()) groups.values().iterator().next().forEach(s -> validation.remove(s.getId()));
        } else {
            throw new ServiceException("不支持的划分方式");
        }
        if (validation.isEmpty() || validation.size() == samples.size()) throw new ServiceException("训练集和验证集都必须至少包含一张图片");
        Map<Long, String> result = new LinkedHashMap<>();
        samples.forEach(s -> result.put(s.getId(), validation.contains(s.getId()) ? "val" : "train"));
        return result;
    }
}
