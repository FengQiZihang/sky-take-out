package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 营业额数据统计
     * @param begin 开始时间
     * @param end 结束时间
     * @return TurnoverReportVO 营业额数据统计结果
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);
}
