package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.List;
import lombok.Data;

/** Whitelisted discovery DTOs: no raw vendor payload, account token or connection secrets. */
public interface ComputeInventoryProvider {
    boolean supports(ComputeNode source);
    Page inventoryPage(ComputeNode source,int page,int size);
    @Data class Item {
        private String instanceId;
        private String name;
        private String gpuSpec;
        private Integer gpuAmount;
        private String powerState;
    }
    @Data class Page {
        private int index;
        private int maxPage;
        private int total;
        private List<Item> items;
    }
}
