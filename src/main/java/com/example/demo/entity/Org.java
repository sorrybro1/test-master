package com.example.demo.entity;
//admin
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_org")
public class Org {
    private String id;
    private String name;

    @TableField("pId")
    private String pId;

    private String uid;
}
