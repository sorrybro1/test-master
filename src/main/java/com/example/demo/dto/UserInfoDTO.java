// src/main/java/com/experiment/dto/UserInfoDTO.java
package com.example.demo.dto;
//admin
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
    private String professional;
    private String prof;
    private String phone;
    private String userClass;

}