/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.compat;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * SpringBlade-compatible page payload with records/total/size/current fields.
 */
public class BladePage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long size;
    private long current;

    public BladePage() {
        this.records = Collections.emptyList();
    }

    private BladePage(List<T> records, long total, long size, long current) {
        this.records = records == null ? Collections.<T>emptyList() : records;
        this.total = total;
        this.size = size;
        this.current = current;
    }

    /**
     * Create a SpringBlade-compatible page payload.
     */
    public static <T> BladePage<T> of(List<T> records, long total, long size, long current) {
        return new BladePage<T>(records, total, size, current);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }
}
