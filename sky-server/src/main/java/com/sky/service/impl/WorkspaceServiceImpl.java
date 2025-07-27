package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDateTime beginTime, LocalDateTime endTime) {
        // 营业额：当日已完成订单的总金额
        Map<String, Object> completedMap = buildQueryMap(beginTime, endTime, Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(completedMap);
        turnover = turnover == null ? 0.0 : turnover;

        // 有效订单：当日已完成订单的数量
        Integer validOrderCount = orderMapper.countByMap(completedMap);

        // 总订单数：当日所有订单数量
        Map<String, Object> allMap = buildQueryMap(beginTime, endTime, null);
        Integer allOrderCount = orderMapper.countByMap(allMap);

        // 订单完成率：有效订单数 / 总订单数
        Double orderCompletionRate = allOrderCount > 0 ? (double) validOrderCount / allOrderCount : 0.0;

        // 平均客单价：营业额 / 有效订单数
        Double unitPrice = validOrderCount > 0 ? turnover / validOrderCount : 0.0;

        // 新增用户：当日新增用户的数量
        Map<String, Object> newUserMap = buildQueryMap(beginTime, endTime, null);
        Integer newUsers = userMapper.countByMap(newUserMap);

        // 封装返回结果
        return BusinessDataVO
                .builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderOverViewVO getOverviewOrders() {
        // 获取当前时间
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

        // 待接单数量
        Map<String, Object> toBeConfirmedMap = buildQueryMap(beginTime, endTime, Orders.TO_BE_CONFIRMED);
        Integer waitingOrders = orderMapper.countByMap(toBeConfirmedMap);

        // 待派送数量
        Map<String, Object> confirmedMap = buildQueryMap(beginTime, endTime, Orders.CONFIRMED);
        Integer deliveredOrders = orderMapper.countByMap(confirmedMap);

        // 已完成数量
        Map<String, Object> completedMap = buildQueryMap(beginTime, endTime, Orders.COMPLETED);
        Integer completedOrders = orderMapper.countByMap(completedMap);

        // 已取消数量
        Map<String, Object> cancelledMap = buildQueryMap(beginTime, endTime, Orders.CANCELLED);
        Integer cancelledOrders = orderMapper.countByMap(cancelledMap);

        // 全部订单数量
        Map<String, Object> allMap = buildQueryMap(beginTime, endTime, null);
        Integer allOrders = orderMapper.countByMap(allMap);

        // 封装返回结果
        return OrderOverViewVO
                .builder()
                .waitingOrders(waitingOrders)
                .deliveredOrders(deliveredOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DishOverViewVO getOverviewDishes() {
        // 已启售数量
        Map<String, Object> soldMap = new HashMap<>();
        soldMap.put("status", StatusConstant.ENABLE);
        Integer sold = dishMapper.countByMap(soldMap);

        // 已停售数量
        Map<String, Object> discontinuedMap = new HashMap<>();
        discontinuedMap.put("status", StatusConstant.DISABLE);
        Integer discontinued = dishMapper.countByMap(discontinuedMap);

        // 封装返回结果
        return DishOverViewVO
                .builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SetmealOverViewVO getOverviewSetmeals() {
        // 已启售数量
        Map<String, Object> soldMap = new HashMap<>();
        soldMap.put("status", StatusConstant.ENABLE);
        Integer sold = setmealMapper.countByMap(soldMap);

        // 已停售数量
        Map<String, Object> discontinuedMap = new HashMap<>();
        discontinuedMap.put("status", StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.countByMap(discontinuedMap);

        // 封装返回结果
        return SetmealOverViewVO
                .builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    // ==================== 公共方法 ====================

    /**
     * 构建查询Map
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param status 状态（可为null）
     * @return 查询Map
     */
    private Map<String, Object> buildQueryMap(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        if (status != null) {
            map.put("status", status);
        }
        return map;
    }
}
