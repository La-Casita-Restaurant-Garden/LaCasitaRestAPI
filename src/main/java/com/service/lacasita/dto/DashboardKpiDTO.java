package com.service.lacasita.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardKpiDTO {

    private Double totalOrders;
    private Long totalCustomers;
    private Double totalRevenue;
}