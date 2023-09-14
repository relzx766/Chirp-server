package com.zyq.chirp.chirperserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.chirperserver.domain.pojo.Like;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikeMapper extends BaseMapper<Like> {

    int insertList(List<Like> likes);

    int deleteList(List<Like> likes);

    int updateChirperLikeCount(@Param("chirpreId") long chirperId, @Param("delta") int delta);
}
