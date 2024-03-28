package com.zyq.chirp.communityserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "tb_community", autoResultMap = true)
public class Community {
    @TableId(type = IdType.ASSIGN_ID)
    Long id;
    Long userId;
    String name;
    String cover;
    String description;
    Integer joinRange;
    Integer postRange;
    String rules;
    String tags;
    Timestamp createTime;

}
