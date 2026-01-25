package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.Content;
import com.example.demo.entity.Courese;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoureseMapper extends BaseMapper<Courese> {
}
