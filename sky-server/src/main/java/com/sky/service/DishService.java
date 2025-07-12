package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

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

    /**
     * 根据ID查询菜品和对应的口味
     * @param id 菜品ID
     * @return 菜品VO对象
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品和对应的口味
     * @param dishDTO 菜品DTO
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 起售或停售菜品
     * @param status 菜品状态，1表示起售，0表示停售
     * @param id 菜品ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类id
     * @return 菜品列表
     */
    List<Dish> getListByCategoryId(Long categoryId);

    /**
     * 根据条件查询菜品数据
     * @param dish 菜品
     * @return 菜品视图对象列表
     */
    List<DishVO> listWithFlavor(Dish dish);
}
