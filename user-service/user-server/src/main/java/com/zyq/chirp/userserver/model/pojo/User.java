package com.zyq.chirp.userserver.model.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("tb_user")
public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 123451111L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;
    private Date birthday;
    private String gender;
    private Timestamp createTime;

    private String description;

    private String profileBackUrl;
    private String smallAvatarUrl;
    private String mediumAvatarUrl;
    private String largeAvatarUrl;

    private Integer status;

}
