package com.example.demo.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_type")
public class Type {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    // desc 是 SQL 关键字，必须加反引号
    @TableField("`desc`")
    private String desc;
    // type 也可能撞关键字，加反引号最稳
    @TableField("`type`")
    private String type;
}
