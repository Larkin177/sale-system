package com.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sales.entity.Sales;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesMapper extends BaseMapper<Sales> {
}
