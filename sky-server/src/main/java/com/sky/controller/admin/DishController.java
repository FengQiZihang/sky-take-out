package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    /**
     * 新增菜品
     * @param dishDTO 菜品DTO
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO 菜品分页查询DTO
     * @return Result<PageResult> 分页查询结果
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids 菜品ID列表
     * @throws DeletionNotAllowedException 起售中的菜品不能删除
     * @throws DeletionNotAllowedException 当前菜品关联了套餐,不能删除
     */
    @DeleteMapping
    @ApiOperation("批量删除菜品")
    public Result<String> delete(@RequestParam List<Long> ids) {
        log.info("批量删除菜品:{}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据ID查询菜品
     * @param id 菜品ID
     * @return 菜品VO
     */
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据ID查询菜品:{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDTO 菜品DTO
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result<String> update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品:{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 起售或停售菜品
     * @param status 状态 0 停售 1 起售
     * @param id 菜品ID
     */
    @PostMapping("/status/{status}")
    @ApiOperation("起售或停售菜品")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        log.info("起售或停售菜品: status:{}, id:{}", status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类id
     * @return Result<List<Dish>> 菜品列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getListByCategoryId(Long categoryId) {
        log.info("根据分类id查询菜品:{}", categoryId);
        List<Dish> list = dishService.getListByCategoryId(categoryId);
        return Result.success(list);
    }

}
