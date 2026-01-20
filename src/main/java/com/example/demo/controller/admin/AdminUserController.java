package com.example.demo.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.demo.entity.Org;
import com.example.demo.entity.Orgrole;
import com.example.demo.entity.User;
import com.example.demo.mapper.AdminUserMapper;
import com.example.demo.mapper.OrgMapper;
import com.example.demo.mapper.OrgroleMapper;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/admin-api/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final OrgroleMapper orgroleMapper;
    private final OrgMapper orgMapper;

    @PostMapping("/page")
    public Map<String, Object> pageUsers(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "orgId", required = false) String orgId,
            HttpSession session
    ) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("total", 0, "rows", List.of());
        }

        //  多选 + 父节点包含子孙
        List<String> orgIds = buildOrgIdFilter(orgId);

        long total = adminUserMapper.countUsers(username, role, orgIds);
        List<User> rows = adminUserMapper.pageUsers(username, role, orgIds, limit, offset);

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("rows", rows);
        return res;
    }

    @PostMapping("/one")
    public Map<String, Object> getOne(@RequestParam("uid") String uid, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }
        Map<String, Object> one = adminUserMapper.getOneForAdmin(uid);
        if (one == null || one.isEmpty()) {
            return Map.of("success", false, "msg", "用户不存在或无权限查看");
        }
        return Map.of("success", true, "one", one);
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestParam("uid") String uid,
                                      @RequestParam(value = "passwd", required = false) String passwd,
                                      @RequestParam(value = "orgid", required = false) String orgid,
                                      HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("uid", uid));
        if (user == null || !(user.getRole() == 1 || user.getRole() == 2)) {
            return Map.of("success", false, "msg", "用户不存在或无权限修改（管理员不可被管理）");
        }

        boolean changed = false;

        if (passwd != null && !passwd.isEmpty()) {
            if (passwd.length() < 6 || passwd.length() > 16) {
                return Map.of("success", false, "msg", "密码长度应为6-16位");
            }
            String newHash = md5Upper(passwd);
            int rows = userMapper.update(null,
                    new UpdateWrapper<User>().eq("uid", uid).set("passwd", newHash));
            changed = changed || rows > 0;
        }

        if (user.getRole() == 1 && orgid != null && !orgid.isEmpty()) {
            orgroleMapper.delete(new QueryWrapper<Orgrole>().eq("userid", uid));
            Orgrole or = new Orgrole();
            or.setUserid(uid);
            or.setOrgid(orgid);
            orgroleMapper.insert(or);
            changed = true;
        }

        if (!changed) return Map.of("success", false, "msg", "信息未修改");
        return Map.of("success", true, "msg", "修改成功");
    }

    private boolean isAdminLoggedIn(HttpSession session) {
        Object adminLogin = session.getAttribute("adminLogin");
        return (adminLogin instanceof Boolean) && (Boolean) adminLogin;
    }

    //  orgId 支持：单个 "558" / 多个 "558,539"；并且父节点会包含子孙
    private List<String> buildOrgIdFilter(String rawOrg) {
        if (rawOrg == null || rawOrg.trim().isEmpty()) return null;

        // 1) 拆分逗号串
        Set<String> selected = new LinkedHashSet<>();
        for (String part : rawOrg.split(",")) {
            if (part != null && !part.trim().isEmpty()) selected.add(part.trim());
        }
        if (selected.isEmpty()) return null;

        // 2) parent -> children（父字段就是 pId）
        List<Org> orgs = orgMapper.selectList(new QueryWrapper<Org>().select("id", "pId"));
        Map<String, List<String>> childrenMap = new HashMap<>();
        for (Org o : orgs) {
            String parent = o.getPId();
            if (parent == null || parent.trim().isEmpty() || "null".equalsIgnoreCase(parent.trim())) parent = "0";
            childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(o.getId());
        }

        // 3) BFS 扩展子孙 + 去重
        Set<String> result = new LinkedHashSet<>();
        for (String root : selected) {
            Deque<String> q = new ArrayDeque<>();
            q.add(root);
            while (!q.isEmpty()) {
                String cur = q.poll();
                if (result.add(cur)) {
                    List<String> kids = childrenMap.get(cur);
                    if (kids != null) q.addAll(kids);
                }
            }
        }
        return new ArrayList<>(result);
    }


    @PostMapping("/create")
    public Map<String, Object> create(@RequestParam("username") String username,
                                      @RequestParam("role") Integer role,
                                      @RequestParam("passwd") String passwd,
                                      @RequestParam(value = "orgid", required = false) String orgid,
                                      HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        String uname = username == null ? "" : username.trim();
        if (uname.isEmpty()) return Map.of("success", false, "msg", "用户名不能为空");
        if (uname.length() > 16) return Map.of("success", false, "msg", "用户名不能大于16位");

        if (role == null || (role != 0 && role != 1 && role != 2 && role != 3)) {
            return Map.of("success", false, "msg", "只允许创建学生(1)、老师(2)、管理员（0、3）账号");
        }

        String pwd = passwd == null ? "" : passwd;
        if (pwd.isEmpty()) return Map.of("success", false, "msg", "密码不能为空");
        if (pwd.length() < 6 || pwd.length() > 16) {
            return Map.of("success", false, "msg", "密码长度应为6-16位");
        }

        if (role == 1 && (orgid == null || orgid.trim().isEmpty())) {
            return Map.of("success", false, "msg", "学生必须选择班级");
        }

        Long cnt = userMapper.selectCount(new QueryWrapper<User>().eq("username", uname));
        if (cnt != null && cnt > 0) {
            return Map.of("success", false, "msg", "用户名已存在");
        }

        String uid = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);

        User u = new User();
        u.setUid(uid);
        u.setUsername(uname);
        u.setRole(role);
        u.setPasswd(md5Upper(pwd));

        //  添加：设置创建时间
        u.setC_time(getCurrentTime());

        int inserted = userMapper.insert(u);
        if (inserted <= 0) {
            return Map.of("success", false, "msg", "创建用户失败");
        }

        if (role == 1 && orgid != null && !orgid.trim().isEmpty()) {
            Orgrole or = new Orgrole();
            or.setUserid(uid);
            or.setOrgid(orgid.trim());
            orgroleMapper.insert(or);
        }

        return Map.of("success", true, "msg", "创建成功");
    }

    //  添加：获取当前时间的方法
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    private String md5Upper(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02X", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/delete")
    @Transactional
    public Map<String, Object> deleteUsers(@RequestParam("uids") String uids, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }

        if (uids == null || uids.trim().isEmpty()) {
            return Map.of("success", false, "msg", "uids不能为空");
        }

        String[] arr = uids.split(",");
        int deleted = 0;
        int skipped = 0;

        for (String raw : arr) {
            String uid = raw == null ? "" : raw.trim();
            if (uid.isEmpty()) continue;

            User user = userMapper.selectOne(new QueryWrapper<User>().eq("uid", uid));
            if (user == null) { skipped++; continue; }

            //  只允许删学生/老师（1/2），管理员(0/3)跳过
            if (!(user.getRole() == 1 || user.getRole() == 2)) {
                skipped++;
                continue;
            }

            // 先删班级映射
            orgroleMapper.delete(new QueryWrapper<Orgrole>().eq("userid", uid));

            // 再删用户
            int rows = userMapper.delete(new QueryWrapper<User>().eq("uid", uid));
            if (rows > 0) deleted++;
        }

        return Map.of("success", true, "msg", "删除成功，删除数量：" + deleted + "，跳过：" + skipped);
    }

}
