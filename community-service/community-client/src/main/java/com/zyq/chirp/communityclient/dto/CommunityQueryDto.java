package com.zyq.chirp.communityclient.dto;

import com.zyq.chirp.common.domain.model.QueryDto;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder

public class CommunityQueryDto extends QueryDto {
    Integer role;
    Long communityId;
    List<Integer> status;
}
