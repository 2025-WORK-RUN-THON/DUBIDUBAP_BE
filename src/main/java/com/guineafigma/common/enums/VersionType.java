package com.guineafigma.common.enums;

public enum VersionType {
    SHORT("Short"),
    LONG("Long");

    private final String displayName;

    VersionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}