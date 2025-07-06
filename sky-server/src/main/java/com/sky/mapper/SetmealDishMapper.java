package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 根据菜品ID列表查询对应的套餐ID列表
     * @param dishIds 菜品ID列表
     * @return 对应的套餐ID列表
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 批量插入套餐菜品数据
     * @param setmealDishes 套餐菜品列表
     */
    void insertBatch(List<SetmealDish> setmealDishes);
}
