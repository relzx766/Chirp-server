package com.zyq.chirp.userserver.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {

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
    private Integer followNum;
    private Integer followingNum;
    private Integer status;
    private Integer relation;
}
