package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    @Select("""
        <script>
        SELECT
            c.id,
            c.title,
            IFNULL(t.name,'') AS tName,
            DATE_FORMAT(c.creat_time, '%Y-%m-%d %H:%i:%s') AS creat_time
        FROM st_content c
        LEFT JOIN st_type t ON c.tid = t.id
        <where>
            <if test="title != null and title != ''">
                AND c.title LIKE CONCAT('%', #{title}, '%')
            </if>
        </where>
        ORDER BY c.id DESC
        LIMIT #{offset}, #{limit}
        </script>
    """)
    List<Map<String, Object>> pageForAdmin(@Param("offset") int offset,
                                           @Param("limit") int limit,
                                           @Param("title") String title);

    @Select("""
        <script>
        SELECT COUNT(1)
        FROM st_content c
        <where>
            <if test="title != null and title != ''">
                AND c.title LIKE CONCAT('%', #{title}, '%')
            </if>
        </where>
        </script>
    """)
    long countForAdmin(@Param("title") String title);
}
