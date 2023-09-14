package com.zyq.chirp.chirperserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;
import com.zyq.chirp.chirperserver.domain.vo.ChirperVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChirperMapper extends BaseMapper<Chirper> {
    int saveBatch(List<Chirper> chirpers, ChirperType type);

    ChirperVo getById(@Param("chirperId") Long chirperId,
                      @Param("userId") Long currentUserId);

    List<ChirperVo> getLimit(@Param("userId") Long currentUserId,
                             @Param("offset") int offset,
                             @Param("size") int size);

    List<ChirperVo> getMatchLimit(@Param("keyword") String keyword,
                                  @Param("userId") Long currentUserId,
                                  @Param("offset") int offset,
                                  @Param("size") int size,
                                  @Param("isMedia") boolean isMedia);

    List<ChirperVo> getChildrenChirperLimit(@Param("parentChirperId") Long parentChirperId,
                                            @Param("userId") Long currentUserId,
                                            @Param("offset") int offset,
                                            @Param("size") int size);

    List<ChirperVo> getByAuthorLimit(@Param("authorId") Long authorId,
                                     @Param("userId") Long currentUserId,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    List<Chirper> getIdByReferenceAndAuthor(@Param("chirpers") List<Chirper> chirpers,
                                            @Param("type") String type);


    int addReply(Chirper chirper);

}
