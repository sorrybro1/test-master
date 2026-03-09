package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.OrgCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrgCourseMapper extends BaseMapper<OrgCourse> {

    int deleteByCourseid(@Param("courseid") String id);

}
