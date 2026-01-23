package com.example.demo.mapper;
//admin
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Org;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrgMapper extends BaseMapper<Org> {
}
