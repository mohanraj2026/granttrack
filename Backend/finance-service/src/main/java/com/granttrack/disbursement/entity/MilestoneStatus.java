package com.granttrack.disbursement.entity;

public enum MilestoneStatus {
    /** Awaiting its progress-report proof (also the state of a milestone locked behind an earlier one). */
    UPCOMING,
    /** A progress report has been submitted and is being reviewed (Compliance, then Finance). */
    UNDER_REVIEW,
    /** Finance verified the Compliance-approved report; ready for fund release. */
    COMPLETED,
    DISBURSED,
    OVERDUE,
    // Legacy states from the previous evidence flow — retained so existing rows still map.
    EVIDENCE_SUBMITTED,
    APPROVED
}
