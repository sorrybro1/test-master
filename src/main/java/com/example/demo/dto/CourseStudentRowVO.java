package com.example.demo.dto;

import lombok.Data;

@Data
public class CourseStudentRowVO {
    private Long scoId;             // sco_id
    private String uploadFileName;  // upload_file_name
    private String cEndingtime;     // c_endingtime

    private String cnumber;
    private Integer conid;
    private String cname;
    private Long tid;

    // 前端 field='cname_url' 需要这个字段
    private String cnameUrl;
}
