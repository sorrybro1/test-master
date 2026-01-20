package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminUserMapper {

    @Select("""
    <script>
    SELECT
        u.id,
        u.uid,
        u.username,
        u.role,
        u.sex,
        u.phone,
        DATE_FORMAT(u.c_time, '%Y-%m-%d %H:%i:%s') AS cTime,
        o.id AS orgid,
        COALESCE(o.name, u.u_class, '') AS name
    FROM st_user u
    LEFT JOIN st_orgrole r ON r.userid = u.uid
    LEFT JOIN st_org o ON o.id = r.orgid
    WHERE u.role IN (1, 2)
      <if test="username != null and username != ''">
        AND u.username LIKE CONCAT('%', #{username}, '%')
      </if>
      <if test="role != null and role != ''">
        AND u.role = #{role}
      </if>
      <if test="orgIds != null and orgIds.size() > 0">
        AND o.id IN
        <foreach collection="orgIds" item="oid" open="(" separator="," close=")">
            #{oid}
        </foreach>
      </if>
    ORDER BY u.c_time DESC
    LIMIT #{limit} OFFSET #{offset}
    </script>
    """)
    List<User> pageUsers(@Param("username") String username,
                         @Param("role") String role,
                         @Param("orgIds") List<String> orgIds,
                         @Param("limit") int limit,
                         @Param("offset") int offset);

    @Select("""
    <script>
    SELECT COUNT(1)
    FROM st_user u
    LEFT JOIN st_orgrole r ON r.userid = u.uid
    LEFT JOIN st_org o ON o.id = r.orgid
    WHERE u.role IN (1, 2)
      <if test="username != null and username != ''">
        AND u.username LIKE CONCAT('%', #{username}, '%')
      </if>
      <if test="role != null and role != ''">
        AND u.role = #{role}
      </if>
      <if test="orgIds != null and orgIds.size() > 0">
        AND o.id IN
        <foreach collection="orgIds" item="oid" open="(" separator="," close=")">
            #{oid}
        </foreach>
      </if>
    </script>
    """)
    long countUsers(@Param("username") String username,
                    @Param("role") String role,
                    @Param("orgIds") List<String> orgIds);

    @Select("""
    SELECT
        u.uid       AS uid,
        u.username  AS username,
        u.role      AS role,
        o.id        AS orgid,
        COALESCE(o.name, u.u_class, '') AS name
    FROM st_user u
    LEFT JOIN st_orgrole r ON r.userid = u.uid
    LEFT JOIN st_org o ON o.id = r.orgid
    WHERE u.uid = #{uid}
      AND u.role IN (1, 2)
    LIMIT 1
    """)
    Map<String, Object> getOneForAdmin(@Param("uid") String uid);
}
