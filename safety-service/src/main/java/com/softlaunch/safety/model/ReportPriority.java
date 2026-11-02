package com.softlaunch.safety.model;

/** rank is stored too, so Mongo can sort URGENT (0) before HIGH (1) before NORMAL (2). */
public enum ReportPriority {
    URGENT(0), HIGH(1), NORMAL(2);

    private final int rank;

    ReportPriority(int rank) { this.rank = rank; }

    public int rank() { return rank; }
}
