package com.ruoyi.vlstream.test.vlstream.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** A validated object staged before the short database transaction of a ZIP import. */
@Getter
@AllArgsConstructor
public class PendingSampleImport {
    private final String archivePath;
    private final String filename;
    private final String objectKey;
    private final long size;
    private final SampleMediaInspector.Inspection inspection;
}
