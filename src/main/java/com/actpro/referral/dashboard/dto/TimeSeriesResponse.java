package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesResponse {

    private Long campaignId;
    private String campaignName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String granularity;
    private List<TimeSeriesDataPoint> dataPoints;
}
