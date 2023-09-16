package com.zyq.chirp.userserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zyq.chirp.common.exception.ChirpException;
import com.zyq.chirp.common.model.Code;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.convertor.UserConvertor;
import com.zyq.chirp.userserver.mapper.UserMapper;
import com.zyq.chirp.userserver.model.enumeration.AccountStatus;
import com.zyq.chirp.userserver.model.enumeration.RelationType;
import com.zyq.chirp.userserver.model.pojo.Relation;
import com.zyq.chirp.userserver.model.pojo.User;
import com.zyq.chirp.userserver.model.vo.UserVo;
import com.zyq.chirp.userserver.service.RelationService;
import com.zyq.chirp.userserver.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserConvertor userConvertor;
    @Resource
    private RelationService relationService;
    @Value("${default-config.page-size}")
    private Integer pageSize;

    @Override
    public UserDto save(UserDto userDto) {
        if (isExistByUsername(userDto.getUsername())) {
            throw new ChirpException(Code.ERR_BUSINESS, "用户名已存在，请更换");
        }
        if (isExistByEmail(userDto.getEmail())) {
            throw new ChirpException(Code.ERR_BUSINESS, "邮箱已存在，请更换");
        }
        User user = userConvertor.dtoToPojo(userDto);
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user.setStatus(AccountStatus.ACTIVE.getStatus());
        userMapper.insert(user);
        return userConvertor.pojoToDto(user);
    }

    @Override
    public boolean update(UserDto userDto) {
        if (Objects.isNull(userDto.getId())) {
            throw new ChirpException(Code.ERR_BUSINESS, "未指定用户");
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userDto.getId());
        Optional.ofNullable(userDto.getNickname()).ifPresent(nickname -> wrapper.set(User::getNickname, nickname));
        Optional.ofNullable(userDto.getBirthday()).ifPresent(birthday -> wrapper.set(User::getBirthday, birthday));
        Optional.ofNullable(userDto.getGender()).ifPresent(gender -> wrapper.set(User::getGender, gender));
        Optional.ofNullable(userDto.getDescription()).ifPresent(description -> wrapper.set(User::getDescription, description));
        Optional.ofNullable(userDto.getProfileBackUrl()).ifPresent(backgroundUrl -> wrapper.set(User::getProfileBackUrl, backgroundUrl));
        Optional.ofNullable(userDto.getLargeAvatarUrl()).ifPresent(largeAvatar -> {
            wrapper.set(User::getLargeAvatarUrl, largeAvatar);
            wrapper.set(User::getMediumAvatarUrl, largeAvatar);
            wrapper.set(User::getSmallAvatarUrl, largeAvatar);
        });
        userMapper.update(null, wrapper);
        return true;
    }

    /**
     * 获取用户主页，包含用户基本信息和与当前用户的关系
     *
     * @param userId
     * @param currentUserId 当前登录用户
     * @return
     */
    @Override
    public UserDto getById(Long userId, Long currentUserId) {
        if (Objects.isNull(userId)) {
            throw new ChirpException(Code.ERR_BUSINESS, "对象用户为空");
        }
        UserVo userVo = userMapper.getById(userId);
        if (currentUserId != null) {
            userVo.setRelation(RelationType.UNFOLLOWED.getRelation());
            Relation relation = relationService.getRelationType(currentUserId, userId);
            if (relation != null) {
                userVo.setRelation(relation.getStatus());
            }
        }
        return userConvertor.voToDto(userVo);
    }

    /**
     * 获取用户主页，包含用户基本信息和与当前用户的关系
     *
     * @param username
     * @param currentUserId 当前登录用户
     * @return
     */


    /**
     * 用于邮箱验证登录，当auth验证成功后调用
     *
     * @param email
     * @return
     */
    @Override
    public UserDto getDetailByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                .or()
                .eq(User::getStatus, AccountStatus.BLOCK.getStatus()));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Override
    public UserDto getDetailById(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                .or()
                .eq(User::getStatus, AccountStatus.BLOCK.getStatus()));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Override
    public UserDto getDetailByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                .or()
                .eq(User::getStatus, AccountStatus.BLOCK.getStatus()));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Override
    public boolean isExistByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ChirpException(Code.ERR_BUSINESS, "请输入用户名");
        }
        return Objects.nonNull(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)));
    }

    @Override
    public boolean isExistByEmail(String email) {
        if (StringUtils.isBlank(email)) {
            throw new ChirpException(Code.ERR_BUSINESS, "请输入邮箱");
        }
        return Objects.nonNull(userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email)));
    }

    /**
     * 搜索用户，包含用户基本信息和与当前用户的关系
     *
     * @param keyword
     * @param currentUserId 当前登录用户
     * @return
     */
    @Override
    public List<UserDto> search(String keyword, Long currentUserId, Integer page) {
        page = Optional.ofNullable(page).orElse(1);
        int currentIndex = (page - 1) * pageSize;
        List<UserDto> userDtos = userMapper.matchUserByKeyword(keyword, AccountStatus.INACTIVE.getStatus(), currentIndex, pageSize).stream()
                .map(user -> userConvertor.pojoToDto(user)).toList();
        List<Long> userIds = userDtos.stream().map(UserDto::getId).toList();
        if (Objects.nonNull(currentUserId)) {
            Map<Long, Integer> relationMap = relationService.getUserRelation(userIds, currentUserId)
                    .stream()
                    .collect(Collectors.toMap(Relation::getToId, Relation::getStatus));
            userDtos.forEach(userDto -> userDto.setRelation(relationMap.get(userDto.getId())));
        }
        return userDtos;
    }

    @Override
    public List<UserDto> getBasicInfo(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户id");
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId,
                                User::getUsername,
                                User::getNickname,
                                User::getSmallAvatarUrl,
                                User::getLargeAvatarUrl)
                        .in(User::getId, userIds))
                .stream()
                .map(user -> userConvertor.pojoToDto(user))
                .toList();
    }
}
