package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("st_orgrole")
public class Orgrole {
    private String userid;
    private String orgid;
}
