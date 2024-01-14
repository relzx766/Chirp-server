package com.zyq.chirp.userclient.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zyq.chirp.userclient.enums.RelationType;
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

    public static RelationDto follow(Long fromId, Long toId) {
        return RelationDto.builder()
                .fromId(fromId)
                .toId(toId)
                .status(RelationType.FOLLOWING.getRelation())
                .createTime(new Timestamp(0))
                .build();
    }

    public static RelationDto unFollow(Long fromId, Long toId) {
        return RelationDto.builder()
                .fromId(fromId)
                .toId(toId)
                .status(RelationType.UNFOLLOWED.getRelation())
                .createTime(new Timestamp(0))
                .build();
    }

    public boolean getIsFollow() {
        return RelationType.FOLLOWING.getRelation() == this.status;
    }

    public boolean getIsBlock() {
        return RelationType.BLOCK.getRelation() == this.status;
    }

}
