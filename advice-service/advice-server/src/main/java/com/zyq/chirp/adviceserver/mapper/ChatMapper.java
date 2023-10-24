package com.zyq.chirp.adviceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.adviceserver.domain.pojo.Chat;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ChatMapper extends BaseMapper<Chat> {

    int insertBatch(Collection<Chat> messages);

}
