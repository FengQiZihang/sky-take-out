package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import java.util.List;

public interface CategoryService {

    /**
     * 新增分类
     * @param categoryDTO 分类DTO
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 分页查询
     * @param categoryPageQueryDTO 分类分页查询DTO
     * @return
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id 分类ID
     * @throws DeletionNotAllowedException 当前分类关联了菜品,不能删除
     * @throws DeletionNotAllowedException 当前分类关联了套餐,不能删除
     */
    void deleteById(Long id);

    /**
     * 修改分类
     * @param categoryDTO 分类DTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 启用、禁用分类
     * @param status 状态 0 禁用 1 启用
     * @param id 分类ID
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据类型查询分类
     * @param type 类型
     * @return List<Category> 分类列表
      */
    List<Category> list(Integer type);
}
