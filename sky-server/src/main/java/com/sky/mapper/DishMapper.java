package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId 分类id
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 插入菜品数据
     * @param dish 菜品实体
     */
    @AutoFill(OperationType.INSERT)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into dish (name, category_id, price, image, description, status, create_time, update_time, create_user, update_user)" +
            " VALUES" +
            " (#{name}, #{categoryId}, #{price}, #{image}, #{description}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Dish dish);

    /**
     * 分页查询菜品
     * @param dishPageQueryDTO 分页查询条件
     * @return Page<DishVO> Dish视图对象的分页结果
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据ID查询菜品
     * @param id 菜品ID
     * @return Dish 菜品实体
    */
    @Select("select * from dish where id = #{id}")
    Dish selectById(Long id);

    /**
     * 根据ID列表批量删除菜品
     * @param ids 菜品ID列表
     */
    void deleteByIds(List<Long> ids);
}
