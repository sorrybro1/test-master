package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_org")
public class Org {
    private String id;
    private String name;
    private String pId;
    private String uid;
}
