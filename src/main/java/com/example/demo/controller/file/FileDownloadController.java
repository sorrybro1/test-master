package com.example.demo.controller.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.Content;
import com.example.demo.mapper.ContentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileDownloadController {

    private final ContentMapper contentMapper;

    // 文件存储基础路径，如果没有配置默认使用 uploads 目录
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 下载源码文件
     * URL: /api/file/download/scode/{contentId}
     */
    @GetMapping("/download/scode/{contentId}")
    public ResponseEntity<Resource> downloadScode(@PathVariable Integer contentId) {
        return downloadFile(contentId, true);
    }

    /**
     * 下载程序运行文件
     * URL: /api/file/download/sdll/{contentId}
     */
    @GetMapping("/download/sdll/{contentId}")
    public ResponseEntity<Resource> downloadSdll(@PathVariable Integer contentId) {
        return downloadFile(contentId, false);
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info/{contentId}")
    public ResponseEntity<Content> getFileInfo(@PathVariable Integer contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        // 只返回必要的文件路径信息
        Content info = new Content();
        info.setId(content.getId());
        info.setTitle(content.getTitle());
        info.setScode(content.getScode());
        info.setSdll(content.getSdll());

        return ResponseEntity.ok(info);
    }

    /**
     * 根据分类ID获取内容ID
     */
    @GetMapping("/content/byTid/{tid}")
    public ResponseEntity<Content> getContentByTid(@PathVariable Integer tid) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getTid, tid);
        Content content = contentMapper.selectOne(wrapper);

        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(content);
    }

    /**
     * 通用文件下载方法
     * @param contentId 内容ID
     * @param isScode true:下载源码, false:下载程序运行文件
     */
    private ResponseEntity<Resource> downloadFile(Integer contentId, boolean isScode) {
        try {
            // 1. 从数据库查询内容
            Content content = contentMapper.selectById(contentId);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. 获取文件路径
            String filePath = isScode ? content.getScode() : content.getSdll();
            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.notFound()
                        .header("X-Error-Message", "文件路径为空")
                        .build();
            }


            // 3. 处理文件路径
            // 数据库存的是 URL 路径：/uploads/xxx，必须映射到磁盘 uploads 目录
            if (filePath.startsWith("/uploads/")) {
                // 去掉开头的 "/uploads/"
                String relative = filePath.substring("/uploads/".length()); // e.g. "program/xxx.zip"
                filePath = Paths.get(uploadDir, relative).toString();       // e.g. "uploads/program/xxx.zip"
            } else if (!isAbsolutePath(filePath)) {
                // 普通相对路径
                filePath = Paths.get(uploadDir, filePath).toString();
            }

            // 4. 构建文件资源
            Path path = Paths.get(filePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());

            // 5. 检查文件是否存在
            if (!resource.exists()) {
                return ResponseEntity.notFound()
                        .header("X-Error-Message", "文件不存在: " + path.toString())
                        .build();
            }

            if (!resource.isReadable()) {
                return ResponseEntity.status(403)
                        .header("X-Error-Message", "文件不可读")
                        .build();
            }

            // 6. 获取文件名
            String fileName = path.getFileName().toString();

            // 7. 根据文件扩展名设置Content-Type
            String contentType = getContentType(fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"")
                    .header("X-File-Name", fileName)
                    .header("X-File-Size", String.valueOf(resource.contentLength()))
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest()
                    .header("X-Error-Message", "文件路径格式错误")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .header("X-Error-Message", "服务器内部错误: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 判断是否为绝对路径
     */
    private boolean isAbsolutePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        // Windows路径 (C:\... 或 \\...)
        if (path.length() > 2 && path.charAt(1) == ':') {
            return true;
        }
        // Unix/Linux路径 (/...)
        return path.startsWith("/");
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".zip")) {
            return "application/zip";
        } else if (lowerName.endsWith(".rar")) {
            return "application/x-rar-compressed";
        } else if (lowerName.endsWith(".tar")) {
            return "application/x-tar";
        } else if (lowerName.endsWith(".gz")) {
            return "application/gzip";
        } else if (lowerName.endsWith(".jar")) {
            return "application/java-archive";
        } else if (lowerName.endsWith(".exe")) {
            return "application/x-msdownload";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return "text/html";
        } else if (lowerName.endsWith(".xml")) {
            return "text/xml";
        } else if (lowerName.endsWith(".json")) {
            return "application/json";
        } else if (lowerName.endsWith(".c") || lowerName.endsWith(".cpp") || lowerName.endsWith(".java")) {
            return "text/plain";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        }

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}