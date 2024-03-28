package com.zyq.chirp.communityserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_community_apply")
public class Apply {
    Long id;
    Long userId;
    Long approverId;
    Long communityId;
    Integer type;
    Timestamp createTime;
    Timestamp updateTime;
    Integer status;
}
