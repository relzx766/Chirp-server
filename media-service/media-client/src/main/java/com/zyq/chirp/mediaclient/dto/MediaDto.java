package com.zyq.chirp.mediaclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MediaDto {
    private Integer id;

    private String name;
    private Long size;
    private String extension;
    private String type;
    private String category;

    private String md5;
    private Timestamp createTime;
    private String url;


}
