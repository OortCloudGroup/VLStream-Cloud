package com.ruoyi.vlstream.test.vlstream.cloudai;

import java.util.ArrayList;
import java.util.List;

/** User inputs deliberately exclude executable URLs, filesystem paths and model binaries. */
public final class CloudAiConfiguration {
    public String name;
    public String description;
    public Long nodeId;
    public boolean enabled = true;
    public Integer revision;
    public int preSeconds = 5;
    public int postSeconds = 10;
    public double sampleFps = 2;
    public List<String> cameras = new ArrayList<>();
    public List<ModelRule> models = new ArrayList<>();

    public static class ModelRule {
        public Long modelId;
        public double confidence = 0.5;
        public int reportInterval = 30;
        public List<Integer> classes = new ArrayList<>();
    }
    public static class Task {
        public String sceneId;
        public String name;
        public Integer revision;
        public boolean enabled = true;
        public CloudAiSchedule.Definition schedule;
    }
}
