package com.zyq.chirp.userserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.redis.util.BloomUtil;
import com.zyq.chirp.userclient.dto.RelationDto;
import com.zyq.chirp.userclient.dto.UserDto;
import com.zyq.chirp.userserver.convertor.UserConvertor;
import com.zyq.chirp.userserver.mapper.UserMapper;
import com.zyq.chirp.userserver.model.enumeration.AccountStatus;
import com.zyq.chirp.userserver.model.enumeration.RelationType;
import com.zyq.chirp.userserver.model.pojo.User;
import com.zyq.chirp.userserver.service.RelationService;
import com.zyq.chirp.userserver.service.UserService;
import io.lettuce.core.RedisCommandExecutionException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@CacheConfig(cacheNames = "user#5")
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserConvertor userConvertor;
    @Resource
    private RelationService relationService;
    @Value("${default-config.page-size}")
    private Integer pageSize;
    private static final String USERNAME_BLOOM = "bloom:username";
    private static final String EMAIL_BLOOM = "bloom:email";

    @Override
    public boolean update(UserDto userDto) {
        if (Objects.isNull(userDto.getId())) {
            throw new ChirpException(Code.ERR_BUSINESS, "未指定用户");
        }
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userDto.getId());
        Optional.ofNullable(userDto.getPassword()).ifPresent(password -> wrapper.set(User::getPassword, password));
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


    @Override
    public List<UserDto> getByIds(Collection<Long> userIds, Long currentUserId) {
        if (CollectionUtils.isEmpty(userIds)) {
            throw new ChirpException(Code.ERR_BUSINESS, "对象用户为空");
        } else {
            CountDownLatch latch = new CountDownLatch(3);
            final Map<Long, Integer> relation = new HashMap<>();
            final Map<Long, Integer> relationReverse = new HashMap<>();
            if (currentUserId != null) {
                Thread.ofVirtual().start(() -> {
                    relation.putAll(relationService.getUserRelation(userIds, currentUserId).stream()
                            .collect(Collectors.toMap(RelationDto::getToId, RelationDto::getStatus, (k1, k2) -> k1)));
                    latch.countDown();
                });
                Thread.ofVirtual().start(() -> {
                    relationReverse.putAll(relationService.getUserRelationReverse(userIds, currentUserId).stream()
                            .collect(Collectors.toMap(RelationDto::getFromId, RelationDto::getStatus, (k1, k2) -> k1)));
                    latch.countDown();
                });
            } else {
                latch.countDown();
                latch.countDown();
            }
            List<User> userList = new ArrayList<>();
            Thread.ofVirtual().start(() -> {
                userList.addAll(userMapper.selectList(new LambdaQueryWrapper<User>()
                        .in(User::getId, userIds)
                        .eq(User::getStatus, AccountStatus.ACTIVE.getStatus())));
                latch.countDown();
            });
            boolean await;
            try {
                await = latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("", e);
                throw new ChirpException(Code.ERR_SYSTEM, "线程中断");
            }
            if (await) {
                return userList.stream()
                        .map(user -> {
                            UserDto userDto = userConvertor.pojoToDto(user);
                            userDto.clearPwd();
                            Integer relationStatus = relation.get(userDto.getId());
                            relationStatus = RelationType.findWithDefault(relationStatus);
                            userDto.setRelation(relationStatus);
                            Integer relationReverseStatus = relationReverse.get(userDto.getId());
                            relationReverseStatus = RelationType.findWithDefault(relationReverseStatus);
                            userDto.setRelationReverse(relationReverseStatus);
                            return userDto;
                        }).toList();
            } else {
                log.warn("查询超时");
                throw new ChirpException(Code.ERR_SYSTEM, "系统繁忙");
            }

        }
    }


    @Override
    @Cacheable(key = "'profile:'+#username")
    public UserDto getByUsername(String username, Long currentUserId) {
        if (username == null || username.trim().isEmpty()) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供用户信息");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().select(User::getId).eq(User::getUsername, username));
        return getByIds(List.of(user.getId()), currentUserId).getFirst();
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
                .and(queryWrapper -> queryWrapper.eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                        .or()
                        .eq(User::getStatus, AccountStatus.BLOCK.getStatus())));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Override
    public UserDto getDetailById(Long userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .and(queryWrapper -> queryWrapper.eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                        .or()
                        .eq(User::getStatus, AccountStatus.BLOCK.getStatus())));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Override
    public UserDto getDetailByUsername(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .and(queryWrapper -> queryWrapper.eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                        .or()
                        .eq(User::getStatus, AccountStatus.BLOCK.getStatus())));
        return Optional.ofNullable(user)
                .map(target -> userConvertor.pojoToDto(target))
                .orElseThrow(() -> new ChirpException(Code.ERR_BUSINESS, "用户不存在"));
    }

    @Resource
    BloomUtil bloomUtil;
    long INIT_CAPACITY = 100 * 100000000L;

    /**
     * 搜索用户，包含用户基本信息和与当前用户的关系
     *
     * @param keyword
     * @param currentUserId 当前登录用户
     * @return
     */
    @Override
    public List<UserDto> search(String keyword, Long currentUserId, Integer page) {
        try {
            Page<User> userPage = new Page<>(page, pageSize);
            userPage.setSearchCount(false);
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(User::getId)
                    .eq(User::getStatus, AccountStatus.ACTIVE.getStatus())
                    .and(queryWrapper -> queryWrapper.like(User::getNickname, keyword)
                            .or()
                            .like(User::getUsername, keyword));
            List<Long> userIds = userMapper.selectPage(userPage, wrapper).getRecords().stream().map(User::getId).toList();
            return getByIds(userIds, currentUserId);
        } catch (Exception e) {
            return List.of();
        }
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

    @Override
    public List<UserDto> getBasicInfo(Collection<Long> userIds, Long targetId) {
        if (userIds == null || userIds.isEmpty() || targetId == null) {
            throw new ChirpException(Code.ERR_BUSINESS, "未提供完整的用户id信息");
        }
        Map<Long, RelationDto> relationDtoMap = relationService.getUserRelationReverse(userIds, targetId)
                .stream().collect(Collectors.toMap(RelationDto::getFromId, Function.identity(), (k1, k2) -> k1));
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername)
                        .in(User::getId, userIds))
                .stream()
                .map(user -> {
                    UserDto userDto = userConvertor.pojoToDto(user);
                    userDto.setRelation(relationDtoMap.get(userDto.getId()).getStatus());
                    return userDto;
                }).toList();


    }

    @Override
    public List<Long> getIdByUsername(Collection<String> username) {
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId)
                .in(User::getUsername, username)).stream().map(User::getId).toList();
    }

    double ERR_RATE = 0.001;

    @Override
    public UserDto save(UserDto userDto) {
        if (isUnExist(userDto.getUsername())) {
            throw new ChirpException(Code.ERR_BUSINESS, "用户名已存在，请更换");
        }
        if (isEmailExist(userDto.getEmail())) {
            throw new ChirpException(Code.ERR_BUSINESS, "邮箱已存在，请更换");
        }
        User user = userConvertor.dtoToPojo(userDto);
        user.setNickname(user.getUsername());
        user.setCreateTime(new Timestamp(System.currentTimeMillis()));
        user.setStatus(AccountStatus.ACTIVE.getStatus());
        userMapper.insert(user);
        this.saveToUnBloom(user.getUsername());
        this.saveToEmailBloom(user.getEmail());
        return userConvertor.pojoToDto(user);
    }

    @Override
    public boolean isUnExist(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ChirpException(Code.ERR_BUSINESS, "请输入用户名");
        }

        return bloomUtil.exists(USERNAME_BLOOM, username);
    }

    @Override
    public boolean isEmailExist(String email) {
        if (StringUtils.isBlank(email)) {
            throw new ChirpException(Code.ERR_BUSINESS, "请输入邮箱");
        }
        return bloomUtil.exists(EMAIL_BLOOM, email);
    }

    @Override
    @PostConstruct
    public void createUsernameBloom() {
        try {
            bloomUtil.createFilter(USERNAME_BLOOM, INIT_CAPACITY, ERR_RATE);
        } catch (RedisCommandExecutionException e) {
            log.error("{}", e.getMessage());
        }

    }

    @Override
    public boolean saveToUnBloom(String username) {
        return bloomUtil.add(USERNAME_BLOOM, username);
    }

    @Override
    @PostConstruct
    public void createEmailBloom() {
        try {
            bloomUtil.createFilter(EMAIL_BLOOM, INIT_CAPACITY, ERR_RATE);
        } catch (RedisCommandExecutionException e) {
            log.error("{}", e.getMessage());
        }

    }

    @Override
    public boolean saveToEmailBloom(String email) {
        return bloomUtil.add(EMAIL_BLOOM, email);
    }


}
