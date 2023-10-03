package com.zyq.chirp.userclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RelationDto {
    private Long fromId;
    private Long toId;
    private Timestamp createTime;
    private Integer status;
}
