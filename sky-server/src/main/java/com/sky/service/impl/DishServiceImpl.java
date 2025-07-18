package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        // 将 DTO 转换为实体类
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表插入1条数据
        dishMapper.insert(dish);
        // 获取插入后的菜品id
        Long dishId = dish.getId();

        if (dishDTO.getFlavors() != null && !dishDTO.getFlavors().isEmpty()) {
            dishDTO.getFlavors().forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            // 向口味表插入n条数据
            dishFlavorMapper.insertBatch(dishDTO.getFlavors());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        // 1. 设置分页查询条件
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 2. 执行分页查询
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        // 3. 封装分页查询结果并返回
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * {@inheritDoc}
     */
    @Transactional // 开启事务
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前菜品是否能够删除---是否存在起售中的菜品
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                // 抛出异常：起售中的菜品不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        // 判断当前菜品是否能够删除---是否被套餐关联了
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && !setmealIds.isEmpty()) {
            // 抛出异常：当前菜品关联了套餐,不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表中的菜品数据
        dishMapper.deleteByIds(ids);
        // 删除菜品口味表中的对应口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据ID查询菜品
        Dish dish = dishMapper.getById(id);
        // 根据菜品ID查询对应的口味
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        // 将菜品和口味数据封装到 DishVO 中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        // 返回菜品视图对象
        return dishVO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        // 将 DTO 转换为实体类
        BeanUtils.copyProperties(dishDTO, dish);
        // 修改菜品表基本信息
        dishMapper.update(dish);
        // 删除原有口味数据
        dishFlavorMapper.deleteByDishIds(Collections.singletonList(dish.getId()));

        // 重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 设置口味数据的菜品ID
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());
            });
            // 批量插入口味数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 构造菜品对象
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        // 更新菜品状态
        dishMapper.update(dish);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Dish> getListByCategoryId(Long categoryId) {
        return dishMapper.getListByCategoryId(categoryId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        // 查询菜品数据
        log.info("【用户端】查询菜品数据:{}", dish);
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            // 将菜品数据封装到 DishVO 中
            BeanUtils.copyProperties(d, dishVO);
            // 根据菜品ID查询对应的口味
            log.info("【用户端】根据菜品ID查询对应的口味:dishId={}", d.getId());
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            // 将口味数据封装到 DishVO 中
            dishVO.setFlavors(flavors);
            // 将 DishVO 添加到 DishVO 列表中
            dishVOList.add(dishVO);
        }
        // 返回 DishVO 列表
        return dishVOList;
    }
}
