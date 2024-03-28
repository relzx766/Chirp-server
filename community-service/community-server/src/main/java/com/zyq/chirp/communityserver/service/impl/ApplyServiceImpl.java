package com.zyq.chirp.communityserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zyq.chirp.common.domain.enums.ApproveEnum;
import com.zyq.chirp.common.domain.enums.OrderEnum;
import com.zyq.chirp.common.domain.exception.ChirpException;
import com.zyq.chirp.common.domain.model.Code;
import com.zyq.chirp.communityclient.dto.ApplyDto;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.CommunityQueryDto;
import com.zyq.chirp.communityclient.dto.MemberDto;
import com.zyq.chirp.communityserver.convertor.ApplyConvertor;
import com.zyq.chirp.communityserver.domain.enums.ApplyTypeEnum;
import com.zyq.chirp.communityserver.domain.enums.CommunityMemberEnum;
import com.zyq.chirp.communityserver.domain.pojo.Apply;
import com.zyq.chirp.communityserver.mapper.ApplyMapper;
import com.zyq.chirp.communityserver.service.ApplyService;
import com.zyq.chirp.communityserver.service.CommunityService;
import com.zyq.chirp.communityserver.service.MemberService;
import com.zyq.chirp.communityserver.util.CommunityUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ApplyServiceImpl extends ServiceImpl<ApplyMapper, Apply> implements ApplyService {
    @Resource
    ApplyMapper applyMapper;
    @Resource
    ApplyConvertor applyConvertor;
    @Resource
    MemberService memberService;
    @Resource
    CommunityService communityService;

    @Override
    public void addOne(ApplyDto applyDto) {
        applyDto.setType(ApplyTypeEnum.findWithDefault(applyDto.getType()).getCode());
        applyDto.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        applyDto.setStatus(ApproveEnum.PENDING.getStatus());
        save(applyConvertor.dtoToPojo(applyDto));
    }

    @Override
    @Transactional
    public void approve(ApplyDto applyDto) {
        Apply apply = getById(applyDto.getId());
        CommunityDto communityDto = communityService.getById(apply.getCommunityId());
        long approver = StpUtil.getLoginIdAsLong();
        MemberDto memberDto = memberService.getOne(approver, communityDto.getId());
        if (approver == communityDto.getUserId() || CommunityUtil.canJoin(communityDto.getJoinRange(), memberDto.getRole())) {
            ApproveEnum approveEnum = ApproveEnum.findWithDefault(applyDto.getStatus());
            boolean update = update(new LambdaUpdateWrapper<Apply>()
                    .eq(Apply::getId, applyDto.getId())
                    .set(Apply::getApproverId, approver)
                    .set(Apply::getUpdateTime, Timestamp.valueOf(LocalDateTime.now()))
                    .set(Apply::getStatus, approveEnum.getStatus()));
            if (update && approveEnum.equals(ApproveEnum.ACCEPTED)) {
                memberService.add(MemberDto.builder()
                        .communityId(communityDto.getId())
                        .userId(apply.getUserId())
                        .role(CommunityMemberEnum.MEMBER.getCode())
                        .build());
            }
        } else {
            throw new ChirpException(Code.ERR_BUSINESS, "操作失败，权限不足");
        }
    }


    @Override
    public List<ApplyDto> getPage(CommunityQueryDto queryDto) {
        queryDto.withDefault();
        long userId = StpUtil.getLoginIdAsLong();
        CommunityDto communityDto = communityService.getById(queryDto.getCommunityId());
        MemberDto memberDto = memberService.getOne(userId, queryDto.getCommunityId());
        if (userId == communityDto.getUserId() || CommunityUtil.canJoin(communityDto.getJoinRange(), memberDto.getRole())) {
            Page<Apply> applyPage = new Page<>(queryDto.getPage(), queryDto.getPageSize());
            applyPage.setSearchCount(false);
            LambdaQueryWrapper<Apply> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Apply::getCommunityId, queryDto.getCommunityId());
            wrapper.orderBy(true, OrderEnum.isAsc(queryDto.getOrder()), Apply::getCreateTime);
            if (!CollectionUtils.isEmpty(queryDto.getStatus())) {
                wrapper.in(Apply::getStatus, queryDto.getStatus());
            }
            return page(applyPage, wrapper).getRecords()
                    .stream()
                    .map(apply -> applyConvertor.pojoToDto(apply))
                    .toList();
        }
        return List.of();
    }

    @Override
    public Map<Long, ApplyDto> getByUserId(Collection<Long> communityIds, Long userId) {
        if (!CollectionUtils.isEmpty(communityIds)) {
            return list(new LambdaQueryWrapper<Apply>()
                    .eq(Apply::getUserId, userId)
                    .in(Apply::getCommunityId, communityIds)
                    .in(Apply::getStatus, ApproveEnum.PENDING.getStatus()))
                    .stream()
                    .map(apply -> applyConvertor.pojoToDto(apply))
                    .collect(Collectors.toMap(ApplyDto::getCommunityId, Function.identity()));
        }
        return Map.of();
    }
}
