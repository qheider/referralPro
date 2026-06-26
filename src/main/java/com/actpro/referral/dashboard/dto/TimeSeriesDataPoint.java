package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSeriesDataPoint {

    private LocalDate date;
    private Long referrals;
    private Long clicks;
    private Long conversions;
    private Long rewards;
}
