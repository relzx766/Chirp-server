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
@TableName("user_of_community")
public class Member {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long communityId;
    Long userId;
    Integer role;
    Timestamp createTime;
}
