package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetMealService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 套餐管理
 */
@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐相关接口")
@Slf4j
public class SetMealController {

    @Autowired
    private SetMealService setMealService;

    /**
     * 新增套餐
     * @param setmealDTO 套餐DTO
     */
    @PostMapping
    @ApiOperation("新增套餐")
    public Result<String> save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);
        setMealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO 套餐分页查询DTO
     * @return Result<PageResult> 分页查询结果
     */
    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询:{}", setmealPageQueryDTO);
        PageResult pageResult = setMealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }
}
