package com.zyq.chirp.chirpclient.dto;

import com.zyq.chirp.common.domain.model.QueryDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ChirperQueryDto extends QueryDto {
    Long chirperId;
    List<Long> userIds;
    String type;
    Boolean media;
    Long communityId;
    Long currentUserId;

    @Override
    public void withDefault() {
        super.withDefault();
        if (this.media == null) {
            this.media = false;
        }
    }

    @Override
    public String toString() {
        return "ChirperQueryDto{" +
                "chirperId=" + chirperId +
                ", userIds=" + userIds +
                ", type='" + type + '\'' +
                ", media=" + media +
                ", communityId=" + communityId +
                ", keyword='" + keyword + '\'' +
                ", page=" + page +
                ", pageSize=" + pageSize +
                ", order='" + order + '\'' +
                '}';
    }
}
