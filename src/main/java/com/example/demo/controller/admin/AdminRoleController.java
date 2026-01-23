package com.example.demo.controller.admin;
//admin
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.demo.entity.StRole;
import com.example.demo.mapper.ContentMapper;
import com.example.demo.mapper.StRoleMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 权限管理（下载权限）
 *
 * 对应前端 role.html / 旧版 role.jsp 的行为：
 * - 列表展示 st_content，并显示是否允许下载（来自 st_role.enabled，默认视为 1）
 * - chmod：批量将选中内容设置为 允许(1)/不允许(2)
 */
@RestController
@RequestMapping("/admin-api/role")
@RequiredArgsConstructor
public class AdminRoleController {

    private final ContentMapper contentMapper;
    private final StRoleMapper stRoleMapper;

    private boolean isAdmin(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        if (!(adminLogin instanceof Boolean) || !((Boolean) adminLogin)) return false;
        Object roleObj = session.getAttribute("role");
        int role = -1;
        try { role = Integer.parseInt(String.valueOf(roleObj)); } catch (Exception ignored) {}
        return role == 0 || role == 3;
    }

    /**
     * bootstrap-table 分页：返回 {total, rows}
     * rows 字段：id,title,tName,enabled,name
     */
    @PostMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "10") int limit,
                                    @RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(required = false) String title,
                                    HttpSession session) {
        if (!isAdmin(session)) {
            return Map.of("total", 0, "rows", List.of());
        }
        String t = (title == null) ? "" : title.trim();
        long total = contentMapper.countForRole(t);
        List<Map<String, Object>> rows = contentMapper.pageForRole(offset, limit, t);

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("rows", rows);
        return res;
    }

    /**
     * 批量设置下载权限
     * 兼容旧前端参数：uid=1,2,3, & enabled=1/2
     */
    @PostMapping("/chmod")
    public Map<String, Object> chmod(@RequestParam("uid") String uid,
                                     @RequestParam("enabled") String enabled,
                                     HttpSession session) {
        if (!isAdmin(session)) return Map.of("success", false, "msg", "未登录或无权限");

        String en = (enabled == null) ? "" : enabled.trim();
        if (!"1".equals(en) && !"2".equals(en)) {
            return Map.of("success", false, "msg", "enabled参数必须为1或2");
        }
        if (uid == null || uid.trim().isEmpty()) {
            return Map.of("success", false, "msg", "uid不能为空");
        }

        // uid 其实是 st_content.id 列表（沿用老系统命名）
        List<String> ids = new ArrayList<>();
        for (String s : uid.split(",")) {
            if (s == null) continue;
            String v = s.trim();
            if (v.isEmpty()) continue;
            ids.add(v);
        }
        if (ids.isEmpty()) return Map.of("success", false, "msg", "uid格式不正确");

        int changed = 0;
        for (String contentId : ids) {
            // 如果表里已有记录就 update，没有就 insert
            LambdaQueryWrapper<StRole> qw = new LambdaQueryWrapper<StRole>()
                    .eq(StRole::getConuid, contentId)
                    .last("LIMIT 1");
            StRole existed = stRoleMapper.selectOne(qw);
            if (existed == null) {
                StRole r = new StRole();
                r.setConuid(contentId);
                r.setEnabled(en);
                changed += (stRoleMapper.insert(r) > 0 ? 1 : 0);
            } else {
                existed.setEnabled(en);
                changed += (stRoleMapper.updateById(existed) > 0 ? 1 : 0);
            }
        }

        return Map.of(
                "success", true,
                "msg", ("1".equals(en) ? "已开放" : "已取消") + "下载权限，处理数量：" + changed
        );
    }
}
