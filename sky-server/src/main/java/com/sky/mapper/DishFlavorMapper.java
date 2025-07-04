package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * 批量插入口味数据
     * @param flavors 口味列表
     */
    void insertBatch(List<DishFlavor> flavors);


    /**
     * 根据菜品ID列表批量删除口味数据
     * @param dishIds 菜品ID列表
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根据菜品ID查询口味数据
     * @param dishId 菜品ID
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(Long dishId);
}
