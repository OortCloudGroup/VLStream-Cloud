package com.ruoyi.workflow.event;

import java.util.Collection;

public class ValidationEvent {

    private final Collection<String> ids;

    public ValidationEvent(Collection<String> ids) {
        this.ids = ids;
    }

    public Collection<String> getIds() {
        return ids;
    }
}
