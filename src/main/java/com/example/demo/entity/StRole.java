package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 下载权限表：st_role
 * conuid：实验/内容ID（这里沿用老系统字段名，实际对应 st_content.id）
 * enabled：1允许，2不允许
 */
@Data
@TableName("st_role")
public class StRole {
    private Integer id;
    private String conuid;
    private String enabled;
}
