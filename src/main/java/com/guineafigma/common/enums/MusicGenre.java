package com.guineafigma.common.enums;

public enum MusicGenre {
    POP("Pop"),
    JAZZ("Jazz"), 
    EDM("EDM"),
    CLASSIC("Classic"),
    ELECTRONIC("Electronic"),
    ACOUSTIC("Acoustic"),
    ROCK("Rock"),
    FOLK("Folk"),
    COUNTRY("Country"),
    BALLAD("Ballad");

    private final String displayName;

    MusicGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}