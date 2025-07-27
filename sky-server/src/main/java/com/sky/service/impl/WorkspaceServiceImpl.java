package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public BusinessDataVO getBusinessData() {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */
        // 获取当前时间
        LocalDate today = LocalDate.now();
        LocalDateTime beginTime = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(today, LocalTime.MAX);

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
