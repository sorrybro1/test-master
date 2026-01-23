package com.example.demo.controller.admin;
//admin
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Type;
import com.example.demo.mapper.TypeMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin-api/types")
@RequiredArgsConstructor
public class AdminTypeController {

    private final TypeMapper typeMapper;

    private boolean isAdminLoggedIn(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        return (adminLogin instanceof Boolean) && (Boolean) adminLogin;
    }

    /**
     * 给 AddContent.html 用：一次性返回全部分类
     * GET /admin-api/types/listAll
     * 返回：{ total, list }（你前端就是按 res.list 取的）
     */
    @GetMapping("/listAll")
    public Map<String, Object> listAll(
            @RequestParam(value = "name", required = false) String name,
            HttpSession session
    ) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("total", 0, "list", List.of());
        }

        QueryWrapper<Type> qw = new QueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            qw.like("name", name.trim());
        }
        // 你 page() 里是 orderByDesc("id")，这里保持一致
        qw.orderByDesc("id");

        List<Type> list = typeMapper.selectList(qw);

        Map<String, Object> out = new HashMap<>();
        out.put("total", list.size());
        out.put("list", list);
        return out;
    }

    /**
     * bootstrap-table 服务端分页
     * 入参：limit, offset
     * 返回：{total, rows}
     */
    @PostMapping("/page")
    public Map<String, Object> page(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "name", required = false) String name,
            HttpSession session
    ) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("total", 0, "rows", List.of());
        }

        QueryWrapper<Type> qw = new QueryWrapper<>();
        if (name != null && !name.trim().isEmpty()) {
            qw.like("name", name.trim());
        }
        qw.orderByDesc("id");

        long current = (offset / (long) limit) + 1;
        Page<Type> page = new Page<>(current, limit);
        Page<Type> res = typeMapper.selectPage(page, qw);

        Map<String, Object> out = new HashMap<>();
        long total = res.getTotal();
        if (total == 0) {
            Long cnt = typeMapper.selectCount(qw);
            total = (cnt == null ? 0 : cnt);
        }
        out.put("total", total);
        out.put("rows", res.getRecords());
        return out;
    }

    @PostMapping("/one")
    public Map<String, Object> one(@RequestParam("id") Integer id, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }
        Type t = typeMapper.selectById(id);
        if (t == null) return Map.of("success", false, "msg", "分类不存在");
        return Map.of("success", true, "data", t);
    }

    @PostMapping("/create")
    public Map<String, Object> create(
            @RequestParam("name") String name,
            @RequestParam("desc") String desc,
            @RequestParam(value = "type", required = false) String type,
            HttpSession session
    ) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        String n = name == null ? "" : name.trim();
        String d = desc == null ? "" : desc.trim();
        if (n.isEmpty()) return Map.of("success", false, "msg", "请输入分类名");
        if (d.isEmpty()) return Map.of("success", false, "msg", "请输入描述");
        if (n.length() > 30) return Map.of("success", false, "msg", "分类名不能超过30个字符");
        if (d.length() > 200) return Map.of("success", false, "msg", "描述不能超过200个字符");

        Long cnt = typeMapper.selectCount(new QueryWrapper<Type>().eq("name", n));
        if (cnt != null && cnt > 0) {
            return Map.of("success", false, "msg", "分类名已存在");
        }

        Type t = new Type();
        t.setName(n);
        t.setDesc(d);
        t.setType((type == null || type.trim().isEmpty()) ? "1" : type.trim());

        int ins = typeMapper.insert(t);
        return ins > 0 ? Map.of("success", true, "msg", "新建成功")
                : Map.of("success", false, "msg", "新建失败");
    }

    @PostMapping("/update")
    public Map<String, Object> update(
            @RequestParam("id") Integer id,
            @RequestParam("name") String name,
            @RequestParam("desc") String desc,
            HttpSession session
    ) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        Type old = typeMapper.selectById(id);
        if (old == null) return Map.of("success", false, "msg", "分类不存在");

        String n = name == null ? "" : name.trim();
        String d = desc == null ? "" : desc.trim();
        if (n.isEmpty()) return Map.of("success", false, "msg", "请输入分类名");
        if (d.isEmpty()) return Map.of("success", false, "msg", "请输入描述");
        if (n.length() > 30) return Map.of("success", false, "msg", "分类名不能超过30个字符");
        if (d.length() > 200) return Map.of("success", false, "msg", "描述不能超过200个字符");

        old.setName(n);
        old.setDesc(d);

        int rows = typeMapper.updateById(old);
        return rows > 0 ? Map.of("success", true, "msg", "修改成功")
                : Map.of("success", false, "msg", "修改失败");
    }

    /**
     * 单删/批量删：ids=1 或 ids=1,2,3
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestParam("ids") String ids, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }
        if (ids == null || ids.trim().isEmpty()) {
            return Map.of("success", false, "msg", "ids不能为空");
        }

        List<Integer> idList = new ArrayList<>();
        for (String s : ids.split(",")) {
            if (s == null || s.trim().isEmpty()) continue;
            try {
                idList.add(Integer.parseInt(s.trim()));
            } catch (Exception ignored) {}
        }
        if (idList.isEmpty()) return Map.of("success", false, "msg", "ids格式不正确");

        int rows = typeMapper.deleteBatchIds(idList);
        return Map.of("success", true, "msg", "删除成功，删除数量：" + rows);
    }
}
