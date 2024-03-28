package com.zyq.chirp.communityclient.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvitationDto {
    Long id;
    Long fromId;
    @NotNull
    Long toId;
    @NotNull
    CommunityDto community;
    Timestamp createTime;
    Timestamp updateTime;
    Integer type;
    Integer status;
}
