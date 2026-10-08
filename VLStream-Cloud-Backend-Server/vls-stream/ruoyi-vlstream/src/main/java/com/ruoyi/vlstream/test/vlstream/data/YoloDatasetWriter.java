package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.*;

/** Encodes the native-pixel rectangles/circles saved by the annotation editor. */
@Component
@RequiredArgsConstructor
public class YoloDatasetWriter {
    private final ObjectMapper json;

    public String labels(List<AnnotationInstance> instances, Map<Long, Integer> classes, int width, int height) {
        if (width <= 0 || height <= 0) throw new ServiceException("图片尺寸无效");
        Set<String> rows = new LinkedHashSet<>();
        for (AnnotationInstance instance : instances) {
            if (!classes.containsKey(instance.getLabelId())) throw new ServiceException("标注引用了缺失的类别");
            try {
                JsonNode shape = json.readTree(instance.getAnnotationData());
                double x, y, w, h;
                if (instance.getAnnotationType() != null && "circle".equals(instance.getAnnotationType().name())) {
                    double r = number(shape, "r");
                    x = number(shape, "cx") - r; y = number(shape, "cy") - r; w = 2 * r; h = 2 * r;
                } else if (instance.getAnnotationType() != null && "rect".equals(instance.getAnnotationType().name())) {
                    x = number(shape, "x"); y = number(shape, "y"); w = number(shape, "width"); h = number(shape, "height");
                } else throw new ServiceException("目标检测训练目前支持矩形和圆形标注，请先转换其他标注类型");
                if (shape.path("normalized").asBoolean(false)) { x *= width; w *= width; y *= height; h *= height; }
                double[] clipped = intersect(x, y, w, h, width, height);
                x = clipped[0]; y = clipped[1]; w = clipped[2]; h = clipped[3];
                rows.add(String.format(Locale.ROOT, "%d %.8f %.8f %.8f %.8f", classes.get(instance.getLabelId()),
                    (x + w / 2) / width, (y + h / 2) / height, w / width, h / height));
            } catch (ServiceException e) { throw e; }
            catch (Exception e) { throw new ServiceException("标注坐标无法解析"); }
        }
        if (rows.isEmpty()) throw new ServiceException("训练样本没有有效标注");
        return String.join("\n", rows) + "\n";
    }

    /** Export uses the visible intersection; the database annotation is never overwritten. */
    public double[] box(AnnotationInstance instance, int width, int height) {
        try {
            JsonNode shape = json.readTree(instance.getAnnotationData());
            double x, y, w, h;
            if (instance.getAnnotationType() != null && "circle".equals(instance.getAnnotationType().name())) {
                double r = number(shape, "r");
                x = number(shape, "cx") - r; y = number(shape, "cy") - r; w = h = 2 * r;
            } else if (instance.getAnnotationType() != null && "rect".equals(instance.getAnnotationType().name())) {
                x = number(shape, "x"); y = number(shape, "y"); w = number(shape, "width"); h = number(shape, "height");
            } else throw new ServiceException("目标检测训练目前支持矩形和圆形标注，请先转换其他标注类型");
            if (shape.path("normalized").asBoolean(false)) { x *= width; w *= width; y *= height; h *= height; }
            return new double[] {x, y, w, h};
        } catch (ServiceException ex) { throw ex; }
        catch (Exception ex) { throw new ServiceException("标注坐标无法解析"); }
    }

    public static double[] intersect(double x, double y, double w, double h, int width, int height) {
        if (width <= 0 || height <= 0 || !Double.isFinite(x) || !Double.isFinite(y)
            || !Double.isFinite(w) || !Double.isFinite(h) || !Double.isFinite(x + w) || !Double.isFinite(y + h)
            || w <= 0 || h <= 0) throw new ServiceException("标注坐标或宽高无效");
        double left = Math.max(0, x), top = Math.max(0, y);
        double right = Math.min(width, x + w), bottom = Math.min(height, y + h);
        if (right <= left || bottom <= top) throw new ServiceException("标注框完全在图片外，请修正后重新生成");
        return new double[] {left, top, right - left, bottom - top};
    }

    public void validateForSave(List<AnnotationInstance> instances, int width, int height) {
        for (AnnotationInstance instance : instances) {
            double[] original = box(instance, width, height);
            double[] clipped = intersect(original[0], original[1], original[2], original[3], width, height);
            for (int i = 0; i < 4; i++) if (Math.abs(original[i] - clipped[i]) > 0.01)
                throw new ServiceException("标注框超出图片边界，请在图片范围内绘制或调整");
        }
    }

    private double number(JsonNode shape, String key) {
        if (shape == null || !shape.has(key) || !shape.get(key).isNumber()) throw new ServiceException("标注缺少数值字段：" + key);
        double value = shape.get(key).asDouble();
        if (!Double.isFinite(value)) throw new ServiceException("标注坐标必须是有限数值");
        return value;
    }
}
