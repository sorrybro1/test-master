package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

@TableName("st_type")
public class Classification implements Serializable {
    private static final long serialVersionUID = -1103785429552175035L;

    // 建议加上 @TableId 明确主键
    @TableId
    public String id;

    public String name;

    // desc 是 SQL 排序关键字，必须加反引号
    @TableField("`desc`")
    public String desc;

    // type 有时也是关键字，加反引号最安全
    @TableField("`type`")
    public String type;



    // Getter 和 Setter 方法保持不变
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }


}