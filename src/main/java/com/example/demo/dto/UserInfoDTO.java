// src/main/java/com/experiment/dto/UserInfoDTO.java
package com.example.demo.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Integer id;
    private String uid;
    private String username;
    private String name;
    private Integer role;
    private String roleName;
    private String sex;
    private String college;
    private String profession;
    private String phone;
    private String userClass;

}