package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;

public interface SetMealService {
    /**
     * 新增套餐和对应的菜品
     * @param setmealDTO 套餐DTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO 套餐分页查询DTO
     * @return 分页结果
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
}
