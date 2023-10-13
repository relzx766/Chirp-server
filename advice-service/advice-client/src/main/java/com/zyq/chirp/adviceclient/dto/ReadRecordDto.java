package com.zyq.chirp.adviceclient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadRecordDto {
    Long userId;
    Timestamp lastReadTime;
    Timestamp finalReadTime;
}
