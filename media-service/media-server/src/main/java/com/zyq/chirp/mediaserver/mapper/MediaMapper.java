package com.zyq.chirp.mediaserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.mediaserver.domain.pojo.Media;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaMapper extends BaseMapper<Media> {
}
