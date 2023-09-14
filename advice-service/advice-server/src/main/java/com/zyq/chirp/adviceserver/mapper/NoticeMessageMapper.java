package com.zyq.chirp.adviceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.adviceserver.domain.pojo.InteractionMessage;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NoticeMessageMapper extends BaseMapper<InteractionMessage> {
    int insertOne(InteractionMessage interactionMessage);

    int insertBatch(@Param("messages") Collection<InteractionMessage> interactionMessages);

    List<InteractionMessage> getByReceiverId(Long id);

    List<InteractionMessage> getUnReadByReceiverId(Long id);
}
