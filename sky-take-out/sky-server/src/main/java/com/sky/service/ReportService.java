package com.sky.service;

import com.sky.dto.TurnoverReportDTO;
import com.sky.vo.TurnoverReportVO;

public interface ReportService {

    /**
     * 营业额统计时间区间内
     * @param turnoverReportDTO
     * @return
     */
    TurnoverReportVO getTurnoverReport(TurnoverReportDTO turnoverReportDTO);
}
