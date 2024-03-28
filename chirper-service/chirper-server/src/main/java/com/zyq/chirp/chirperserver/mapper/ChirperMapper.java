package com.zyq.chirp.chirperserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.chirperserver.domain.enums.ChirperType;
import com.zyq.chirp.chirperserver.domain.pojo.Chirper;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChirperMapper extends BaseMapper<Chirper> {
    int saveBatch(List<Chirper> chirpers, ChirperType type);


    List<Chirper> getIdByReferenceAndAuthor(@Param("chirpers") List<Chirper> chirpers,
                                            @Param("type") String type);


    int addReply(Chirper chirper);

    int addForward(Chirper chirper);

    /*    int addForwardBatch(List<Chirper> chirpers);*/
    int addQuote(Chirper chirper);

}
