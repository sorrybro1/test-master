package com.example.demo.controller.admin;

import com.example.demo.mapper.ContentMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/admin-api/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentMapper contentMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private boolean isAdmin(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        if (!(adminLogin instanceof Boolean) || !((Boolean) adminLogin)) return false;
        Object roleObj = session.getAttribute("role");
        int role = -1;
        try { role = Integer.parseInt(String.valueOf(roleObj)); } catch (Exception ignored) {}
        return role == 0 || role == 3;
    }

    // bootstrap-table 分页：返回 {total, rows}
    @PostMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "10") int limit,
                                    @RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(required = false) String title,
                                    HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("total", 0, "rows", List.of());
        }
        String t = (title == null) ? "" : title.trim();

        long total = contentMapper.countForAdmin(t);
        List<Map<String, Object>> rows = contentMapper.pageForAdmin(offset, limit, t);

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("rows", rows);
        return res;
    }

    /**
     * 新增内容（支持上传：程序运行文件 + 源码下载文件）
     * - 程序运行文件保存到 uploads/program
     * - 源码下载文件保存到 uploads/code
     * 数据库中保存可访问路径：/uploads/program/xxx 和 /uploads/code/xxx
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> create(
            @RequestParam("tid") Integer tid,
            @RequestParam("title") String title,
            @RequestParam(value = "pid", required = false) Integer pid,
            @RequestParam(value = "objective", required = false) String objective,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "codetext", required = false) String codetext,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "experPurpose", required = false) String experPurpose,
            @RequestParam(value = "experTheory", required = false) String experTheory,
            @RequestParam(value = "ariParameter", required = false) String ariParameter,
            @RequestParam(value = "ariFlow", required = false) String ariFlow,
            @RequestParam(value = "comResults", required = false) String comResults,
            @RequestParam(value = "import_scode", required = false) MultipartFile programFile,
            @RequestParam(value = "import_sdll", required = false) MultipartFile codeFile,
            HttpSession session
    ) {
        if (!isAdmin(session)) return Map.of("success", false, "msg", "未登录或无权限");
        if (tid == null) return Map.of("success", false, "msg", "tid不能为空");
        if (title == null || title.trim().isEmpty()) return Map.of("success", false, "msg", "title不能为空");

        Map<String, Object> param = new HashMap<>();
        param.put("tid", tid);
        param.put("pid", pid);
        param.put("title", title.trim());
        param.put("objective", objective);
        param.put("type", type);
        param.put("experPurpose", experPurpose);
        param.put("experTheory", experTheory);
        param.put("ariParameter", ariParameter);
        param.put("ariFlow", ariFlow);
        param.put("comResults", comResults);

        // BLOB 字段（codetext / content）
        param.put("codetextBytes", codetext == null ? null : codetext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        param.put("contentBytes", content == null ? null : content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // 当前登录管理员 uid（你登录时 session 里保存了 uid）
        Object uidObj = session.getAttribute("uid");
        Integer uid = null;
        try { uid = uidObj == null ? null : Integer.parseInt(String.valueOf(uidObj)); } catch (Exception ignored) {}
        param.put("uid", uid);

        // 创建时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        param.put("creat_time", now);

        // 保存上传文件，并写入 scode / sdll
        try {
            String programPath = saveToSubDir(programFile, "program");
            String codePath = saveToSubDir(codeFile, "code");
            param.put("scode", codePath); // 源码下载
            param.put("sdll", programPath); // 程序运行
        } catch (IOException e) {
            return Map.of("success", false, "msg", "文件保存失败：" + e.getMessage());
        }

        int n = contentMapper.insertContent(param);
        return Map.of("success", n > 0, "msg", n > 0 ? "新增成功" : "新增失败");
    }

    private static final SecureRandom RNG = new SecureRandom();

    private String saveToSubDir(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            String clean = original.replace("\\", "/");
            clean = clean.substring(clean.lastIndexOf('/') + 1);
            int dot = clean.lastIndexOf('.');
            if (dot >= 0 && dot < clean.length() - 1) {
                ext = clean.substring(dot);
            }
        }
        String name = System.currentTimeMillis() + "_" + (100000 + RNG.nextInt(900000)) + ext;

        Path dir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name).normalize();
        file.transferTo(target);

        // 访问路径：/uploads/...  （WebConfig 已映射到 file:uploads/）
        return "/uploads/" + subDir + "/" + name;
    }

    @PostMapping("/deleteOne")
    public Map<String, Object> deleteOne(@RequestParam("id") Integer id, HttpSession session) {
        if (!isAdmin(session)) return Map.of("success", false, "msg", "未登录或无权限");
        if (id == null) return Map.of("success", false, "msg", "id不能为空");

        int n = contentMapper.deleteById(id);
        return Map.of("success", n > 0, "msg", n > 0 ? "删除成功" : "删除失败");
    }

    // ids=1,2,3
    @PostMapping("/deleteBatch")
    public Map<String, Object> deleteBatch(@RequestParam("ids") String ids, HttpSession session) {
        if (!isAdmin(session)) return Map.of("success", false, "msg", "未登录或无权限");
        if (ids == null || ids.trim().isEmpty()) return Map.of("success", false, "msg", "ids不能为空");

        List<Integer> idList = new ArrayList<>();
        for (String s : ids.split(",")) {
            if (s == null || s.trim().isEmpty()) continue;
            try { idList.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
        }
        if (idList.isEmpty()) return Map.of("success", false, "msg", "ids格式不正确");

        int n = contentMapper.deleteBatchIds(idList);
        return Map.of("success", true, "msg", "批量删除成功，删除数量：" + n);
    }
}
