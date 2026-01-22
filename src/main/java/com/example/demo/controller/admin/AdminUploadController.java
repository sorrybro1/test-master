package com.example.demo.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CKEditor / 管理端通用上传接口
 *
 * 1) 图片上传：POST /admin-api/uploads/image
 *    CKEditor 4 默认字段名为 upload
 *    返回：{ uploaded: 1, url: "..." }
 */
@RestController
@RequestMapping("/admin-api/uploads")
public class AdminUploadController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(
            @RequestParam(value = "upload", required = false) MultipartFile upload,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request
    ) {
        MultipartFile f = (upload != null ? upload : file);
        if (f == null || f.isEmpty()) {
            return ckErr("请选择图片文件");
        }

        String original = Optional.ofNullable(f.getOriginalFilename()).orElse("");
        String ext = getExt(original);
        if (ext.isEmpty() || !IMAGE_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            return ckErr("仅支持图片格式：" + String.join(",", IMAGE_EXT));
        }

        try {
            Path dir = Paths.get(uploadDir, "images");
            Files.createDirectories(dir);

            String name = genName(ext);
            Path target = dir.resolve(name);
            f.transferTo(target);

            // 给前端一个可访问的 URL（由 WebConfig 的 /uploads/** 资源映射提供）
            String url = "/uploads/images/" + name;

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("uploaded", 1);
            ok.put("fileName", name);
            ok.put("url", url);
            return ok;
        } catch (IOException e) {
            return ckErr("上传失败：" + e.getMessage());
        }
    }

    /**
     * 可选：通用文件上传（如果以后需要 CKEditor 插入附件）
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadFile(
            @RequestParam(value = "upload", required = false) MultipartFile upload,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        MultipartFile f = (upload != null ? upload : file);
        if (f == null || f.isEmpty()) {
            return ckErr("请选择文件");
        }
        try {
            Path dir = Paths.get(uploadDir, "files");
            Files.createDirectories(dir);

            String original = Optional.ofNullable(f.getOriginalFilename()).orElse("file");
            String ext = getExt(original);
            String name = genName(ext.isEmpty() ? "bin" : ext);
            Path target = dir.resolve(name);
            f.transferTo(target);
            String url = "/uploads/files/" + name;

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("uploaded", 1);
            ok.put("fileName", name);
            ok.put("url", url);
            return ok;
        } catch (IOException e) {
            return ckErr("上传失败：" + e.getMessage());
        }
    }

    private Map<String, Object> ckErr(String msg) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("uploaded", 0);
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("message", msg);
        err.put("error", e);
        return err;
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
}
