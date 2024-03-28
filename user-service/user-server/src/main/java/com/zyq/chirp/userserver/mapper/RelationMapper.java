package com.zyq.chirp.userserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.userserver.model.pojo.Relation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationMapper extends BaseMapper<Relation> {
    @Insert("""
            replace into tb_relation(from_id, to_id, create_time, status) 
            values (#{fromId} ,#{toId} ,#{createTime} ,#{status} )
            """)
    int replace(Relation relation);

    @Select({"""
            <script>
            select from_id, to_id, create_time, status from tb_relation where (from_id,to_id) in 
            <foreach collection='relations' item='relation' open='(' separator=',' close=')'>
            (#{relation.fromId},#{relation.toId})
            </foreach>
            </script>
            """})
    List<Relation> getList(List<Relation> relations);
}
