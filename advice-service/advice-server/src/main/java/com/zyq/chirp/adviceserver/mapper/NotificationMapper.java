package com.zyq.chirp.adviceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.adviceserver.domain.pojo.Notification;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationMapper extends BaseMapper<Notification> {

    int insertBatch(@Param("messages") Collection<Notification> notifications);

    List<Notification> getByReceiverId(Long id);

    List<Notification> getUnReadByReceiverId(Long id);
}
