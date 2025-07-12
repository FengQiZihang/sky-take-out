package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetMealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 套餐业务层
 */
@Service
public class SetMealServiceImpl implements SetMealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 将 DTO 转换为实体类
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 向套餐表插入1条数据
        setmealMapper.insert(setmeal);
        // 获取插入后的套餐id
        Long setmealId = setmeal.getId();

        if (setmealDTO.getSetmealDishes() != null && !setmealDTO.getSetmealDishes().isEmpty()) {
            setmealDTO.getSetmealDishes().forEach(setmealDish -> {
                setmealDish.setSetmealId(setmealId);
            });
            // 向套餐菜品表插入n条数据
            setmealDishMapper.insertBatch(setmealDTO.getSetmealDishes());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        // 1. 设置分页查询条件
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        // 2. 执行分页查询
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        // 3. 封装分页查询结果并返回
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        // 判断当前套餐是否能够删除---是否存在起售中的套餐
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                // 抛出异常：起售中的套餐不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 删除套餐表中的套餐数据
        setmealMapper.deleteByIds(ids);
        // 删除套餐菜品表中的对应套餐菜品数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 根据id查询套餐
        Setmeal setmeal = setmealMapper.getById(id);
        // 根据套餐id查询套餐菜品
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        // 将套餐菜品封装到套餐视图对象中
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        // 返回套餐视图对象
        return setmealVO;
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        // 将 DTO 转换为实体类
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        // 修改套餐表基本信息
        setmealMapper.update(setmeal);
        // 删除原有套餐菜品数据
        setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmeal.getId()));

        // 重新插入套餐菜品数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && !setmealDishes.isEmpty()) {
            // 设置套餐菜品数据的套餐ID
            setmealDishes.forEach(setmealDish -> {
                setmealDish.setSetmealId(setmeal.getId());
            });
            // 批量插入套餐菜品数据
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 构造套餐对象
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        // 更新套餐状态
        setmealMapper.update(setmeal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        return setmealMapper.list(setmeal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DishItemVO> getDishItemListBySetmealId(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }
}
