package com.example.demo.controller.org; // 修改为你的实际包名

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.entity.Org;
import com.example.demo.entity.OrgInfo;
import com.example.demo.mapper.OrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stOrgCtr")
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数注入 mapper
public class stOrgCtr {

    private final OrgMapper orgMapper;

    /**
     * 获取班级/组织树结构
     * 兼容旧接口：返回格式 { "tree": [ {id:.., pId:.., name:.., open:true}, ... ] }
     */
    @RequestMapping("/getOrgInfo")
    public Map<String, Object> getOrgInfo() {
        // 1. 查询所有数据
        // 如果需要排序，可以使用 wrapper.orderByAsc("id")
        List<Org> list = orgMapper.selectList(new QueryWrapper<>());

        // 2. 转换数据格式以适配前端 ZTree
        // 原代码逻辑：手动拼接字符串 {id:..., pId:..., name:..., open:true}
        // 新代码逻辑：构建 Map 列表，Jackson 会自动序列化为标准的 JSON 数组
        List<Map<String, Object>> treeNodes = new ArrayList<>();

        if (list != null) {
            for (Org org : list) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", org.getId());
                // 注意：ZTree 默认使用 pId (驼峰)，这里显式设置 key 为 "pId"
                node.put("pId", org.getPId());
                node.put("name", org.getName());
                // 原代码写死 open: true
                node.put("open", true);

                treeNodes.add(node);
            }
        }

        // 3. 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("tree", treeNodes);

        return result;
    }
}