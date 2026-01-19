package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Org;
import com.example.demo.mapper.OrgMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin-api/orgs")
@RequiredArgsConstructor
public class AdminOrgController {

    private final OrgMapper orgMapper;

    /**
     * 返回 zTree simpleData 结构:
     * { tree: [ {id: "...", pId: "...", name: "..."}, ... ] }
     *
     * 兼容 st_org 父字段叫 pld 或 pid 两种情况
     */
    @PostMapping("/tree")
    public Map<String, Object> tree(HttpSession session) {

        Object adminLogin = session.getAttribute("adminLogin");
        if (!(adminLogin instanceof Boolean) || !((Boolean) adminLogin)) {
            return Map.of("tree", List.of());
        }

        // 关键：数据库父字段就是 pId，所以这里按 pId 查，不走实体映射
        List<Map<String, Object>> rows = orgMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Org>()
                        .select("`id`", "`name`", "`pId`")
                        .orderByAsc("id")
        );

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> n = new HashMap<>();

            String id = r.get("id") == null ? "" : String.valueOf(r.get("id"));
            String name = r.get("name") == null ? "" : String.valueOf(r.get("name"));

            Object pObj = r.get("pId"); // 注意这里就是 pId
            String p = (pObj == null) ? "0" : String.valueOf(pObj).trim();
            if (p.isEmpty() || "null".equalsIgnoreCase(p)) p = "0";

            n.put("id", id);
            n.put("pId", p);   // 返回给前端 zTree 的字段也叫 pId
            n.put("name", name);
            tree.add(n);
        }

        return Map.of("tree", tree);
    }
}
