package com.example.demo.controller.admin;

import com.example.demo.mapper.ContentMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin-api/contents")
@RequiredArgsConstructor
public class AdminContentController {

    private final ContentMapper contentMapper;

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
