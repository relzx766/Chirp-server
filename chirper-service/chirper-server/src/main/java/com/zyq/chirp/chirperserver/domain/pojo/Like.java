package com.zyq.chirp.chirperserver.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Like {
    Long chirperId;
    Long userId;
    Timestamp createTime;
}
