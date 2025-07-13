package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Api(tags = "C端-购物车相关接口")
@Slf4j
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCartDTO 购物车DTO
     */
    @PostMapping("/add")
    @ApiOperation("添加购物车")
    private Result<String> add(@RequestBody ShoppingCartDTO shoppingCartDTO) {
        log.info("【用户端】添加购物车:{}", shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }

    /**
     * 查看购物车
     * @return Result<List<ShoppingCart>> 购物车列表
     */
    @GetMapping("/list")
    @ApiOperation("查看购物车")
    private Result<List<ShoppingCart>> list() {
        log.info("【用户端】查看购物车");
        return Result.success(shoppingCartService.showShoppingCart());
    }
}
