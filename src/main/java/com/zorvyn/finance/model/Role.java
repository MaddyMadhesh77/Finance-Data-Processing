package com.zorvyn.finance.model;

/**
 * Represents the role of a user within the finance system.
 *
 * VIEWER   -> can only view dashboard data
 * ANALYST  -> can view records and access insights/summaries
 * ADMIN    -> full management of records and users
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}
