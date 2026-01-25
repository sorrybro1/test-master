package com.example.demo.controller.index;
//admin
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.*;
import com.example.demo.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/content")
public class ContentController {
    @Autowired
    private ContentMapper contentMapper;
    @Autowired
    private CoureseMapper coureseMapper;
    @Autowired
    private OrgCourseMapper orgCourseMapper;
    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 1. 获取内容列表接口
     * URL: /content/list?tid=1
     */
    @PostMapping("/list")
    @ResponseBody
    public List<Content> getContentList(@RequestParam(value = "tid", required = false) String tid) {
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        if (tid != null && !tid.isEmpty()) {
            wrapper.eq("tid", tid);
        }
        // 只查询必要的字段
        wrapper.select("id", "title", "tid");
        wrapper.orderByAsc("id");
        return contentMapper.selectList(wrapper);
    }

    @PostMapping("/detail")
    @ResponseBody
    public Content getContentDetail(@RequestParam("id") String id) {
        return contentMapper.selectById(id);
    }

    /**
     * 2. 获取学期下拉框选项
     */
    @PostMapping("/getSemester")
    @ResponseBody
    public String getSemester() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(new Date());

        int year = Integer.parseInt(dateStr.substring(0, 4));
        // 简单模拟旧系统逻辑：生成 "当前年-下一年" 的学期选项
        String option1 = year + "-" + (year + 1) + "年秋季学期";
        String option2 = (year + 1) + "-" + (year + 2) + "年春季学期";

        StringBuilder html = new StringBuilder();
        html.append("<option value='").append(option1).append("'>").append(option1).append("</option>");
        html.append("<option value='").append(option2).append("'>").append(option2).append("</option>");

        try {
            // 使用 Jackson 将字符串序列化为 JSON 格式的字符串 (例如: "<option>...</option>")
            // 前端 $.parseJSON 需要这种格式
            return objectMapper.writeValueAsString(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 3. 开设实验 (核心业务逻辑)
     */
    @PostMapping("/establish")
    @ResponseBody
    public Map<String, Object> establish(
            @RequestParam("uid") String uids,          // 选中的实验内容ID，逗号分隔
            @RequestParam("semester") String semesterStr, // 学期字符串
            @RequestParam("orgid") String orgId) {     // 班级ID

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("success", false);

        try {
            // 1. 解码学期参数
            if (semesterStr != null) {
                semesterStr = URLDecoder.decode(semesterStr, "UTF-8"); //
            }

            // 2. 解析学期字符串为 LocalDateTime (因为 Courese 实体要求 LocalDateTime)
            LocalDateTime semesterDate = parseSemesterString(semesterStr);

            // 3. 分割实验ID
            String[] contentIds = uids.split(",");

            // 4. 获取该班级下的所有学生 (用于初始化成绩表)
            // 假设 User 表中有 org_id 字段
            QueryWrapper<User> userWrapper = new QueryWrapper<>();
            userWrapper.eq("u_class", orgId); // 请根据实际数据库字段调整 "orgid"
            List<User> students = userMapper.selectList(userWrapper);

            // 5. 遍历每一个选中的实验进行开设
            for (String contentId : contentIds) {
                if (contentId == null || contentId.trim().isEmpty()) continue;

                Content content = contentMapper.selectById(contentId);
                if (content == null) continue;

                // --- 步骤A：创建课程记录 (st_course) ---
                Courese course = new Courese();
                String cNumber = UUID.randomUUID().toString().replace("-", ""); // 生成课程主键

                course.setCnumber(cNumber);
                // content.getId() 是String, Courese.conid 是Integer，需要转换
                try {
                    course.setConid(Integer.parseInt(String.valueOf(content.getId())));
                } catch (NumberFormatException e) {
                    System.err.println("Content ID 非数字，无法存入 conid: " + content.getId());
                    continue;
                }
                course.setCname(content.getTitle());
                course.setSemester(semesterDate);
                // course.setTnumber(currentUser.getId()); // 如果有登录用户，这里设置教师ID

                coureseMapper.insert(course);

                // --- 步骤B：关联班级 (OrgCourse) ---
                OrgCourse orgCourse = new OrgCourse();
                orgCourse.setOrgid(orgId);
                orgCourse.setCourseid(cNumber); // 关联刚刚生成的 cnumber
                orgCourseMapper.insert(orgCourse);

                // --- 步骤C：初始化学生成绩 (st_score) ---
                if (students != null && !students.isEmpty()) {
                    for (User student : students) {
                        Score score = new Score();
                        score.setCnumber(cNumber);
                        // 假设 User 实体的主键或学号字段是 snumber 或 id
                        // score.setSnumber(student.getSnumber());
                        // 如果 User 只有 id (Integer/String)，这里需要转为 score 需要的 String snumber
                        score.setSnumber(String.valueOf(student.getId()));

                        scoreMapper.insert(score); // (addScore)
                    }
                }
            }

            resultMap.put("success", true);
            resultMap.put("msg", "开设成功");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            resultMap.put("msg", "字符编码异常");
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("msg", "开设失败: " + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 辅助方法：将学期字符串 (如 "2025-2026年秋季学期") 解析为 LocalDateTime
     * 策略：
     * - 秋季 -> 当年 9月1日
     * - 春季 -> 次年 3月1日
     */
    private LocalDateTime parseSemesterString(String semesterStr) {
        if (semesterStr == null) return LocalDateTime.now();

        try {
            // 提取年份 (假设字符串以年份开头，如 "2025-...")
            Pattern pattern = Pattern.compile("^(\\d{4})");
            Matcher matcher = pattern.matcher(semesterStr);
            int year = LocalDateTime.now().getYear();
            if (matcher.find()) {
                year = Integer.parseInt(matcher.group(1));
            }

            if (semesterStr.contains("秋") || semesterStr.contains("Autumn")) {
                // 秋季：设为当年的 9月1日
                return LocalDateTime.of(year, 9, 1, 0, 0);
            } else if (semesterStr.contains("春") || semesterStr.contains("Spring")) {
                // 春季：通常指跨年后的春季，如果是 "2025-2026春"，通常指2026春
                // 简单处理：设为当年的 3月1日 (或者 year+1，取决于你的业务定义)
                // 按照 getSemester 的逻辑 "2025-2026春" 通常指 2026年3月
                return LocalDateTime.of(year + 1, 3, 1, 0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return LocalDateTime.now(); // 解析失败返回当前时间
    }
}

