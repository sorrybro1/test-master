package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Content;
import com.example.demo.entity.Type;
import com.example.demo.mapper.ContentMapper;
import com.example.demo.mapper.TypeMapper;
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
import java.util.*;

@RestController
@RequestMapping("/admin-api/contents")
@RequiredArgsConstructor
public class AdminContentEditController {

    private final ContentMapper contentMapper;
    private final TypeMapper typeMapper;

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

    // updateContent.html 需要：{success:true, data:..., types:[...]}
    @GetMapping("/getOne")
    public Map<String, Object> getOne(@RequestParam("id") Integer id, HttpSession session) {
        if (!isAdmin(session)) return Map.of("success", false, "msg", "未登录或无权限");
        if (id == null) return Map.of("success", false, "msg", "id不能为空");

        Content c = contentMapper.selectById(id);
        if (c == null) return Map.of("success", false, "msg", "内容不存在");

        List<Type> types = typeMapper.selectList(new QueryWrapper<Type>().orderByDesc("id"));
        return Map.of("success", true, "data", c, "types", types);
    }

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> update(
            @RequestParam("id") Integer id,
            @RequestParam("tid") Integer tid,
            @RequestParam("title") String title,
            @RequestParam(value = "pid", required = false) Integer pid,
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
        if (id == null) return Map.of("success", false, "msg", "id不能为空");
        if (tid == null) return Map.of("success", false, "msg", "tid不能为空");
        if (title == null || title.trim().isEmpty()) return Map.of("success", false, "msg", "title不能为空");

        Content old = contentMapper.selectById(id);
        if (old == null) return Map.of("success", false, "msg", "内容不存在");

        Map<String, Object> param = new HashMap<>();
        param.put("id", id);
        param.put("tid", tid);
        param.put("pid", pid);
        param.put("title", title.trim());

        // BLOB：避免 CKEditor 未就绪把字段置空：空就保留旧值
        String finalCode = (codetext != null && !codetext.trim().isEmpty())
                ? codetext
                : (old.getCodetext() == null ? "" : old.getCodetext());
        param.put("codetextBytes", finalCode.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String finalContent = (content != null && !content.trim().isEmpty()) ? content : old.getContent();
        param.put("contentBytes", (finalContent == null ? "" : finalContent).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        param.put("experPurpose", pickNonBlank(experPurpose, old.getExperPurpose()));
        param.put("experTheory", pickNonBlank(experTheory, old.getExperTheory()));
        param.put("ariParameter", pickNonBlank(ariParameter, old.getAriParameter()));
        param.put("ariFlow", pickNonBlank(ariFlow, old.getAriFlow()));
        param.put("comResults", pickNonBlank(comResults, old.getComResults()));

        // 文件：没传新文件就保留旧路径
        String scode = old.getScode(); // 源码下载 -> uploads/code
        String sdll = old.getSdll();   // 程序运行 -> uploads/program

        try {
            // import_scode 是程序运行包 -> 存 sdll
            if (programFile != null && !programFile.isEmpty()) {
                sdll = saveToSubDir(programFile, "program");
            }
            // import_sdll 是源码包 -> 存 scode
            if (codeFile != null && !codeFile.isEmpty()) {
                scode = saveToSubDir(codeFile, "code");
            }
        } catch (IOException e) {
            return Map.of("success", false, "msg", "文件保存失败：" + e.getMessage());
        }

        param.put("scode", scode);
        param.put("sdll", sdll);

        int n = contentMapper.updateContent(param);
        return Map.of("success", n > 0, "msg", n > 0 ? "修改成功" : "修改失败");
    }

    private static String pickNonBlank(String v, String fallback) {
        return (v != null && !v.trim().isEmpty()) ? v : fallback;
    }

    private static final SecureRandom RNG = new SecureRandom();

    // ✅ 复用你 create() 的保存风格：存到 uploadDir/subDir，并返回 /uploads/subDir/xxx
    private String saveToSubDir(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            String clean = original.replace("\\", "/");
            clean = clean.substring(clean.lastIndexOf('/') + 1);
            int dot = clean.lastIndexOf('.');
            if (dot >= 0 && dot < clean.length() - 1) ext = clean.substring(dot);
        }

        String name = System.currentTimeMillis() + "_" + (100000 + RNG.nextInt(900000)) + ext;

        Path dir = Paths.get(uploadDir, subDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(name).normalize();
        file.transferTo(target);

        return "/uploads/" + subDir + "/" + name;
    }
}
