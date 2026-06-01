package com.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sales.entity.Commission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommissionMapper extends BaseMapper<Commission> {
}
