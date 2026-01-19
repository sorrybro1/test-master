package com.example.demo.dto;

import lombok.Data;

@Data
public class AdminUpdatePasswordRequest {
    private String uid;          // 用户ID
    private String oldPassword;  // 旧密码（可选，看需求）
    private String newPassword;  // 新密码
}