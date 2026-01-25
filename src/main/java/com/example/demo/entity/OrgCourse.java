package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_orgcourse")
public class OrgCourse {
    private String orgid;
    private String courseid;
}
