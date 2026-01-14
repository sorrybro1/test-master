package com.example.demo.controller.index;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Content;
import com.example.demo.mapper.ContentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/content")
public class ContentController {
    @Autowired
    private ContentMapper contentMapper;

    /**
     * 1. 获取内容列表接口
     * URL: /content/list?tid=1
     */
    @PostMapping("/list")
    @ResponseBody
    public List<Content> getContentList(@RequestParam(value = "tid", required = false) String tid) {
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        if (tid != null && !tid.isEmpty()) {
            wrapper.eq("tid", tid);
        }
        // 只查询必要的字段
        wrapper.select("id", "title", "tid");
        wrapper.orderByAsc("id");
        return contentMapper.selectList(wrapper);
    }

    @PostMapping("/detail")
    @ResponseBody
    public Content getContentDetail(@RequestParam("id") String id) {
        return contentMapper.selectById(id);
    }
}
