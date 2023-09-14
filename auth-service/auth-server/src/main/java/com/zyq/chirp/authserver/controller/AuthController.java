package com.zyq.chirp.authserver.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.zyq.chirp.authserver.util.AccountType;
import com.zyq.chirp.authserver.util.AccountTypeUtil;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.common.model.Result;
import com.zyq.chirp.userclient.client.UserClient;
import com.zyq.chirp.userclient.dto.UserDto;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {
    @Resource
    UserClient userClient;

    @PostMapping("/signIn")
    public ResponseEntity<Map<String, Object>> signIn(@RequestParam("account") String account, @RequestParam("password") String password) {
        UserDto userDto;
        if (AccountType.USERNAME.equals(AccountTypeUtil.identifyType(account))) {
            userDto = userClient.getDetailByUsername(account).getBody();
        } else if (AccountType.EMAIL.equals(AccountTypeUtil.identifyType(account))) {
            userDto = userClient.getDetailByEmail(account).getBody();
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, "未知的账号格式");
        }
        if (userDto.getPassword().equals(password)) {
            StpUtil.login(userDto.getId());
            userDto.setPassword("");
            Map<String, Object> data = new HashMap<>();
            data.put("user", userDto);
            data.put("token", StpUtil.getTokenValue());
            return ResponseEntity.ok(data);
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, "账号或密码错误");
        }
    }

    @PostMapping("/signUp")
    public Result signUp(@RequestBody UserDto userDto) {
        return Result.ok().put("user", userClient.addUser(userDto).getBody());
    }

    @RequestMapping("/signOut")
    public void signOut() {
        StpUtil.logout();
    }
}