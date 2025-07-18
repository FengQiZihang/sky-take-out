package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Api(tags = "C端-菜品浏览接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     * @param categoryId 分类id
     * @return Result<List<DishVO>> 菜品视图对象列表
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("【用户端】根据分类id查询菜品:{}", categoryId);
        // 构造redis中的key，规则：dish_分类id
        String key = "dish_" + categoryId;

        // 查询redis是否缓存了菜品数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        // 如果redis中存在菜品数据，则直接返回
        if (list != null && list.size() > 0) {
            log.info("【用户端】从redis中获取菜品数据:{}", list);
            return Result.success(list);
        }

        // 构造查询条件
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        // 查询菜品数据
        log.info("【用户端】从mysql中获取菜品数据");
        list = dishService.listWithFlavor(dish);

        // 将菜品数据缓存到redis中
        redisTemplate.opsForValue().set(key, list);
        log.info("【用户端】将菜品数据缓存到redis中:{}", list);

        // 返回结果
        return Result.success(list);
    }

}
