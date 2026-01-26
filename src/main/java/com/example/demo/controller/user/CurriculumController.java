package com.example.demo.controller.user;

import com.example.demo.dto.CourseStudentRowVO;
import com.example.demo.entity.User;
import com.example.demo.mapper.ScoreMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/curriculum")
@RequiredArgsConstructor
public class CurriculumController {

    private final ScoreMapper stScoreMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "zip", "rar", "7z");

    // ==========================================
    // 1. 下拉框数据接口 (Semester)
    // ==========================================

    /**
     * [补全] 教师/管理员端：获取所有学期下拉列表
     * 对应旧代码: /getSemester
     */
    @PostMapping(value = "/getSemester.do", produces = "application/json;charset=UTF-8")
    public String getSemester() throws Exception {
        // Mapper 返回 List<LocalDateTime>
        List<LocalDateTime> list = stScoreMapper.selectAllSemesters(); // 对应 XML 中的 selectAllSemesters

        // Java 层去重并转为 "2025秋季学期"
        Set<String> semesters = new TreeSet<>(Collections.reverseOrder());
        for (LocalDateTime dt : list) {
            if (dt != null) {
                semesters.add(conversionSemester(dt));
            }
        }

        String current = conversionSemester(LocalDateTime.now());
        StringBuilder sb = new StringBuilder();
        for (String s : semesters) {
            boolean sel = Objects.equals(current, s);
            sb.append("<option value='").append(escapeHtml(s)).append("'")
                    .append(sel ? " selected" : "").append(">")
                    .append(escapeHtml(s)).append("</option>");
        }
        return objectMapper.writeValueAsString(sb.toString());
    }

    /**
     * 学生端：获取该学生有课的学期下拉列表
     * 对应旧代码: /getStdentSemester
     */
    @PostMapping(value = "/getStdentSemester.do", produces = "application/json;charset=UTF-8")
    public String getStdentSemester(HttpSession session) throws Exception {
        String uid = requireUid(session);

        // 【关键修改】这里 Mapper 返回的是 List<LocalDateTime>，不再是 List<String>
        // 如果你的 ScoreMapper.java 接口没改，请也去把它改成 List<LocalDateTime> selectStudentSemesters(...)
        List<LocalDateTime> dateList = stScoreMapper.selectStudentSemesters(uid);

        // Java 层去重并转为 "2025秋季学期"
        Set<String> semesterSet = new TreeSet<>(Collections.reverseOrder());
        for (LocalDateTime date : dateList) {
            if (date != null) {
                semesterSet.add(conversionSemester(date)); // 调用你的转换方法
            }
        }

        String current = conversionSemester(LocalDateTime.now());
        StringBuilder sb = new StringBuilder();
        for (String s : semesterSet) {
            boolean sel = Objects.equals(current, s);
            sb.append("<option value='").append(escapeHtml(s)).append("'")
                    .append(sel ? " selected" : "").append(">")
                    .append(escapeHtml(s)).append("</option>");
        }
        return objectMapper.writeValueAsString(sb.toString());
    }

    /**
     * [修复] 总成绩统计页面的学期/批次下拉
     * 修复了 ClassCastException (LocalDateTime -> String)
     */
    @PostMapping(value = "/getTotalScoreTime", produces = "application/json;charset=UTF-8")
    public String getTotalScoreTime() throws Exception {
        // 1. 接收类型改为 List<LocalDateTime>，因为 MyBatis 返回的是时间对象
        // 注意：如果您的 Mapper 接口定义是 List<String>，请改为 List<LocalDateTime> 或 List<?>，
        // 但通常 MyBatis 实际返回的对象类型由 SQL 和数据库列类型决定。
        List<LocalDateTime> list = stScoreMapper.selectTotalScoreTimes();

        // 2. 使用 Set 去重并排序
        Set<String> semesters = new TreeSet<>(Collections.reverseOrder());

        // 3. 遍历时间对象，转换为字符串 (复用 conversionSemester 方法)
        // 如果 list 为 null 或空，循环会自动跳过
        if (list != null) {
            for (Object obj : list) {
                // 兼容处理：防止某些极端情况下 MyBatis 真的返回了 String 或 Timestamp
                if (obj instanceof LocalDateTime) {
                    semesters.add(conversionSemester((LocalDateTime) obj));
                } else if (obj != null) {
                    // 如果已经是字符串或其他类型，尝试转换或直接使用
                    // 这里假设 conversionSemester 逻辑主要处理 LocalDateTime
                    // 如果偶尔返回 Timestamp，可能需要 obj.toLocalDateTime()
                    // 简单起见，这里假设主要是 LocalDateTime
                    try {
                        semesters.add(conversionSemester((LocalDateTime) obj));
                    } catch (Exception e) {
                        // 兜底：直接 toString
                        semesters.add(obj.toString());
                    }
                }
            }
        }

        // 4. 拼接 HTML 选项
        StringBuilder sb = new StringBuilder();
        for (String s : semesters) {
            // 简单转义防止 XSS
            String safeS = escapeHtml(s);
            sb.append("<option value='").append(safeS).append("'>")
                    .append(safeS).append("</option>");
        }
        return objectMapper.writeValueAsString(sb.toString());
    }

    // ==========================================
    // 2. 课程管理 (Teacher/Admin)
    // ==========================================

    /**
     * [补全] 课程列表管理
     * 对应旧代码: /curriculumList
     */
    // 2. [修复] 教师/管理员：课程列表 (处理字符串 "2025秋季学期" -> 时间范围)
    @PostMapping(value = "/curriculumList.do", produces = "application/json;charset=UTF-8")
    public Map<String, Object> curriculumList(HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String semester = request.getParameter("semester");
            if (semester != null && semester.contains("%")) {
                semester = URLDecoder.decode(semester, StandardCharsets.UTF_8);
            }
            int limit = parseInt(request.getParameter("limit"), 10);
            int offset = parseInt(request.getParameter("offset"), 0);

            // 解析时间范围
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            if (StringUtils.hasText(semester)) {
                Map<String, LocalDateTime> range = parseSemesterToRange(semester);
                if (range != null) {
                    startTime = range.get("start");
                    endTime = range.get("end");
                }
            }

            // 调用 Mapper (Mapper 参数要改成 LocalDateTime)
            long total = stScoreMapper.countCurriculum(startTime, endTime); // XML 需要对应
            List<Map<String, Object>> rows = stScoreMapper.listCurriculum(startTime, endTime, offset, limit); // XML 需要对应

            resp.put("total", total);
            resp.put("rows", rows);
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("total", 0);
            resp.put("rows", Collections.emptyList());
        }
        return resp;
    }

    /**
     * [补全] 查看单门课程详情 (通常用于编辑回显)
     * 对应旧代码: /seeCurriculum
     */
    @PostMapping(value = "/seeCurriculum.do", produces = "application/json;charset=UTF-8")
    public Map<String, Object> seeCurriculum(@RequestParam("id") Long id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            Map<String, Object> curriculum = stScoreMapper.selectCurriculumById(id);
            resp.put("data", curriculum);
            resp.put("success", true);
        } catch (Exception e) {
            resp.put("success", false);
        }
        return resp;
    }

    /**
     * 批量修改课程结课时间
     * 对应旧代码: /updateEndingtime
     * 前端传参: str="cnumber,time>cnumber,time>", semester="2025xxx"
     */
    @PostMapping("/updateEndingtime.do")
    @Transactional(rollbackFor = Exception.class) // 建议添加事务，保证批量更新的一致性
    public Map<String, Object> updateEndingTime(
            @RequestParam("str") String str,
            @RequestParam(value = "semester", required = false) String semester
    ) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (!StringUtils.hasText(str)) {
                resp.put("success", false);
                resp.put("msg", "数据为空");
                return resp;
            }

            // 解析前端传来的格式: "1001,2023-01-01 12:00:00>1002,2023-01-01 12:00:00>"
            String[] items = str.split(">");
            int updateCount = 0;

            for (String item : items) {
                if (item.contains(",")) {
                    // 分割 cnumber 和 time
                    // 注意：如果时间里包含空格，split(",") 也能正常处理，因为 split 默认处理逗号
                    String[] parts = item.split(",");
                    if (parts.length >= 2) {
                        String cNumber = parts[0];
                        String time = parts[1];

                        // 调用 Mapper 更新
                        // 注意：如果您的 Mapper 不需要 semester 参数即可定位课程，可以传 null 或不传
                        // 但为了数据安全，建议带上 semester
                        stScoreMapper.updateCourseEndingTime(cNumber, semester, time);
                        updateCount++;
                    }
                }
            }

            resp.put("success", true);
            resp.put("msg", "成功更新 " + updateCount + " 条记录");
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("success", false);
            resp.put("msg", "系统异常");
        }
        return resp;
    }

    /**
     * 删除课程
     * 对应旧代码: /deleteCurriculum
     */
    @PostMapping("/deleteCurriculum.do")
    public Map<String, Object> deleteCurriculum(@RequestParam("id") Long id) {
        Map<String, Object> resp = new HashMap<>();
        try {
            stScoreMapper.deleteCurriculumById(id);
            resp.put("success", true);
        } catch (Exception e) {
            resp.put("success", false);
        }
        return resp;
    }

    // ==========================================
    // 3. 学生端操作
    // ==========================================

    /**
     * 学生课程列表
     * 对应旧代码: /courseStudentList
     */
    @PostMapping(value = "/courseStudentList.do", produces = "application/json;charset=UTF-8")
    public Map<String, Object> courseStudentList(HttpServletRequest request, HttpSession session,
                                                 @RequestParam(required = false) String semester) throws Exception {
        String uid = requireUid(session);
        if (semester != null && semester.contains("%")) {
            semester = URLDecoder.decode(semester, StandardCharsets.UTF_8);
            if (semester.contains("%")) semester = URLDecoder.decode(semester, StandardCharsets.UTF_8);
        }

        int limit = parseInt(request.getParameter("limit"), 10);
        int offset = parseInt(request.getParameter("offset"), 0);

        // 【核心修改】把 "2025春季学期" -> 转成 时间段
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (StringUtils.hasText(semester)) {
            Map<String, LocalDateTime> range = parseSemesterToRange(semester);
            if (range != null) {
                startTime = range.get("start");
                endTime = range.get("end");
            }
        }

        // 传时间段给 Mapper，而不是传字符串！
        long total = stScoreMapper.countCourseStudent(uid, startTime, endTime);
        List<CourseStudentRowVO> rows = stScoreMapper.listCourseStudent(uid, startTime, endTime, offset, limit);

        // ... 后续 URL 处理代码不变 ...
        String ctx = request.getContextPath();
        for (CourseStudentRowVO r : rows) {
            String url = ctx + "/content/detail.do?id=" + (r.getConid() == null ? "" : r.getConid());
            r.setCnameUrl("<a href='" + url + "'>" + escapeHtml(r.getCname()) + "</a>");
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", total);
        resp.put("rows", rows);
        return resp;
    }
    // 【核心逻辑】根据你的 conversionSemester 逆推的时间范围
    // 3. 辅助方法：解析 "2025秋季学期" -> start, end
    private Map<String, LocalDateTime> parseSemesterToRange(String semesterStr) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(semesterStr);
            if (!m.find()) return null;
            int year = Integer.parseInt(m.group(1));

            LocalDateTime start;
            LocalDateTime end;
            if (semesterStr.contains("春季")) {
                start = LocalDateTime.of(year, 3, 1, 0, 0, 0);
                end = LocalDateTime.of(year, 9, 1, 0, 0, 0);
            } else if (semesterStr.contains("秋季")) {
                start = LocalDateTime.of(year, 9, 1, 0, 0, 0);
                end = LocalDateTime.of(year + 1, 3, 1, 0, 0, 0);
            } else {
                return null;
            }
            return Map.of("start", start, "end", end);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传实验报告
     * 对应旧代码: /uploadReport
     */
    @PostMapping(value = "/uploadReport.do", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadReport(
            HttpSession session,
            @RequestParam("sco_id") Long scoId,
            @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> resp = new HashMap<>();
        try {
            requireUid(session);

            if (file == null || file.isEmpty()) {
                resp.put("msg", "未选择文件");
                return resp;
            }

            String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("");
            String ext = getExt(originalFilename);
            if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
                resp.put("msg", "不支持的文件格式");
                return resp;
            }

            Path reportsDir = Paths.get(uploadDir, "reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }

            String newFileName = genName(ext);
            Path targetPath = reportsDir.resolve(newFileName);
            file.transferTo(targetPath);

            String oldFileName = stScoreMapper.selectUploadFileName(scoId);
            if (StringUtils.hasText(oldFileName)) {
                try {
                    Path oldPath = reportsDir.resolve(oldFileName);
                    if (!Files.exists(oldPath)) {
                        oldPath = Paths.get(uploadDir, oldFileName);
                    }
                    Files.deleteIfExists(oldPath);
                } catch (IOException e) { /*ignore*/ }
            }

            stScoreMapper.updateUploadInfo(scoId, newFileName, LocalDateTime.now());

            resp.put("msg", "提交成功");
            resp.put("fileName", newFileName);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("msg", "系统异常：" + e.getMessage());
            return resp;
        }
    }

    // ==========================================
    // 4. 实验报告评分与管理 (Teacher)
    // ==========================================

    /**
     * 获取实验报告列表
     * 对应旧代码: /laboratoryReportList
     */
    @PostMapping(value = "/laboratoryReportList.do", produces = "application/json;charset=UTF-8")
    public Map<String, Object> laboratoryReportList(HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String cname = request.getParameter("cname");
            String cnumber = request.getParameter("cnumber");
            int limit = parseInt(request.getParameter("limit"), 10);
            int offset = parseInt(request.getParameter("offset"), 0);

            long total = stScoreMapper.countLaboratoryReports(cname, cnumber);
            List<Map<String, Object>> rows = stScoreMapper.listLaboratoryReports(cname, cnumber, offset, limit);

            resp.put("total", total);
            resp.put("rows", rows);
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("total", 0);
            resp.put("rows", Collections.emptyList());
        }
        return resp;
    }

    /**
     * 保存评分 (接收 JSON 列表)
     * 对应前端: JSON.stringify(scoreList)
     */
    @PostMapping("/saveScoring.do")
    @Transactional(rollbackFor = Exception.class)
    // 【关键】使用 @RequestBody 接收 JSON 数组
    public Map<String, Object> saveScoring(@RequestBody List<Map<String, Object>> scoreList) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (scoreList == null || scoreList.isEmpty()) {
                resp.put("msg", "未接收到数据");
                return resp;
            }

            // 遍历列表进行更新
            for (Map<String, Object> item : scoreList) {
                // 安全获取数据
                Long scoId = Long.valueOf(item.get("sco_id").toString());

                // 处理分数，防止空指针
                Double degreeExp = item.get("degree_exp") == null ? 0.0 : Double.valueOf(item.get("degree_exp").toString());
                Double degreeOther = item.get("degree_other") == null ? 0.0 : Double.valueOf(item.get("degree_other").toString());

                // 自动计算总分
                Double totalPoints = degreeExp + degreeOther;

                // 执行更新
                stScoreMapper.updateScore(scoId, degreeExp, degreeOther, totalPoints);
            }

            resp.put("msg", "保存成功");
            resp.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("msg", "保存失败：" + e.getMessage());
            resp.put("success", false);
        }
        return resp;
    }
    /**
     * [补全] 批量打包下载实验报告 (ZIP)
     * 对应旧代码: /downloadCheckLaboratoryReport
     */
    @GetMapping("/downloadCheckLaboratoryReport.do")
    public void downloadCheckLaboratoryReport(
            HttpServletResponse response,
            // 假设前端传递的是逗号分隔的 ID 字符串，或者 scoop_ids[]
            @RequestParam(value = "sco_id", required = false) String scoIdsStr
    ) {
        try {
            if (!StringUtils.hasText(scoIdsStr)) return;

            List<Long> idList = Arrays.stream(scoIdsStr.split(","))
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .toList();

            List<Map<String, String>> fileList = stScoreMapper.selectFilesByScoIds(idList);

            String zipName = "LaboratoryReport_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".zip";
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipName, StandardCharsets.UTF_8));

            try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
                Path reportsBase = Paths.get(uploadDir, "reports");
                Set<String> addedEntries = new HashSet<>();

                for (Map<String, String> item : fileList) {
                    String storedName = item.get("upload_file_name");
                    if (storedName == null || storedName.isEmpty()) continue;

                    // 构造文件名: 学号_姓名_课程名.ext
                    String entryName = item.get("snumber") + "_" + item.get("sname") + "_" + item.get("cname") + "." + getExt(storedName);

                    Path filePath = reportsBase.resolve(storedName);
                    if (!Files.exists(filePath)) filePath = Paths.get(uploadDir, storedName);

                    if (Files.exists(filePath)) {
                        if (addedEntries.contains(entryName)) {
                            entryName = System.currentTimeMillis() + "_" + entryName;
                        }
                        addedEntries.add(entryName);
                        ZipEntry zipEntry = new ZipEntry(entryName);
                        zos.putNextEntry(zipEntry);
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                    }
                }
                zos.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * [补全] 单个下载实验报告
     * 对应旧代码: /downloadLaboratoryReport
     */
    @GetMapping("/downloadLaboratoryReport.do")
    public void downloadLaboratoryReport(HttpServletResponse response, @RequestParam("sco_id") Long scoId) {
        try {
            // 获取单个文件信息
            Map<String, String> fileInfo = stScoreMapper.selectFileByScoId(scoId);
            if (fileInfo == null) return;

            String storedName = fileInfo.get("upload_file_name");
            if (!StringUtils.hasText(storedName)) return;

            Path filePath = Paths.get(uploadDir, "reports", storedName);
            if (!Files.exists(filePath)) filePath = Paths.get(uploadDir, storedName);

            if (Files.exists(filePath)) {
                // 下载名：学号_姓名_课程名.ext
                String dlName = fileInfo.get("snumber") + "_" + fileInfo.get("sname") + "_" + fileInfo.get("cname") + "." + getExt(storedName);

                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(dlName, StandardCharsets.UTF_8));

                Files.copy(filePath, response.getOutputStream());
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // 5. 总成绩管理
    // ==========================================

    /**
     * [补全] 获取总成绩列表
     * 对应旧代码: /getTotalScoreList
     */
    @PostMapping(value = "/getTotalScoreList", produces = "application/json;charset=UTF-8")
    public Map<String, Object> getTotalScoreList(HttpServletRequest request) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String time = request.getParameter("time"); // 学期/批次
            int limit = parseInt(request.getParameter("limit"), 10);
            int offset = parseInt(request.getParameter("offset"), 0);

            // 需统计总记录数 + 数据
            long total = stScoreMapper.countTotalScores(time);
            List<Map<String, Object>> rows = stScoreMapper.listTotalScores(time, offset, limit);

            resp.put("total", total);
            resp.put("rows", rows);
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("total", 0);
            resp.put("rows", Collections.emptyList());
        }
        return resp;
    }

    /**
     * 导出总成绩
     * 对应旧代码: /excelTotalScore
     */
    @GetMapping("/excelTotalScore")
    public void excelTotalScore(HttpServletResponse response) {
        try {
            List<Map<String, Object>> list = stScoreMapper.selectTotalScoreForExport();
            try (HSSFWorkbook wb = new HSSFWorkbook()) {
                HSSFSheet sheet = wb.createSheet("总成绩");
                String[] headers = {"序号", "学号", "姓名", "学院", "专业", "班级", "实验", "分数"};
                HSSFRow headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.length; i++) {
                    headerRow.createCell(i).setCellValue(headers[i]);
                }
                int rowIdx = 1;
                for (Map<String, Object> map : list) {
                    HSSFRow row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(rowIdx - 1);
                    row.createCell(1).setCellValue(val(map.get("snumber")));
                    row.createCell(2).setCellValue(val(map.get("sname")));
                    row.createCell(3).setCellValue(val(map.get("college")));
                    row.createCell(4).setCellValue(val(map.get("profession")));
                    row.createCell(5).setCellValue(val(map.get("class_name")));
                    row.createCell(6).setCellValue(val(map.get("cname")));
                    row.createCell(7).setCellValue(val(map.get("total_points")));
                }
                String fileName = "总成绩_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xls";
                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
                wb.write(response.getOutputStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // 工具方法
    // ==========================================

    private String requireUid(HttpSession session) {
        Object isLogin = session.getAttribute("isLogin");
        if (!(isLogin instanceof Boolean) || !((Boolean) isLogin)) {
            throw new RuntimeException("未登录");
        }
        Object u = session.getAttribute("user");
        if (u instanceof User user && user.getUid() != null) {
            return user.getUid();
        }
        throw new RuntimeException("未登录");
    }

    private String genName(String ext) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return ts + "_" + rand + "." + ext;
    }

    private String getExt(String filename) {
        String clean = StringUtils.cleanPath(filename);
        int idx = clean.lastIndexOf('.');
        if (idx < 0 || idx == clean.length() - 1) return "";
        return clean.substring(idx + 1);
    }

    private String conversionSemester(LocalDateTime t) {
        int year = t.getYear();
        int month = t.getMonthValue();
        if (month > 2 && month < 9) return year + "春季学期";
        if (month > 8 && month < 13) return year + "秋季学期";
        if (month > 0 && month < 3) return (year - 1) + "秋季学期";
        return "";
    }

    private int parseInt(String v, int def) {
        try { return Integer.parseInt(v); } catch (Exception e) { return def; }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String val(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}