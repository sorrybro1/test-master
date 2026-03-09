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
@RequestMapping("/api/content")
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
        return contentMapper.getListByTid(tid);
        //return contentMapper.selectList(wrapper);
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
        // 1. 获取当前时间的年和月 (采用第二个方法的逻辑)
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String ctime = sdf.format(date);

        int y = Integer.parseInt(ctime.substring(0, 4));
        int m = Integer.parseInt(ctime.substring(5, 7));

        List<String> list = new ArrayList<>();

        // 2. 根据月份生成学期列表 (完全采用第二个方法的逻辑)
        if (m >= 1 && m <= 2) {
            list.add((y - 1) + "年秋季学期");
            for (int i = 0; i < 4; ++i) {
                list.add((y + i) + "年春季学期");
                list.add((y + i) + "年秋季学期");
            }
            list.add((y + 4) + "年春季学期");
        } else if (m >= 3 && m <= 8) {
            for (int i = 0; i <= 4; ++i) {
                list.add((y + i) + "年春季学期");
                list.add((y + i) + "年秋季学期");
            }
        } else if (m >= 9 && m <= 12) {
            list.add(y + "年秋季学期");
            for (int i = 1; i <= 4; ++i) {
                list.add((y + i) + "年春季学期");
                list.add((y + i) + "年秋季学期");
            }
            list.add((y + 5) + "年春季学期");
        }

        // 3. 构建 HTML 字符串
        StringBuilder html = new StringBuilder();
        for (String s : list) {
            html.append("<option value='").append(s).append("'>").append(s).append("</option>");
        }

        // 4. 保持原函数的返回格式 (Jackson 序列化)
        try {
            // 前端 $.parseJSON 需要这种 JSON 字符串格式
            return objectMapper.writeValueAsString(html.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 3. 开设实验 (完整版：包含查重、多班级支持、复用逻辑)
     */
    @PostMapping("/establish")
    @ResponseBody
    public Map<String, Object> establish(
            @RequestParam("uid") String uids,          // 选中的实验内容ID，逗号分隔
            @RequestParam("semester") String semesterStr, // 学期字符串
            @RequestParam("orgid") String orgIdsStr) {    // 班级ID，逗号分隔

        Map<String, Object> resultMap = new HashMap<>();

        // 统计计数
        int successCount = 0;
        int totalOps = 0; // 总操作数 (实验数 * 班级数)
        StringBuilder msgBuilder = new StringBuilder();

        try {
            // 1. 解码与参数处理
            if (semesterStr != null && semesterStr.contains("%")) {
                semesterStr = URLDecoder.decode(semesterStr, "UTF-8");
            }

            // 解析学期时间 (统一使用 parseSemesterToDate 方法生成的固定时间点)
            LocalDateTime semesterDate = parseSemesterToDate(semesterStr);

            String[] contentIdArr = uids.split(",");
            String[] orgIdArr = orgIdsStr.split(",");

            totalOps = contentIdArr.length * orgIdArr.length;

            // 2. 外层循环：遍历选中的每一个实验
            for (String contentId : contentIdArr) {
                if (contentId == null || contentId.trim().isEmpty()) continue;

                Content content = contentMapper.selectById(contentId);
                if (content == null) continue;

                // --- 步骤A：检查课程是否已存在 (查重逻辑) ---
                // 在同一学期、同一实验内容下，是否已有课程记录？
                QueryWrapper<Courese> courseQuery = new QueryWrapper<>();
                courseQuery.eq("conid", contentId); // 注意类型匹配，如果是int需转换
                courseQuery.eq("semester", semesterDate);
                // courseQuery.eq("tnumber", currentUser.getUid()); // 可选：是否区分不同老师开的同一门课？旧代码似乎没严格区分，这里暂不加

                Courese existingCourse = coureseMapper.selectOne(courseQuery);

                String cNumber;
                boolean isNewCourse = false;

                if (existingCourse != null) {
                    // 场景1：课程已存在 -> 复用 cnumber
                    cNumber = existingCourse.getCnumber();
                } else {
                    // 场景2：课程不存在 -> 创建新课程
                    isNewCourse = true;
                    cNumber = UUID.randomUUID().toString().replace("-", "");

                    Courese newCourse = new Courese();
                    newCourse.setCnumber(cNumber);
                    try {
                        newCourse.setConid(Integer.parseInt(String.valueOf(content.getId())));
                    } catch (Exception e) {
                        newCourse.setConid(Integer.parseInt(contentId));
                    }
                    newCourse.setCname(content.getTitle());
                    newCourse.setSemester(semesterDate);
                    // newCourse.setTnumber(user.getUid()); // 如有登录信息需设置

                    coureseMapper.insert(newCourse);
                }

                // 3. 内层循环：遍历选中的每一个班级
                for (String orgId : orgIdArr) {
                    if (orgId == null || orgId.trim().isEmpty()) continue;

                    // --- 步骤B：检查班级是否已关联该课程 (班级查重) ---
                    QueryWrapper<OrgCourse> orgCourseQuery = new QueryWrapper<>();
                    orgCourseQuery.eq("courseid", cNumber);
                    orgCourseQuery.eq("orgid", orgId);

                    Long count = orgCourseMapper.selectCount(orgCourseQuery);

                    if (count > 0) {
                        // 已开设过，记录提示
                        // 只有当课程不是新创建的，才提示“已开设”
                        if (!isNewCourse) {
                            // 简单提示即可，避免消息过长
                            msgBuilder.append(content.getTitle()).append("在班级[").append(orgId).append("]已开设; ");
                        }
                    } else {
                        // --- 步骤C：关联班级并初始化成绩 ---

                        // 1. 插入班级关联 (st_orgcourse)
                        OrgCourse orgCourse = new OrgCourse();
                        orgCourse.setOrgid(orgId);
                        orgCourse.setCourseid(cNumber);
                        orgCourseMapper.insert(orgCourse);

                        // 2. [关键修复] 获取该班级学生
                        // 原错误逻辑: userWrapper.eq("u_class", orgId);
                        // 修正逻辑: 通过 st_orgrole 中间表查找学生
                        QueryWrapper<User> userWrapper = new QueryWrapper<>();

                        // 使用 SQL 子查询：SELECT userid FROM st_orgrole WHERE orgid = '...'
                        // 对应 st_user.uid IN ( ... )
                        userWrapper.inSql("uid", "SELECT userid FROM st_orgrole WHERE orgid = '" + orgId + "'");

                        // 建议加上角色过滤，只查学生 (假设 role=1 是学生，根据 st_user 表注释)
                        userWrapper.eq("role", 1);

                        List<User> students = userMapper.selectList(userWrapper);

                        // 3. 批量插入初始成绩记录
                        if (students != null && !students.isEmpty()) {
                            for (User student : students) {
                                Score score = new Score();
                                score.setCnumber(cNumber);
                                score.setSnumber(String.valueOf(student.getUid())); // 注意是用 uid 还是 id，通常关联表存的是 uid
                                scoreMapper.insert(score);
                            }
                        }
                        successCount++;
                    }
                }
            }

            // 4. 构造返回消息
            resultMap.put("success", true); // 只要没有系统异常，通常都算success，具体看msg
            if (successCount == totalOps) {
                resultMap.put("msg", "开设成功");
            } else if (successCount == 0) {
                resultMap.put("msg", "所有选中的实验在对应班级均已开设，无需重复操作。");
            } else {
                resultMap.put("msg", "部分开设成功。" + msgBuilder.toString());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            resultMap.put("success", false);
            resultMap.put("msg", "编码异常");
        } catch (Exception e) {
            e.printStackTrace();
            resultMap.put("success", false);
            resultMap.put("msg", "系统异常: " + e.getMessage());
        }

        return resultMap;
    }

    /**
     * 辅助方法：统一学期时间解析逻辑
     * 保持与 XML 查询逻辑一致，将"2025xxx"转为固定的时间点存储
     */
    private LocalDateTime parseSemesterToDate(String semesterStr) {
        // 默认当前时间
        LocalDateTime now = LocalDateTime.now();
        if (semesterStr == null) return now;

        try {
            // 提取年份
            Pattern pattern = Pattern.compile("^(\\d{4})");
            Matcher matcher = pattern.matcher(semesterStr);
            int year = now.getYear();
            if (matcher.find()) {
                year = Integer.parseInt(matcher.group(1));
            }

            // 根据 stContentCtr.java 的逻辑 (旧系统习惯)
            // 春季 -> 06-01 (或者使用新逻辑的 03-01，只要存取一致即可)
            // 这里建议使用新逻辑的月份，但必须精确到日，以便 equals 查询能命中
            if (semesterStr.contains("秋") || semesterStr.contains("Autumn")) {
                // 秋季：当年 9月1日
                return LocalDateTime.of(year, 9, 1, 0, 0, 0);
            } else if (semesterStr.contains("春") || semesterStr.contains("Spring")) {
                // 春季：通常指跨年后的春季。如果下拉框是 "2025-2026春"，那应该是 2026年3月
                // 但如果下拉框仅显示 "2025春"，则可能是 2025年3月
                // 根据 getSemester 逻辑：
                // Option: (y-1) + "年秋"
                // Option: y + "年春"
                // 假设前端传的是 "2026年春季学期" (year=2026) -> 2026-03-01
                return LocalDateTime.of(year, 3, 1, 0, 0, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return now;
    }
}

