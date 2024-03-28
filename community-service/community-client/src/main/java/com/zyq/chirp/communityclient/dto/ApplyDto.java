package com.zyq.chirp.communityclient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApplyDto {
    Long id;
    Long userId;
    Long approverId;
    Long communityId;
    Integer type;
    Timestamp createTime;
    Timestamp updateTime;
    Integer status;
}
