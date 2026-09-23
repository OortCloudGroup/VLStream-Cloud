package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.BitSet;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AnnotationMaskTest {
    @Test void preservesHolesAndSeparateComponentsAtNativeResolution() {
        BitSet pixels = new BitSet(100);
        pixels.set(11, 19); pixels.set(81, 89);
        for (int y = 2; y < 8; y++) { pixels.set(y * 10 + 1); pixels.set(y * 10 + 8); }
        pixels.set(0); pixels.set(99);
        BitSet restored = AnnotationMask.decode(AnnotationMask.encode(pixels, 10, 10), 10, 10);
        assertEquals(pixels, restored); assertFalse(restored.get(55)); assertTrue(restored.get(99));
    }

    @Test void rejectsDimensionMismatchAndOversizedImagesBeforeDecode() {
        String png = AnnotationMask.encode(new BitSet(4), 2, 2);
        assertThrows(ServiceException.class, () -> AnnotationMask.decode(png, 3, 3));
        assertThrows(ServiceException.class, () -> AnnotationMask.decode(png, 100_000, 100_000));
        assertThrows(ServiceException.class, () -> AnnotationMask.decode("not-base64", 2, 2));
    }
}
