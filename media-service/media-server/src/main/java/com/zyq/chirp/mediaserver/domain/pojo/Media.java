package com.zyq.chirp.mediaserver.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_media_file")
public class Media {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;
    private Long size;
    private String extension;
    private String type;
    private String category;
    private String md5;
    private Timestamp createTime;
    private String url;
}
