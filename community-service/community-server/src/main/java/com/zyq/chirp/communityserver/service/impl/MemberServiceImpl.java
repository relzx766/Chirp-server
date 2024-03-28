package com.zyq.chirp.communityserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyq.chirp.common.domain.enums.OrderEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.common.util.StringUtil;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;
import com.zyq.chirp.communityserver.convertor.MemberConvertor;
import com.zyq.chirp.communityserver.domain.enums.CommunityMemberEnum;
import com.zyq.chirp.communityserver.domain.pojo.Member;
import com.zyq.chirp.communityserver.mapper.MemberMapper;
import com.zyq.chirp.communityserver.service.MemberService;
import com.zyq.chirp.communityserver.util.CommunityUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {
    @Resource
    MemberMapper memberMapper;
    @Resource
    MemberConvertor memberConvertor;


    @Override
    public void add(Long userId, Long communityId) {
        Member member = new Member(IdWorker.getId(), communityId, userId, CommunityMemberEnum.MEMBER.getCode(), new Timestamp(System.currentTimeMillis()));
        memberMapper.insert(member);
    }

    @Override
    public void add(MemberDto memberDto) {
        Member member = memberConvertor.dtoToPojo(memberDto);
        member.setCreateTime(new Timestamp(System.currentTimeMillis()));
        memberMapper.insert(member);
    }

    @Override
    public void leave(MemberDto memberDto) {
        boolean remove = remove(new LambdaQueryWrapper<Member>()
                .eq(Member::getCommunityId, memberDto.getCommunityId())
                .eq(Member::getUserId, memberDto.getUserId())
                .lt(Member::getRole, CommunityMemberEnum.ADMIN.getCode()));
        assert remove;
    }


    @Override
    public void update(MemberDto memberDto) {
        memberMapper.update(null, new LambdaUpdateWrapper<Member>()
                .eq(Member::getUserId, memberDto.getUserId())
                .eq(Member::getCommunityId, memberDto.getCommunityId())
                .set(Member::getRole, CommunityMemberEnum.findWithDefault(memberDto.getRole()).getCode()));
    }

    @Override
    public MemberDto getOne(Long userId, Long communityId) {
        return memberConvertor.pojoToDto(memberMapper.selectOne(new LambdaQueryWrapper<Member>().eq(Member::getUserId, userId).eq(Member::getCommunityId, communityId)));
    }

    @Override
    public Map<Long, MemberDto> getMap(Long communityId, Collection<Long> userIds) {
        return list(new LambdaQueryWrapper<Member>()
                .eq(Member::getCommunityId, communityId)
                .in(Member::getUserId, userIds))
                .stream()
                .map(member -> memberConvertor.pojoToDto(member))
                .collect(Collectors.toMap(MemberDto::getUserId, Function.identity()));
    }

    @Override
    public List<MemberDto> getByUserId(Long userId, List<Long> communityId) {
        return memberMapper.selectList(new LambdaQueryWrapper<Member>()
                        .eq(Member::getUserId, userId)
                        .in(Member::getCommunityId, communityId))
                .stream()
                .map(member -> memberConvertor.pojoToDto(member))
                .toList();
    }

    @Override
    public List<MemberDto> getModeratorOfCommunity(Long communityId) {
        return memberMapper.selectList(new LambdaQueryWrapper<Member>()
                        .eq(Member::getCommunityId, communityId)
                        .eq(Member::getRole, CommunityMemberEnum.MODERATOR.getCode())
                        .orderByDesc(Member::getCreateTime))
                .stream()
                .map(member -> memberConvertor.pojoToDto(member)).toList();
    }

    @Override
    public List<MemberDto> getMemberOfCommunity(Long communityId, Integer page) {
        int pageSize = 50;
        Page<Member> memberPage = new Page<>(page, pageSize);
        memberPage.setSearchCount(false);
        return memberMapper.selectPage(memberPage, new LambdaQueryWrapper<Member>()
                        .eq(Member::getCommunityId, communityId)
                        .eq(Member::getRole, CommunityMemberEnum.MEMBER.getCode())
                        .orderByDesc(Member::getCreateTime))
                .getRecords()
                .stream()
                .map(member -> memberConvertor.pojoToDto(member)).toList();
    }

    @Override
    public Long getCountOfCommunity(Long communityId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<Member>().eq(Member::getCommunityId, communityId));
    }

    @Override
    public Map<Long, Long> getCountOfCommunity(List<Long> communityIds) {
        if (!CollectionUtils.isEmpty(communityIds)) {
            int limit = 4;
            ArrayList<List<Long>> subList = new ArrayList<>();
            for (int i = 0; i < communityIds.size(); i += limit) {
                int end = Math.min(i + limit, communityIds.size());
                subList.add(communityIds.subList(i, end));
            }
            CountDownLatch latch = new CountDownLatch(Math.ceilDiv(communityIds.size(), limit));
            final Map<Long, Long> countMap = new HashMap<>();
            for (List<Long> cIds : subList) {
                Thread.ofVirtual().start(() -> {
                    cIds.forEach(id -> {
                        Long count = memberMapper.selectCount(new LambdaQueryWrapper<Member>().eq(Member::getCommunityId, id));
                        countMap.put(id, count);
                    });
                    latch.countDown();
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("批量获取社群成员数量时线程中断", e);
                throw new ChirpException(Code.ERR_SYSTEM, "线程中断");
            }
            return countMap;
        }
        return Map.of();
    }

    @Override
    public List<MemberDto> getPage(CommunityQueryDto communityQueryDto) {
        communityQueryDto.withDefault();
        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<>();
        if (communityQueryDto.getCommunityId() != null) {
            wrapper.eq(Member::getCommunityId, communityQueryDto.getCommunityId());
        }
        if (communityQueryDto.getRole() != null) {
            CommunityMemberEnum memberEnum = CommunityMemberEnum.findWithDefault(communityQueryDto.getRole());
            wrapper.eq(Member::getRole, memberEnum.getCode());
        }
        if (!StringUtil.isBlank(communityQueryDto.getOrder())) {
            wrapper.orderBy(true, OrderEnum.isAsc(communityQueryDto.getOrder()), Member::getCreateTime);
        }
        Page<Member> page = new Page<>(communityQueryDto.getPage(), communityQueryDto.getPageSize());
        page.setSearchCount(false);
        return memberMapper.selectPage(page, wrapper).getRecords().stream().map(member -> memberConvertor.pojoToDto(member)).toList();
    }

    @Override
    public Map<Long, List<MemberDto>> getMemberOfCommunity(List<Long> communityIds, Integer size) {
        if (!CollectionUtils.isEmpty(communityIds)) {
            int limit = 4;
            ArrayList<List<Long>> subList = new ArrayList<>();
            for (int i = 0; i < communityIds.size(); i += limit) {
                int end = Math.min(i + limit, communityIds.size());
                subList.add(communityIds.subList(i, end));
            }
            CountDownLatch latch = new CountDownLatch(Math.ceilDiv(communityIds.size(), limit));
            final Map<Long, List<MemberDto>> map = new HashMap<>();
            for (List<Long> ids : subList) {
                Thread.ofVirtual().start(() -> {
                    ids.forEach(id -> {
                        CommunityQueryDto queryDto = CommunityQueryDto.builder().communityId(id).page(1).pageSize(size).build();
                        map.put(id, getPage(queryDto));
                    });
                    latch.countDown();
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                log.error("批量获取社群成员信息时线程中断", e);
                throw new ChirpException(Code.ERR_SYSTEM, "线程中断");
            }
            return map;
        }
        return Map.of();
    }

    @Override
    public Map<String, MemberDto> getMap(List<Map.Entry<Long, Long>> maps) {
        String inSql = maps.stream()
                .map(map -> STR."(\{map.getKey()},\{map.getValue()})")
                .collect(Collectors.joining(","));
        return list(new QueryWrapper<Member>()
                .inSql("(community_id,user_id)", inSql))
                .stream()
                .map(member -> memberConvertor.pojoToDto(member))
                .collect(Collectors
                        .toMap(memberDto -> StringUtil.combineKey(memberDto.getCommunityId(), memberDto.getUserId()), Function.identity()));
    }
}
