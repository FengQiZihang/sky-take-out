package com.sky.service;

import com.sky.dto.SetmealDTO;

public interface SetMealService {
    /**
     * 新增套餐和对应的菜品
     * @param setmealDTO 套餐DTO
     */
    void saveWithDish(SetmealDTO setmealDTO);
}
