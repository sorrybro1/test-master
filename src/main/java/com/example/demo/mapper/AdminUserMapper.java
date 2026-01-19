package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Map;
import java.util.List;

@Mapper
public interface AdminUserMapper {

    @Select("""
    SELECT
        u.id,
        u.uid,
        u.username,
        u.role,
        u.sex,
        u.phone,
        DATE_FORMAT(u.c_time, '%Y-%m-%d %H:%i:%s') AS cTime,
        COALESCE(o.name, u.u_class, '') AS name
    FROM st_user u
    LEFT JOIN st_orgrole r ON r.userid = u.uid
    LEFT JOIN st_org o ON o.id = r.orgid
    WHERE u.role IN (1, 2)
      AND (#{username} IS NULL OR #{username} = '' OR u.username LIKE CONCAT('%', #{username}, '%'))
      AND (#{role} IS NULL OR #{role} = '' OR u.role = #{role})
      AND (#{orgId} IS NULL OR #{orgId} = '' OR o.id = #{orgId})
    ORDER BY u.c_time DESC
    LIMIT #{limit} OFFSET #{offset}
""")
    List<User> pageUsers(@Param("username") String username,
                         @Param("role") String role,
                         @Param("orgId") String orgId,
                         @Param("limit") int limit,
                         @Param("offset") int offset);


    @Select("""
    SELECT COUNT(1)
    FROM st_user u
    LEFT JOIN st_orgrole r ON r.userid = u.uid
    LEFT JOIN st_org o ON o.id = r.orgid
    WHERE u.role IN (1, 2)
      AND (#{username} IS NULL OR #{username} = '' OR u.username LIKE CONCAT('%', #{username}, '%'))
      AND (#{role} IS NULL OR #{role} = '' OR u.role = #{role})
      AND (#{orgId} IS NULL OR #{orgId} = '' OR o.id = #{orgId})
""")
    long countUsers(@Param("username") String username,
                    @Param("role") String role,
                    @Param("orgId") String orgId);


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


