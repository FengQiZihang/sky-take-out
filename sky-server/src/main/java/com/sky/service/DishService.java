package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;

import java.util.List;

public interface DishService {
    /**
     * 新增菜品和对应的口味
     * @param dishDTO 菜品DTO
     */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO 菜品分页查询DTO
     * @return 分页结果
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids 菜品ID列表
     * @throws DeletionNotAllowedException 起售中的菜品不能删除
     * @throws DeletionNotAllowedException 当前菜品关联了套餐,不能删除
     */
    void deleteBatch(List<Long> ids);
}
