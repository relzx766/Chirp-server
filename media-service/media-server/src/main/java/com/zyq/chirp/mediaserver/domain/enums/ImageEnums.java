package com.zyq.chirp.mediaserver.domain.enums;

public enum ImageEnums {
    JPG,
    JPEG,
    JPE,
    PNG,
    GIF,
    BMP,
    WEBP,
    TIFF,
    TIF,
    PSD,
    SVG,
    ICO;

    public static ImageEnums find(String extension) {
        for (ImageEnums imageEnums : ImageEnums.values()) {
            if (imageEnums.name().equalsIgnoreCase(extension)) {
                return imageEnums;
            }
        }
        return null;
    }
}
