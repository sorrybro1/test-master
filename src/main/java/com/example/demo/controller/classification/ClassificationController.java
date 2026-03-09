package com.example.demo.controller.classification;
//admin
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Classification;
import com.example.demo.mapper.ClassificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/classification")
public class ClassificationController {

    @Autowired
    private ClassificationMapper classificationMapper;

    @PostMapping("/getNavigation")
    public List<Classification> getNavigation() {
        QueryWrapper<Classification> wrapper = new QueryWrapper<>();

        // 尝试按 id 排序，保证菜单顺序固定
        wrapper.orderByAsc("id");

        return classificationMapper.selectList(wrapper);
    }
}