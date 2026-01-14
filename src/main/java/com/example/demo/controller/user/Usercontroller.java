package com.example.demo.controller.user;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
