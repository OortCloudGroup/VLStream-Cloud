package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.MobileSceneGovernance;

/** Service contract for mobile immediate and cyclic governance. */
public interface IVlsMobileSceneGovernanceService {

    /** Persist one immediate-governance task. */
    MobileSceneGovernance saveImmediate(MobileSceneGovernance governance);

    /** Persist one cyclic task and generate all executable child tasks. */
    MobileSceneGovernance saveLoop(MobileSceneGovernance governance);

    /** Page immediate tasks with resolved algorithm and camera names. */
    BladePage<MobileSceneGovernance> listImmediate(Long current, Long size);

    /** Page cyclic tasks with their persisted child tasks. */
    BladePage<MobileSceneGovernance> listLoop(Long current, Long size);
}
