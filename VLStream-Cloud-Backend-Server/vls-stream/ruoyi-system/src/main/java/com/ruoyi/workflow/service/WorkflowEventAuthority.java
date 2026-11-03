package com.ruoyi.workflow.service;

/** Implementations verify one event using trusted server-side identity, never request-supplied credentials. */
public interface WorkflowEventAuthority {
    void requireEvent(String tenant, String eventId);
}
