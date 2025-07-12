package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetMealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端-套餐浏览接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetMealService setmealService;

    /**
     * 条件查询
     * @param categoryId 分类id
     * @return Result<List<Setmeal>> 套餐列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<Setmeal>> list(Long categoryId) {
        log.info("【用户端】根据分类id查询套餐:{}", categoryId);
        // 构造查询条件
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);
        // 查询套餐数据
        List<Setmeal> list = setmealService.list(setmeal);
        // 返回结果
        return Result.success(list);
    }

    /**
     * 根据套餐id查询包含的菜品列表
     * @param setmealId 套餐id
     * @return Result<List<DishItemVO>> 菜品列表
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long setmealId) {
        log.info("【用户端】根据套餐id查询包含的菜品列表:{}", setmealId);
        List<DishItemVO> list = setmealService.getDishItemListBySetmealId(setmealId);
        return Result.success(list);
    }
}
