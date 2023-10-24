package com.zyq.chirp.userclient.dto;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {
    private static final long serialVersionUID = 123451111L;
    @JsonSerialize(using = ToStringSerializer.class)

    private Long id;
    @Length(min = 4, max = 30, message = "用户名长度应该为4-30")
    @Pattern(regexp = "[a-zA-Z0-9]+", message = "用户名只能包含字母和数字")
    private String username;
    @Length(min = 8, message = "密码不能小于8位")
    private String password;
    @Length(min = 1, max = 40, message = "昵称长度应为1-40")
    private String nickname;
    @Email(message = "请输入合法邮箱")
    private String email;
    private Date birthday;
    private String gender;
    private Timestamp createTime;
    @Length(max = 255)
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
