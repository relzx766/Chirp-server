package com.zyq.chirp.mediaclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChunkUploadReqDto {
    String uploadId;
    String objectName;
    Integer chunkSize;
    String type;
    String extension;
}
