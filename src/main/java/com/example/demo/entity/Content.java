package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.nio.charset.StandardCharsets;

@Data
@TableName("st_content")
public class Content {
    private static final long serialVersionUID = 5741824657396366939L;
    private int id;
    private int tid;
    private int pid;
    private String title;
//    private String codetext;
//    private String content;
// 1) 数据库真实字段：BLOB -> byte[]
    @JsonIgnore
    @TableField("codetext")
    private byte[] codetextBytes;

    @JsonIgnore
    @TableField("content")
    private byte[] contentBytes;
    private String scode;
    private String sdll;
    private String creat_time;
    private String objective;
    private String type;
    @TableField(exist = false)
    private String tName;
    @TableField("experPurpose")
    private String experPurpose;
    @TableField("experTheory")
    private String experTheory;
    @TableField("ariParameter")
    private String ariParameter;
    @TableField("ariFlow")
    private String ariFlow;
    @TableField("comResults")
    private String comResults;


    // 2) 对外 JSON 字段：解码后的字符串（前端仍然用 data.codetext/data.content）
    @JsonProperty("codetext")
    public String getCodetext() {
        return codetextBytes == null ? "" : new String(codetextBytes, StandardCharsets.UTF_8);
    }

    @JsonProperty("content")
    public String getContent() {
        return contentBytes == null ? "" : new String(contentBytes, StandardCharsets.UTF_8);
    }
}
