package com.zyq.chirp.chirperserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_chirper_like")
public class Like {
    Long chirperId;
    Long userId;
    Timestamp createTime;
}
