package com.zyq.chirp.userserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.userserver.model.pojo.Relation;
import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Repository;

@Repository
public interface RelationMapper extends BaseMapper<Relation> {
    @Insert("""
            replace into tb_relation(from_id, to_id, create_time, status) 
            values (#{fromId} ,#{toId} ,#{createTime} ,#{status} )
            """)
    int replace(Relation relation);
}
