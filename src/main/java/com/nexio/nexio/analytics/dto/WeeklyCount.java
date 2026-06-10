package com.nexio.nexio.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WeeklyCount {
    private String week;
    private long count;
}
