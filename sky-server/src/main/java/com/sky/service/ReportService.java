package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 营业额数据统计
     * @param begin 开始时间
     * @param end 结束时间
     * @return TurnoverReportVO 营业额数据统计结果
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户数据统计
     * @param begin 开始时间
     * @param end 结束时间
     * @return UserReportVO 用户数据统计结果
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单数据统计
     * @param begin 开始时间
     * @param end 结束时间
     * @return OrderReportVO 订单数据统计结果
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);
}
