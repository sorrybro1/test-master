//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("st_user")
public class User implements Serializable {
    private static final long serialVersionUID = 4398649106711709696L;
    public static final String PRINCIPAL_ATTRIBUTE_NAME = User.class.getName() + ".PRINCIPAL";
    private int id;
    private String uid;
    private String username;
    private String passwd;
    private int role;
    private String c_time;
    private String name;
    private String sex;
    private String prof;
    private String college;
    private String professional;
    private String u_class;
    private String phone;
    @TableField(exist = false)
    private String org_id;
    @TableField(exist = false)
    private String org_name;
    @TableField(exist = false)
    private String session_id;

    @TableField(exist = false)
    private int limit;
    @TableField(exist = false)
    private int offset;

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.uid == null ? 0 : this.uid.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            User other = (User)obj;
            if (this.uid == null) {
                if (other.uid != null) {
                    return false;
                }
            } else if (!this.uid.equals(other.uid)) {
                return false;
            }

            return true;
        }
    }
    public String getRoleName() {
        return switch (role) {
            case 0 -> "超级管理员";
            case 1 -> "学生";
            case 2 -> "老师";
            case 3 -> "管理员";
            default -> "未知";
        };
    }
}
