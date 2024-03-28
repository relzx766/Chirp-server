package com.zyq.chirp.communityserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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
@TableName("tb_community_invitation")
public class Invitation {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long fromId;
    Long toId;
    Long communityId;
    Timestamp createTime;
    Timestamp updateTime;
    Integer status;
}
