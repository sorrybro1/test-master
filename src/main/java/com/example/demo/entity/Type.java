package com.example.demo.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_type")
public class Type {
    private int id;
    private String name;
    private String desc;
    private String type;
}
