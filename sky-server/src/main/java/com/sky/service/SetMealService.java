package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

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

    /**
     * 批量删除套餐
     * @param ids 套餐ID列表
     * @throws DeletionNotAllowedException 起售中的套餐不能删除
     */
    void deleteBatch(List<Long> ids);

    /**
     * 起售或停售套餐
     * @param status 套餐状态，1表示起售，0表示停售
     * @param id 套餐ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据id查询套餐和对应的菜品选项
     * @param id 套餐ID
     * @return SetmealVO 套餐视图对象
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐和对应的菜品选项
     * @param setmealDTO 套餐DTO
     */
    void updateWithDish(SetmealDTO setmealDTO);
}
