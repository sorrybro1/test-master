package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("st_course")
public class Courese {
    private Integer id;

    @TableId
    private String cnumber;     // cnumber（旧系统用它做课程主键）

    private Integer conid;      // conid（关联 st_content.id）
    private String cname;       // cname

    private String tnumber;
    private String cdesc;

    private LocalDateTime semester;  // semester (datetime)
    private String cEndingtime;
}
