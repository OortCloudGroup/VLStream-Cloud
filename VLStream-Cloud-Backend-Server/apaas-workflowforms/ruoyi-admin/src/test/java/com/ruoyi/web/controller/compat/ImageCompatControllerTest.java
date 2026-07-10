package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.compat.BladeResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("dev")
class ImageCompatControllerTest {

    @Test
    void deleteReturnsBladeSuccessForFrontendImageDeletion() {
        ImageCompatController controller = new ImageCompatController();

        BladeResult<Map<String, Object>> result = controller.delete("sample.jpg");

        assertEquals(200, result.getCode());
        assertEquals("sample.jpg", result.getData().get("fileName"));
        assertEquals(Boolean.TRUE, result.getData().get("deleted"));
    }

    @Test
    void exposesImageDeleteRoute() throws Exception {
        RequestMapping classMapping = ImageCompatController.class.getAnnotation(RequestMapping.class);
        Method delete = ImageCompatController.class.getDeclaredMethod("delete", String.class);

        assertArrayEquals(new String[] {"/image"}, classMapping.value());
        assertArrayEquals(new String[] {"/delete"}, delete.getAnnotation(DeleteMapping.class).value());
    }
}
