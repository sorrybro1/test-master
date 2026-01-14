package com.example.demo.controller.index;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.entity.User;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 1. 页面跳转逻辑
     * 对应原代码：/toIndex/getuserManagement.do
     * 作用：接收 tid，把用户带到静态 HTML 页面
     */
    @GetMapping("/toIndex/getuserManagement.do")
    public void toUserManagementPage(@RequestParam(value = "tid", required = false) String tid,
                                     HttpServletResponse response) throws IOException {
        // 重定向到静态页面，并透传 tid 参数
        if (tid != null) {
            response.sendRedirect("/user/userManagement.html?tid=" + tid);
        } else {
            response.sendRedirect("/user/userManagement.html");
        }
    }

    /**
     * 2. 数据查询接口 (供 HTML 页面的 AJAX 调用)
     * 作用：根据 tid 查 user 表
     */
    @PostMapping("/user/list")
    @ResponseBody
    public List<User> getUserList(@RequestParam(value = "tid", required = false) String tid) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        // 如果 tid 存在，就只查这个分类下的用户
        // 对应 SQL: SELECT * FROM user WHERE tid = ?
        if (tid != null && !tid.isEmpty()) {
            wrapper.eq("tid", tid);
        }

        // 还可以按 id 倒序排列，新用户在前
        wrapper.orderByDesc("id");

        return userMapper.selectList(wrapper);


    }
}