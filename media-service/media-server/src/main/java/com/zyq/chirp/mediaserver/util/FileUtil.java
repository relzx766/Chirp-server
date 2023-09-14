package com.zyq.chirp.mediaserver.util;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileUtil {


    public static String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static void creatNewFile(Path path) throws IOException {
        File file = new File(path.toString());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static String getMime(Path path) throws IOException {
        Tika tika = new Tika();
        return tika.detect(path);
    }

    public static String getMediaCategory(Path path) throws IOException {
        // 获取文件的媒体类别
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, path.getFileName().toString());
        MediaType mediaType = detector.detect(TikaInputStream.get(path, metadata), metadata);
        return mediaType.getType();
    }

    public static String getMediaCategory(String mime) {
        return mime.substring(0, mime.lastIndexOf("/"));
    }
}
