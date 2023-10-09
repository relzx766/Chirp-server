package com.zyq.chirp.userserver.model.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
@TableName("tb_relation")
public class Relation {
    private Long fromId;
    private Long toId;
    private Timestamp createTime;
    private Integer status;
}
