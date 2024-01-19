package com.zyq.chirp.mediaserver.domain.enums;

public enum VideoEnums {
    MP4,
    AVI,
    FLV,
    MOV,
    WMV,
    RMVB,
    MKV,
    WEBM,
    MPEG,
    VOB;

    public static VideoEnums find(String extension) {
        for (VideoEnums videoEnums : VideoEnums.values()) {
            if (videoEnums.name().equalsIgnoreCase(extension)) {
                return videoEnums;
            }
        }
        return null;
    }
}