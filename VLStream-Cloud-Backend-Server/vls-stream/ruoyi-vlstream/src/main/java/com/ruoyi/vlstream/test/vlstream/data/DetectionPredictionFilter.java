package com.ruoyi.vlstream.test.vlstream.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Geometry-only post-processing for machine-generated detection candidates. */
public final class DetectionPredictionFilter {
    private static final double SAME_CLASS_IOU = 0.45;
    private static final double CLASS_CONFLICT_IOU = 0.7;

    private DetectionPredictionFilter() { }

    public static List<SmartAnnotationRequests.Box> suppressDuplicates(List<SmartAnnotationRequests.Box> boxes) {
        List<SmartAnnotationRequests.Box> ordered = new ArrayList<>(boxes);
        ordered.sort(Comparator.comparingDouble(DetectionPredictionFilter::score).reversed());
        List<SmartAnnotationRequests.Box> kept = new ArrayList<>();
        for (SmartAnnotationRequests.Box box : ordered) {
            boolean duplicate = false;
            for (SmartAnnotationRequests.Box existing : kept) {
                if (score(box) >= 0 && score(existing) >= 0 && sameClass(box, existing)
                    && overlap(box, existing) > SAME_CLASS_IOU) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) kept.add(box);
        }
        return kept;
    }

    public static boolean hasClassConflict(List<SmartAnnotationRequests.Box> boxes) {
        for (int first = 0; first < boxes.size(); first++) {
            for (int second = first + 1; second < boxes.size(); second++) {
                SmartAnnotationRequests.Box a = boxes.get(first), b = boxes.get(second);
                if (hasCategory(a) && hasCategory(b) && !sameClass(a, b)
                    && overlap(a, b) > CLASS_CONFLICT_IOU) return true;
            }
        }
        return false;
    }

    private static boolean sameClass(SmartAnnotationRequests.Box first, SmartAnnotationRequests.Box second) {
        if (first.getLabelId() != null || second.getLabelId() != null)
            return Objects.equals(first.getLabelId(), second.getLabelId());
        return first.getClassName() != null && !first.getClassName().isEmpty()
            && Objects.equals(first.getClassName(), second.getClassName());
    }

    private static boolean hasCategory(SmartAnnotationRequests.Box box) {
        return box.getLabelId() != null || (box.getClassName() != null && !box.getClassName().isEmpty());
    }

    private static double score(SmartAnnotationRequests.Box box) {
        Double confidence = box.getConfidence();
        return confidence != null && Double.isFinite(confidence) && confidence >= 0 && confidence <= 1 ? confidence : -1;
    }

    private static boolean validGeometry(SmartAnnotationRequests.Box box) {
        return finite(box.getX()) && finite(box.getY()) && finite(box.getWidth()) && finite(box.getHeight())
            && box.getWidth() > 0 && box.getHeight() > 0;
    }

    private static boolean finite(Double value) {
        return value != null && Double.isFinite(value);
    }

    private static double overlap(SmartAnnotationRequests.Box first, SmartAnnotationRequests.Box second) {
        if (!validGeometry(first) || !validGeometry(second)) return 0;
        double left = Math.max(first.getX(), second.getX());
        double top = Math.max(first.getY(), second.getY());
        double right = Math.min(first.getX() + first.getWidth(), second.getX() + second.getWidth());
        double bottom = Math.min(first.getY() + first.getHeight(), second.getY() + second.getHeight());
        double intersection = Math.max(0, right - left) * Math.max(0, bottom - top);
        double union = first.getWidth() * first.getHeight() + second.getWidth() * second.getHeight() - intersection;
        return union > 0 ? intersection / union : 0;
    }
}
