package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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
    @Autowired
    private WorkspaceService workspaceService;
    
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX)
        );

        // 2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 填入时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            // 获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX)
                );

                // 获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + "运营数据报表.xlsx");
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
