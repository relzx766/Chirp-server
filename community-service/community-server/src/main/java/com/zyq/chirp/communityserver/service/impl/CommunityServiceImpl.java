package com.zyq.chirp.communityserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyq.chirp.common.domain.enums.ApproveEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.domain.model.QueryDto;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.communityclient.dto.ApplyDto;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.InvitationDto;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityserver.convertor.CommunityConvertor;
import com.zyq.chirp.communityserver.domain.enums.ApplyTypeEnum;
import com.zyq.chirp.communityserver.domain.enums.CommunityJoinRangeEnum;
import com.zyq.chirp.communityserver.domain.enums.CommunityMemberEnum;
import com.zyq.chirp.communityserver.domain.enums.CommunityPostRangeEnum;
import com.zyq.chirp.communityserver.domain.pojo.Community;
import com.zyq.chirp.communityserver.mapper.CommunityMapper;
import com.zyq.chirp.communityserver.service.ApplyService;
import com.zyq.chirp.communityserver.service.CommunityService;
import com.zyq.chirp.communityserver.service.MemberService;
import com.zyq.chirp.communityserver.util.CommunityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community> implements CommunityService {
    @Resource
    CommunityMapper communityMapper;
    @Resource
    CommunityConvertor communityConvertor;
    @Resource
    MemberService memberService;
    @Resource
    @Lazy
    ApplyService applyService;
    @Value("${default-config.page-size}")
    Integer pageSize;

    @Resource
    KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    @Transactional
    public CommunityDto save(CommunityDto communityDto) {
        Community community = communityConvertor.dtoToPojo(communityDto);
        community.setUserId(StpUtil.getLoginIdAsLong());
        community.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        if (CommunityJoinRangeEnum.find(community.getJoinRange()) == null) {
            community.setJoinRange(CommunityJoinRangeEnum.ANYONE.getCode());
        }
        if (CommunityPostRangeEnum.find(community.getPostRange()) == null) {
            community.setPostRange(CommunityPostRangeEnum.ANYONE.getCode());
        }
        community.setId(IdWorker.getId());
        save(community);
        return communityConvertor.pojoToDto(community);
    }

    @Override
    public CommunityDto update(CommunityDto communityDto) {
        assert communityDto.getId() != null;
        Community community = communityConvertor.dtoToPojo(communityDto);
        LambdaUpdateWrapper<Community> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Community::getId, community.getId());
        if (!StringUtil.isBlank(community.getCover())) {
            wrapper.set(Community::getCover, community.getCover());
        }
        if (!StringUtil.isBlank(community.getDescription())) {
            wrapper.set(Community::getDescription, community.getDescription());
        }
        if (!StringUtil.isBlank(community.getName())) {
            wrapper.set(Community::getName, community.getName());
        }
        if (CommunityJoinRangeEnum.find(community.getJoinRange()) != null) {
            wrapper.set(Community::getJoinRange, community.getJoinRange());
        }
        if (CommunityPostRangeEnum.find(community.getPostRange()) != null) {
            wrapper.set(Community::getPostRange, community.getPostRange());
        }
        if (!StringUtil.isBlank(community.getRules())) {
            wrapper.set(Community::getRules, community.getRules());
        }
        communityMapper.update(null, wrapper);
        return communityDto;
    }

    @Override
    public CommunityDto getById(Long id) {
        return getById(List.of(id)).getFirst();
    }

    @Override
    public List<CommunityDto> getById(Collection<Long> ids) {
        if (!CollectionUtils.isEmpty(ids)) {
            try {
                CountDownLatch latch = new CountDownLatch(2);
                final Map<Long, Long> countMap = new HashMap<>();
                Thread.ofVirtual().start(() -> {
                    Map<Long, Long> map = memberService.getCountOfCommunity(new ArrayList<>(ids));
                    countMap.putAll(map);
                    latch.countDown();
                });
                Map<Long, List<MemberDto>> memberMap = new HashMap<>();
                Thread.ofVirtual().start(() -> {
                    int size = 5;
                    Map<Long, List<MemberDto>> map = memberService.getMemberOfCommunity(new ArrayList<>(ids), size);
                    memberMap.putAll(map);
                    latch.countDown();
                });
                List<Community> communities = communityMapper.selectList(new LambdaQueryWrapper<Community>()
                        .in(Community::getId, ids));
                latch.await();
                return communities.stream()
                        .map(community -> {
                            CommunityDto communityDto = communityConvertor.pojoToDto(community);
                            Long count = countMap.get(communityDto.getId());
                            count = count != null ? count : 0;
                            //管理员的份
                            count++;
                            communityDto.setMemberCount(count.intValue());
                            List<MemberDto> memberDtos = memberMap.get(communityDto.getId());
                            memberDtos = memberDtos != null ? memberDtos : List.of();
                            communityDto.setMembers(memberDtos);
                            return communityDto;
                        })
                        .toList();
            } catch (InterruptedException e) {
                log.error("获取社群信息时线程中断", e);
                throw new ChirpException(Code.ERR_SYSTEM, "线程中断");
            }
        }
        return List.of();

    }

    @Override
    public CommunityDto join(Long communityId, Long userId) {
        Community community = communityMapper.selectById(communityId);
        CommunityDto communityDto = new CommunityDto();
        communityDto.setId(communityId);
        if (community.getJoinRange() > CommunityJoinRangeEnum.ANYONE.getCode()) {
            ApplyDto applyDto = ApplyDto.builder().userId(userId).communityId(communityId).type(ApplyTypeEnum.JOIN.getCode()).build();
            applyService.addOne(applyDto);
            communityDto.setJoinStatus(ApproveEnum.PENDING.getStatus());
        } else {
            memberService.add(userId, communityId);
            communityDto.setJoinStatus(ApproveEnum.ACCEPTED.getStatus());
        }
        return communityDto;
    }

    @Override
    public void join(InvitationDto invitationDto) {
        if (ApproveEnum.isAvailable(invitationDto.getStatus())) {
            CommunityDto communityDto = getById(invitationDto.getCommunity().getId());
            MemberDto memberDto = memberService.getOne(invitationDto.getFromId(), invitationDto.getCommunity().getId());
            boolean couldJoin = CommunityUtil.canJoin(communityDto.getJoinRange(), memberDto.getRole());
            if (couldJoin) {
                memberService.add(invitationDto.getToId(), invitationDto.getCommunity().getId());
            } else {
                throw new ChirpException(Code.ERR_BUSINESS, CommunityJoinRangeEnum.find(communityDto.getJoinRange()).getMessage());
            }
        }
    }

    @Override
    public List<CommunityDto> getPage(QueryDto queryDto) {
        queryDto.withDefault();
        Page<Community> searchPage = new Page<>(queryDto.getPage(), queryDto.getPageSize());
        searchPage.setSearchCount(false);
        LambdaQueryWrapper<Community> wrapper = new LambdaQueryWrapper<Community>()
                .select(Community::getId)
                .orderByDesc(Community::getCreateTime);
        if (!StringUtil.isBlank(queryDto.getKeyword())) {
            wrapper.like(Community::getName, queryDto.getKeyword());
        }
        List<Long> ids = page(searchPage, wrapper)
                .getRecords()
                .stream()
                .map(Community::getId).toList();
        return getById(ids);
    }

    @Override
    public List<CommunityDto> assemble(List<CommunityDto> communityDtos, Long userId) {
        if (!CollectionUtils.isEmpty(communityDtos)) {
            List<Long> communityIds = communityDtos.stream().map(CommunityDto::getId).toList();
            Map<Long, MemberDto> memberDtoMap = memberService.getByUserId(userId, communityIds).stream().collect(Collectors.toMap(MemberDto::getCommunityId, Function.identity()));
            Map<Long, ApplyDto> applyDtoMap = applyService.getByUserId(communityIds, userId);
            return communityDtos.stream()
                    .peek(communityDto -> {
                        if (memberDtoMap.get(communityDto.getId()) != null) {
                            communityDto.setJoinStatus(ApproveEnum.ACCEPTED.getStatus());
                        } else {
                            ApplyDto applyDto = applyDtoMap.get(communityDto.getId());
                            Integer status = applyDto != null ? applyDto.getStatus() : ApproveEnum.INVALID.getStatus();
                            communityDto.setJoinStatus(status);
                        }
                    }).toList();
        }
        return List.of();
    }

    @Override
    public void leave(Long communityId, Long userId) {
        long loginId = StpUtil.getLoginIdAsLong();
        MemberDto memberDto = new MemberDto();
        memberDto.setCommunityId(communityId);
        memberDto.setUserId(userId);
        if (userId.equals(loginId)) {
            memberService.leave(memberDto);
        } else {
            CommunityDto communityDto = getById(communityId);
            Map<Long, MemberDto> memberDtoMap = memberService.getMap(communityId, List.of(userId, loginId));
            boolean couldJoin = loginId == communityDto.getUserId() || CommunityUtil.canJoin(communityDto.getJoinRange(), memberDtoMap.get(loginId).getRole());
            boolean couldDelete = loginId == communityDto.getUserId() || memberDtoMap.get(loginId).getRole() > memberDtoMap.get(userId).getRole();
            assert couldDelete && couldJoin;
            memberService.leave(memberDto);
        }

    }

    @Override
    public void updateRole(Long communityId, Long userId, Integer role) {
        CommunityDto communityDto = getById(communityId);
        long loginId = StpUtil.getLoginIdAsLong();
        assert loginId == communityDto.getUserId();
        MemberDto memberDto = MemberDto.builder().userId(userId).communityId(communityId).role(role).build();
        memberService.update(memberDto);
    }

    @Override
    public Map<String, CommunityDto> getMap(List<Map.Entry<Long, Long>> maps) {
        Map<String, MemberDto> memberDtoMap = memberService.getMap(maps);
        List<Long> communityIds = maps.stream().map(Map.Entry::getKey).toList();
        Map<Long, Community> communityMap = list(new LambdaQueryWrapper<Community>().in(Community::getId, communityIds)).stream().collect(Collectors.toMap(Community::getId, Function.identity()));
        Map<String, CommunityDto> communityDtoMap = new HashMap<>();
        memberDtoMap.forEach((key, value) -> {
            Community community = communityMap.get(value.getCommunityId());
            CommunityDto communityDto = communityConvertor.pojoToDto(community);
            boolean couldPost = CommunityUtil.canPost(communityDto.getPostRange(), value.getRole());
            communityDto.setPostable(couldPost);
            communityDtoMap.put(key, communityDto);
        });
        return communityDtoMap;

    }
}
