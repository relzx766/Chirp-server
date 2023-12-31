package com.zyq.chirp.userclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)

    private Long fromId;
    @JsonSerialize(using = ToStringSerializer.class)

    private Long toId;
    private Timestamp createTime;
    private Integer status;
}
