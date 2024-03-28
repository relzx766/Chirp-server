package com.zyq.chirp.communityclient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommunityDto {
    Long id;
    Long userId;
    @NotBlank
    String name;
    @NotBlank
    String cover;
    @Length(max = 1000)
    String description;
    Integer joinRange;
    Integer postRange;
    Timestamp createTime;
    @Size(max = 20)
    List<RuleDto> rules;
    @Size(max = 20)
    List<String> tags;
    Integer joinStatus;
    Integer memberCount;
    List<MemberDto> members;
    Boolean postable;
}
