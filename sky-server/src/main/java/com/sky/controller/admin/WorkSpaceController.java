package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/workspace")
@Api(tags = "工作台相关接口")
@Slf4j
public class WorkSpaceController {

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 查询今日运营数据
     * @return Result<WorkSpaceVO> 工作台数据
     */
    @GetMapping("/businessData")
    @ApiOperation("查询今日运营数据")
    public Result<BusinessDataVO> businessData() {
        // 获取当前时间
        LocalDateTime beginTime = LocalDateTime.now().with(LocalDateTime.MIN);
        LocalDateTime endTime = LocalDateTime.now().with(LocalDateTime.MAX);
        log.info("查询今日运营数据");
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(beginTime, endTime);
        return Result.success(businessDataVO);
    }

    /**
     * 查询订单管理数据
     * @return Result<OrderOverViewVO> 订单管理数据
     */
    @GetMapping("/overviewOrders")
    @ApiOperation("查询订单管理数据")
    public Result<OrderOverViewVO> overviewOrders() {
        log.info("查询订单管理数据");
        OrderOverViewVO orderOverViewVO = workspaceService.getOverviewOrders();
        return Result.success(orderOverViewVO);
    }

    /**
     * 查询菜品总览
     * @return Result<DishOverViewVO> 菜品总览数据
     */
    @GetMapping("/overviewDishes")
    @ApiOperation("查询菜品总览")
    public Result<DishOverViewVO> overviewDishes() {
        log.info("查询菜品总览");
        DishOverViewVO dishOverViewVO = workspaceService.getOverviewDishes();
        return Result.success(dishOverViewVO);
    }

    /**
     * 查询套餐总览
     * @return Result<SetmealOverViewVO> 套餐总览数据
     */
    @GetMapping("/overviewSetmeals")
    @ApiOperation("查询套餐总览")
    public Result<SetmealOverViewVO> overviewSetmeals() {
        log.info("查询套餐总览");
        SetmealOverViewVO setmealOverViewVO = workspaceService.getOverviewSetmeals();
        return Result.success(setmealOverViewVO);
    }
}
