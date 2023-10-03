package com.zyq.chirp.userserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyq.chirp.userserver.model.pojo.User;
import com.zyq.chirp.userserver.model.vo.UserVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserMapper extends BaseMapper<User> {
    /**
     * 分页获取符合的结果
     *
     * @param keyword
     * @param excludeStatus 排除的账号状态，如注销
     * @param currentIndex
     * @param size
     * @return
     */
    @Select("""
            select * from tb_user where match(username,nickname) against(#{keyword})
            and not (status=#{excludeStatus})
            limit #{currentIndex},#{size}
            """)
    List<User> matchUserByKeyword(@Param("keyword") String keyword,
                                  @Param("excludeStatus") int excludeStatus,
                                  @Param("currentIndex") int currentIndex,
                                  @Param("size") int size);

    @Select("""
                       
             select tu.id,
                   tu.username,
                   tu.nickname,
                   tu.email,
                   tu.birthday,
                   tu.gender,
                   tu.create_time,
                   tu.description,
                   tu.profile_back_url,
                   tu.small_avatar_url,
                   tu.medium_avatar_url,
                   tu.large_avatar_url,
                   tu.status,
                   sum(case when tr.to_id = tu.id then 1 else 0 end) as follow_num,
                   sum(case when tr.from_id = tu.id then 1 else 0 end) as following_num
            from tb_user tu
                     left join
                 tb_relation tr
                 on tr.to_id = tu.id or tr.from_id = tu.id
            where tu.id = #{id}
                and (tu.status = 1 or tu.status = 3)
            group by tu.id;
            """)
    UserVo getById(Long id);

    @Select("""
                       
             select tu.id,
                   tu.username,
                   tu.nickname,
                   tu.email,
                   tu.birthday,
                   tu.gender,
                   tu.create_time,
                   tu.description,
                   tu.profile_back_url,
                   tu.small_avatar_url,
                   tu.medium_avatar_url,
                   tu.large_avatar_url,
                   tu.status,
                   sum(case when tr.to_id = tu.id then 1 else 0 end) as follow_num,
                   sum(case when tr.from_id = tu.id then 1 else 0 end) as following_num
            from tb_user tu
                     left join
                 tb_relation tr
                 on tr.to_id = tu.id or tr.from_id = tu.id
            where tu.username=#{username}
                and (tu.status = 1 or tu.status = 3)
            group by tu.id;
            """)
    UserVo getByUsername(String username);

}
