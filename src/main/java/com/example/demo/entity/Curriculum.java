package com.example.demo.entity;

import lombok.Data;

@Data
public class Curriculum {
    private String id;
    private String cnumber;
    private String conid;
    private String cname;
    private String cname_url;
    private String tnumber;
    private String cdesc;
    private String semester;
    private String condition;
    private String c_endingtime;
    private String orgid;
    private String orgname;
    private String[] orgids;
    private String sco_ids;
    private String s_start_time;
    private String s_end_time;
    private String a_start_time;
    private String a_end_time;
    private String a_start_time2;
    private String a_end_time2;
    private int limit;
    private int offset;
}
