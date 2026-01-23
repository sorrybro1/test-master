package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Content;
import org.apache.ibatis.annotations.*;
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

    /**
     * 权限管理页：分页列表（带下载权限）
     * - enabled: 1允许 2不允许（来自 st_role.enabled，默认视为 1）
     * - name: 允许/不允许（给前端显示）
     */
    @Select("""
        <script>
        SELECT
            c.id,
            c.title,
            IFNULL(t.name,'') AS tName,
            IFNULL(r.enabled,'1') AS enabled,
            CASE IFNULL(r.enabled,'1')
                WHEN '2' THEN '不允许'
                ELSE '允许'
            END AS name
        FROM st_content c
        LEFT JOIN st_type t ON c.tid = t.id
        LEFT JOIN st_role r ON r.conuid = CAST(c.id AS CHAR)
        <where>
            <if test="title != null and title != ''">
                AND c.title LIKE CONCAT('%', #{title}, '%')
            </if>
        </where>
        ORDER BY c.id DESC
        LIMIT #{offset}, #{limit}
        </script>
    """)
    List<Map<String, Object>> pageForRole(@Param("offset") int offset,
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
    long countForRole(@Param("title") String title);

    /**
     * 方案A：新增 insertContent(Map) 给 AdminContentController 调用
     * 注意：
     * - codetext / content 是 BLOB，所以用 jdbcType=BLOB 接 byte[]
     * - scode/sdll 存文件路径（/uploads/program/..  /uploads/code/..）
     */
    @Insert("""
        <script>
        INSERT INTO st_content
        (
            tid, pid, title,
            codetext, content,
            scode, sdll,
            creat_time,
            objective, type,
            uid,
            experPurpose, experTheory,
            ariParameter, ariFlow, comResults
        )
        VALUES
        (
            #{tid},
            #{pid},
            #{title},
            #{codetextBytes, jdbcType=BLOB},
            #{contentBytes, jdbcType=BLOB},
            #{scode},
            #{sdll},
            #{creat_time},
            #{objective},
            #{type},
            #{uid},
            #{experPurpose},
            #{experTheory},
            #{ariParameter},
            #{ariFlow},
            #{comResults}
        )
        </script>
    """)
    int insertContent(Map<String, Object> param);


    @Update("""
    <script>
    UPDATE st_content
    SET
        tid = #{tid},
        pid = #{pid},
        title = #{title},
        codetext = #{codetextBytes, jdbcType=BLOB},
        content = #{contentBytes, jdbcType=BLOB},
        scode = #{scode},
        sdll = #{sdll},
        experPurpose = #{experPurpose},
        experTheory = #{experTheory},
        ariParameter = #{ariParameter},
        ariFlow = #{ariFlow},
        comResults = #{comResults}
    WHERE id = #{id}
    </script>
""")
    int updateContent(Map<String, Object> param);

}
