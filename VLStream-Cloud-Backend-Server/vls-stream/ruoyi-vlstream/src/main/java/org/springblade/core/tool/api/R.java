package org.springblade.core.tool.api;

import lombok.Data;

import java.io.Serializable;

/**
 * SpringBlade-compatible response body used by the copied VLS controllers.
 *
 * @param <T> response data type
 */
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int SUCCESS_CODE = 200;
    private static final int FAILURE_CODE = 500;

    private int code;
    private boolean success;
    private T data;
    private String msg;

    /**
     * Creates a successful response without data.
     */
    public static <T> R<T> success(String message) {
        return build(SUCCESS_CODE, true, null, message);
    }

    /**
     * Creates a successful response using the default message.
     */
    public static <T> R<T> success() {
        return build(SUCCESS_CODE, true, null, "操作成功");
    }

    /**
     * Creates a successful response containing data.
     */
    public static <T> R<T> data(T data) {
        return build(SUCCESS_CODE, true, data, "操作成功");
    }

    /**
     * Converts a real operation result into success or failure.
     */
    public static <T> R<T> status(boolean status) {
        return status ? build(SUCCESS_CODE, true, null, "操作成功")
            : build(FAILURE_CODE, false, null, "操作失败");
    }

    /**
     * Creates a failed response with the supplied reason.
     */
    public static <T> R<T> fail(String message) {
        return build(FAILURE_CODE, false, null, message);
    }

    /**
     * Builds the response object without masking the requested status.
     */
    private static <T> R<T> build(int code, boolean success, T data, String message) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setSuccess(success);
        result.setData(data);
        result.setMsg(message);
        return result;
    }
}
