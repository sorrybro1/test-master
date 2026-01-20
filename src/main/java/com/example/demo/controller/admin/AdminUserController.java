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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

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


    @PostMapping("/initPwd")
    public Map<String, Object> initPwd(@RequestParam("uids") String uids, HttpSession session) {
        if (!isAdminLoggedIn(session)) {
            return Map.of("success", false, "msg", "管理员未登录");
        }
        if (uids == null || uids.trim().isEmpty()) {
            return Map.of("success", false, "msg", "uids不能为空");
        }

        String[] arr = uids.split(",");
        int updated = 0;
        int skipped = 0;

        //  初始化密码（要固定啥就改这里）
        String initRawPwd = "123456";
        String initHash = md5Upper(initRawPwd);

        for (String raw : arr) {
            String uid = raw == null ? "" : raw.trim();
            if (uid.isEmpty()) continue;

            User user = userMapper.selectOne(new QueryWrapper<User>().eq("uid", uid));
            if (user == null) { skipped++; continue; }

            //  只允许重置学生/老师；管理员跳过
            if (!(user.getRole() == 1 || user.getRole() == 2)) {
                skipped++;
                continue;
            }

            int rows = userMapper.update(null,
                    new UpdateWrapper<User>()
                            .eq("uid", uid)
                            .set("passwd", initHash));
            if (rows > 0) updated++;
        }

        return Map.of("success", true, "msg", "初始化成功：" + updated + "，跳过：" + skipped);
    }

    @PostMapping(value = "/import", produces = "text/plain;charset=UTF-8")
    public String importUsers(@RequestParam("importUserFile") MultipartFile file, HttpSession session) throws Exception {
        if (!isAdminLoggedIn(session)) {
            return "{\"success\":\"d\",\"msg\":\"管理员未登录\"}";
        }
        if (file == null || file.isEmpty()) {
            return "{\"success\":\"e\",\"msg\":\"导入文件不能为空\"}";
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!(filename.endsWith(".xls") || filename.endsWith(".xlsx"))) {
            return "{\"success\":\"f\",\"msg\":\"导入文件格式不正确（仅支持xls/xlsx）\"}";
        }

        List<Map<String, String>> failList = new ArrayList<>();
        int ok = 0;

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return "{\"success\":\"d\",\"msg\":\"Excel内容为空\"}";
            }

            // 默认第0行是表头，第1行开始是数据
            int last = sheet.getLastRowNum();
            for (int r = 1; r <= last; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String username = cellToString(row.getCell(0)).trim();
                String passwd   = cellToString(row.getCell(1)).trim();
                String roleStr  = cellToString(row.getCell(2)).trim();
                String orgNameOrId = cellToString(row.getCell(3)).trim();

                if (username.isEmpty()) continue; // 空行跳过
                if (username.length() > 16) {
                    failList.add(fail(username, "用户名长度不能大于16位"));
                    continue;
                }

                Integer role;
                try {
                    role = Integer.valueOf(roleStr);
                } catch (Exception ex) {
                    failList.add(fail(username, "角色必须是数字：1学生/2老师/3管理员"));
                    continue;
                }
                if (!(role == 1 || role == 2 || role == 3)) {
                    failList.add(fail(username, "角色只允许：1学生/2老师/3管理员"));
                    continue;
                }

                if (passwd.isEmpty()) passwd = "123456"; // 允许空，默认123456
                if (passwd.length() < 6 || passwd.length() > 16) {
                    failList.add(fail(username, "密码长度需 6-16 位（为空默认123456）"));
                    continue;
                }

                // 用户名重复校验
                Long cnt = userMapper.selectCount(new QueryWrapper<User>().eq("username", username));
                if (cnt != null && cnt > 0) {
                    failList.add(fail(username, "用户名已存在"));
                    continue;
                }

                // 学生必须有班级（老师/管理员不需要）
                String orgId = null;
                if (role == 1) {
                    if (orgNameOrId.isEmpty()) {
                        failList.add(fail(username, "学生必须填写所属班级（班级ID或班级名称）"));
                        continue;
                    }
                    // 如果填的是纯数字 => 当作orgId；否则当作班级名称去查 st_org
                    if (orgNameOrId.matches("\\d+")) {
                        orgId = orgNameOrId;
                    } else {
                        Org org = orgMapper.selectOne(new QueryWrapper<Org>().eq("name", orgNameOrId));
                        if (org == null) {
                            failList.add(fail(username, "班级不存在：" + orgNameOrId));
                            continue;
                        }
                        orgId = org.getId();
                    }
                }

                // 插入 st_user
                String uid = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);

                User u = new User();
                u.setUid(uid);
                u.setUsername(username);
                u.setRole(role);
                u.setPasswd(md5Upper(passwd));
                u.setC_time(getCurrentTime()); // 写入创建时间

                int ins = userMapper.insert(u);
                if (ins <= 0) {
                    failList.add(fail(username, "插入用户失败"));
                    continue;
                }

                // 学生写入 st_orgrole
                if (role == 1 && orgId != null) {
                    Orgrole or = new Orgrole();
                    or.setUserid(uid);
                    or.setOrgid(orgId);
                    orgroleMapper.insert(or);
                }

                ok++;
            }
        }

        if (failList.isEmpty()) {
            return "{\"success\":\"a\",\"msg\":\"导入成功，共导入 " + ok + " 条\"}";
        } else {
            String listJson = new ObjectMapper().writeValueAsString(failList);
            return "{\"success\":\"b\",\"msg\":\"导入完成：成功 " + ok + " 条，失败 " + failList.size() + " 条\",\"list\":" + listJson + "}";
        }
    }

    private Map<String, String> fail(String username, String reason) {
        Map<String, String> m = new HashMap<>();
        m.put("username", username);
        m.put("reason", reason);
        return m;
    }

    private String cellToString(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue();
    }
}
