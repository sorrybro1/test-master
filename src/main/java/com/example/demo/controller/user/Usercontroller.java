package com.example.demo.controller.user;

import com.example.demo.dto.UserInfoDTO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class Usercontroller {
    // 直接注入 Mapper，不再使用 Service
    private final UserMapper userMapper;

    // 查单个
    @GetMapping("/{id}")
    public User getById(@PathVariable Integer id) {
        // IService.getById() -> BaseMapper.selectById()
        return userMapper.selectById(id);
    }

    // 查全部
    @GetMapping("/list")
    public List<User> list() {
        // IService.list() -> BaseMapper.selectList(null)
        // null 表示没有查询条件，即查询所有
        return userMapper.selectList(null);
    }

    // 查询用户数据
    @PostMapping("/getUser")
    public Map<String,Object> getUser(HttpSession session) {
        // 1) 判断登录态（与你 /api/auth/check 的逻辑一致）
        Object isLogin = session.getAttribute("isLogin");
        if (!(isLogin instanceof Boolean) || !((Boolean) isLogin)) {
            return resp(false, "未登录", null);
        }

        // 2) 优先用 userId 查库（你登录时已存 userId）:contentReference[oaicite:2]{index=2}
        Object userIdObj = session.getAttribute("userId");
        User user = null;

        if (userIdObj != null) {
            Integer userId = (userIdObj instanceof Integer)
                    ? (Integer) userIdObj
                    : Integer.valueOf(String.valueOf(userIdObj));
            user = userMapper.selectById(userId);
        }

        // 3) 兜底：如果 userId 查不到，则从 session 里的 user 取
        if (user == null) {
            Object u = session.getAttribute("user");
            if (u instanceof User) {
                user = (User) u;
                // 保险起见再查一次库（避免 session 对象字段缺失/过旧）
                if (user.getId() > 0) {
                    User fresh = userMapper.selectById(user.getId());
                    if (fresh != null) user = fresh;
                }
            }
        }

        if (user == null) {
            return resp(false, "用户不存在或会话失效", null);
        }

        // 4) 组装 DTO（不要返回 passwd）
        UserInfoDTO dto = toUserInfoDTO(user);

        // 如果你要班级(org_name/org_id)，建议在这里查关联表后 set 到 dto（见下面“二、带班级查询”）
        return resp(true, "ok", dto);
    }

    private Map<String, Object> resp(boolean success, String msg, UserInfoDTO dto) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("msg", msg);
        // 兼容旧前端：res.user
        m.put("user", dto);
        return m;
    }
    private UserInfoDTO toUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUid(user.getUid());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setRoleName(user.getRoleName());
        dto.setSex(user.getSex());
        dto.setCollege(user.getCollege());
        dto.setProfessional(user.getProfessional());      // 你登录响应里也是这么填的 :contentReference[oaicite:3]{index=3}
        dto.setProf(user.getProf());
        dto.setPhone(user.getPhone());
        dto.setUserClass(user.getU_class());
        return dto;
    }

    // 保存个人资料
    @PostMapping("/updateUser.do")
    public Map<String, Object> updateUser(@RequestParam(required = false) String name,
                                          @RequestParam(required = false) String sex,
                                          @RequestParam(required = false) String college,
                                          @RequestParam(required = false) String professional,
                                          @RequestParam(required = false) String prof,
                                          @RequestParam(required = false) String phone,
                                          HttpSession session) {

        if (!isLoggedIn(session)) {
            return resp(false, "未登录", null);
        }

        Integer userId = getSessionUserId(session);
        if (userId == null) {
            return resp(false, "未登录", null);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return resp(false, "用户不存在", null);
        }

        user.setName(name);
        user.setSex(sex);
        user.setCollege(college);
        user.setPhone(phone);


        // 角色分支：学生写 professional，老师写 prof（与你前端逻辑对齐）
        if (user.getRole() > 0  && user.getRole() == 2) {
            user.setProf(prof);
        } else {
            user.setProfessional(professional);
        }

        int rows = userMapper.updateById(user);
        if (rows > 0) {
            // 可选：更新 session 里的 user 对象（如果你存了）
            session.setAttribute("user", user);
            return resp(true, "保存成功", null);
        }
        return resp(false, "保存失败", null);
    }

    // 修改密码：/user/updatePwd1.do?pwd1=旧&pwd=新
    @PostMapping("/updatePwd1.do")
    public Map<String, Object> updatePwd1(@RequestParam("pwd1") String oldPwd,
                                          @RequestParam("pwd") String newPwd,
                                          HttpSession session) {

        if (!isLoggedIn(session)) {
            return resp(false, "未登录", null);
        }

        Integer userId = getSessionUserId(session);
        if (userId == null) {
            return resp(false, "未登录", null);
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return resp(false, "用户不存在", null);
        }

        // 你库里的 passwd 很像 MD5 大写十六进制，这里按 MD5(UTF-8).toUpperCase() 写
        //如果要密码要加密 改这里
        //String oldHash = md5Upper(oldPwd);
        String oldHash = oldPwd;
        if (user.getPasswd() == null || !user.getPasswd().equalsIgnoreCase(oldHash)) {
            return resp(false, "原始密码错误", null);
        }

        if (newPwd == null || newPwd.length() < 6 || newPwd.length() > 16) {
            return resp(false, "新密码长度应为6-16位", null);
        }

        //如果要密码要加密 改这里
        //user.setPasswd(md5Upper(newPwd));
        user.setPasswd(newPwd);
        int rows = userMapper.updateById(user);
        if (rows > 0) {
            return resp(true, "修改成功", null);
        }
        return resp(false, "修改失败", null);
    }

    // ----------------- 工具方法 -----------------

    private boolean isLoggedIn(HttpSession session) {
        Object isLogin = session.getAttribute("isLogin");
        if (isLogin instanceof Boolean) return (Boolean) isLogin;
        // 兜底：只要 userId 存在也认为登录
        return session.getAttribute("userId") != null;
    }

    private Integer getSessionUserId(HttpSession session) {
        Object obj = session.getAttribute("userId");
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        try { return Integer.valueOf(String.valueOf(obj)); }
        catch (Exception e) { return null; }
    }

    private Map<String, Object> resp(boolean success, String msg, Object user) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("msg", msg);
        // 兼容旧前端：res.user
        m.put("user", user);
        return m;
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
}
