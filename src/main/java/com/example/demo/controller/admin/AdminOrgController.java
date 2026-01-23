package com.example.demo.controller.admin;
//admin
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Org;
import com.example.demo.mapper.OrgMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 班级/组织管理（zTree）
 * 前端 orgInfo.html 约定接口：
 *  - POST /admin-api/orgs/tree
 *  - POST /admin-api/orgs/add?pId=...&name=...
 *  - POST /admin-api/orgs/update?id=...&name=...
 *  - POST /admin-api/orgs/canDelete?id=...
 *  - POST /admin-api/orgs/delete?id=...
 */
@RestController
@RequestMapping("/admin-api/orgs")
@RequiredArgsConstructor
public class AdminOrgController {

    private final OrgMapper orgMapper;

    private boolean isAdminLoggedIn(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        return (adminLogin instanceof Boolean) && (Boolean) adminLogin;
    }

    /**
     * 返回 zTree simpleData 结构:
     * { tree: [ {id: "...", pId: "...", name: "..."}, ... ] }
     */
    @PostMapping("/tree")
    public Map<String, Object> tree(HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("tree", List.of());
        }

        // st_org 字段名就是 pId（注意大小写），这里用 selectMaps 确保取到正确列
        List<Map<String, Object>> rows = orgMapper.selectMaps(
                new QueryWrapper<Org>()
                        .select("`id`", "`name`", "`pId`")
                        .orderByAsc("id")
        );

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String id = r.get("id") == null ? "" : String.valueOf(r.get("id"));
            String name = r.get("name") == null ? "" : String.valueOf(r.get("name"));

            Object pObj = r.get("pId");
            String pId = (pObj == null) ? "0" : String.valueOf(pObj).trim();
            if (pId.isEmpty() || "null".equalsIgnoreCase(pId)) pId = "0";

            Map<String, Object> n = new HashMap<>();
            n.put("id", id);
            n.put("pId", pId);
            n.put("name", name);
            tree.add(n);
        }
        return Map.of("tree", tree);
    }

    /**
     * 添加节点：在某个父节点下新增一个子节点
     * 返回：{success:true, id:"新id", msg:"..."}
     */
    @PostMapping("/add")
    public Map<String, Object> add(@RequestParam("pId") String pId,
                                   @RequestParam("name") String name,
                                   HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        name = (name == null) ? "" : name.trim();
        if (name.isEmpty()) {
            return Map.of("success", false, "msg", "名称不能为空");
        }

        // 根节点统一用 "0"
        if (pId == null || pId.trim().isEmpty() || "null".equalsIgnoreCase(pId.trim())) {
            pId = "0";
        } else {
            pId = pId.trim();
        }

        Org org = new Org();
        // id 用 UUID（你 Org.id 是 String）
        String id = UUID.randomUUID().toString().replace("-", "");
        org.setId(id);
        org.setName(name);
        org.setPId(pId);

        int n = orgMapper.insert(org);
        if (n > 0) {
            return Map.of("success", true, "id", id, "msg", "添加成功");
        }
        return Map.of("success", false, "msg", "添加失败");
    }

    /**
     * 重命名节点
     * 返回：{success:true/false, msg:"..."}
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam("id") String id,
                                      @RequestParam("name") String name,
                                      HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        name = (name == null) ? "" : name.trim();
        if (name.isEmpty()) {
            return Map.of("success", false, "msg", "名称不能为空");
        }

        Org org = orgMapper.selectById(id);
        if (org == null) {
            return Map.of("success", false, "msg", "节点不存在");
        }

        org.setName(name);
        int n = orgMapper.updateById(org);
        if (n > 0) {
            return Map.of("success", true, "msg", "修改成功");
        }
        return Map.of("success", false, "msg", "修改失败");
    }

    /**
     * 删除前校验：只允许删除“叶子节点”（没有子节点）
     * （如果你还想加“班级下有学生不能删”，后面我再给你加 UserMapper 判断）
     */
    @PostMapping("/canDelete")
    public Map<String, Object> canDelete(@RequestParam("id") String id,
                                         HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }
        if (id == null || id.isBlank() || "0".equals(id.trim())) {
            return Map.of("success", false, "msg", "根节点不允许删除");
        }

        Long children = orgMapper.selectCount(new QueryWrapper<Org>().eq("`pId`", id));
        if (children != null && children > 0) {
            return Map.of("success", false, "msg", "该节点下还有子节点，无法删除");
        }
        return Map.of("success", true);
    }

    /**
     * 删除节点（会先做一次 canDelete 同样的校验）
     */
    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestParam("id") String id,
                                      HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        // 复用校验逻辑
        Map<String, Object> check = canDelete(id, session);
        Object ok = check.get("success");
        if (!(ok instanceof Boolean) || !((Boolean) ok)) {
            return check;
        }

        int n = orgMapper.deleteById(id);
        if (n > 0) {
            return Map.of("success", true, "msg", "删除成功");
        }
        return Map.of("success", false, "msg", "删除失败");
    }
}
