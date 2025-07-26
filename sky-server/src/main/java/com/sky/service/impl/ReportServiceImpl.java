package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = generateDateList(begin, end);

        // 存放每天的营业额
        List<Double> turnoverList = dateList.stream().map(date -> {
            // 根据时间范围和订单状态统计营业额
            Double turnover = getTurnoverByTimeAndStatus(date, begin, end, Orders.COMPLETED);
            return turnover == null ? 0.0 : turnover;
        }).collect(Collectors.toList());

        // 封装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
    
    /**
     * 根据时间区间和订单状态统计营业额
     * @param date 日期
     * @param begin 开始日期
     * @param end 结束日期
     * @param status 订单状态
     * @return 营业额
     */
    private Double getTurnoverByTimeAndStatus(LocalDate date, LocalDate begin, LocalDate end, Integer status) {
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(beginTime, endTime, status);
        return orderMapper.sumByMap(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = generateDateList(begin, end);

        // 存放每天的用户总量
        List<Integer> totalUserList = new ArrayList<>();
        // 存放每天的新增用户数量
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询每天的用户总量（截止到当天结束）
            Integer totalUser = getTotalUserCountByDate(date);
            totalUserList.add(totalUser);

            // 查询每天的新增用户数量
            Integer newUser = getNewUserCountByDate(date);
            newUserList.add(newUser);
        }

        // 封装返回结果
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 根据日期统计用户总量（截止到指定日期）
     * @param date 指定日期
     * @return 用户总量
     */
    private Integer getTotalUserCountByDate(LocalDate date) {
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(null, endTime, null);
        return userMapper.countByMap(map);
    }

    /**
     * 根据日期统计新增用户数量（当天新增）
     * @param date 指定日期
     * @return 新增用户数量
     */
    private Integer getNewUserCountByDate(LocalDate date) {
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(beginTime, endTime, null);
        return userMapper.countByMap(map);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 存放从begin到end范围内的每天的日期
        List<LocalDate> dateList = generateDateList(begin, end);

        // 存放每天的订单数量
        List<Integer> orderCountList = new ArrayList<>();
        // 存放每天的有效订单数量
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 查询每天的订单数量
            Integer orderCount = getOrderCountByDate(date);
            orderCountList.add(orderCount);

            // 查询每天的有效订单数量
            Integer validOrderCount = getValidOrderCountByDate(date);
            validOrderCountList.add(validOrderCount);
        }

        // 订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        // 有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);
        // 订单完成率
        Double orderCompletionRate = totalOrderCount > 0 ? (double) validOrderCount / totalOrderCount : 0.0;

        // 封装返回结果
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据日期统计订单数量
     * @param date 指定日期
     * @return 订单数量
     */
    private Integer getOrderCountByDate(LocalDate date) {
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(beginTime, endTime, null);
        return orderMapper.countByMap(map);
    }

    /**
     * 根据日期统计有效订单数量
     * @param date 指定日期
     * @return 有效订单数量
     */
    private Integer getValidOrderCountByDate(LocalDate date) {
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(beginTime, endTime, Orders.COMPLETED);
        return orderMapper.countByMap(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        // 存放商品名称列表
        List<String> nameList = new ArrayList<>();
        // 存放销量列表
        List<Integer> numberList = new ArrayList<>();

        // 查询销量前10的商品
        List<GoodsSalesDTO> salesTop10 = getSalesTop10ByTimeRange(begin, end);
        
        // 将商品名称和销量分别存放到对应的列表中
        for (GoodsSalesDTO goodsSalesDTO : salesTop10) {
            nameList.add(goodsSalesDTO.getName());
            numberList.add(goodsSalesDTO.getNumber());
        }

        // 封装返回结果
        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 根据时间范围统计销量前10的商品
     * @param begin 开始日期
     * @param end 结束日期
     * @return 销量前10的商品列表
     */
    private List<GoodsSalesDTO> getSalesTop10ByTimeRange(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        Map<String, Object> map = buildQueryMap(beginTime, endTime, null);
        return orderMapper.getSalesTop10ByMap(map);
    }

    // ==================== 公共方法 ====================

    /**
     * 生成从开始日期到结束日期的日期列表
     * @param begin 开始日期
     * @param end 结束日期
     * @return 日期列表
     */
    private List<LocalDate> generateDateList(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }

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
