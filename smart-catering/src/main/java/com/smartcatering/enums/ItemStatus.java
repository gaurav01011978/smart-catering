package com.smartcatering.enums;

import java.util.List;

public enum ItemStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED;

    // Jo items vendor ko "busy" bana dete hain
    public static final List<ItemStatus> ACTIVE = List.of(PENDING, ACCEPTED);
}