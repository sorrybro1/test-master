package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("st_score")
public class Score {
    @TableId
    private Long scoId;              // sco_id

    private String cnumber;          // cnumber
    private String snumber;          // snumber

    private Integer degreeExp;
    private Integer degreeOther;
    private Integer totalPoints;

    private String uploadFileName;   // upload_file_name
    private LocalDateTime submissionTime; // submission_time
}
