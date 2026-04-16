/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.dashboard.service;

import java.time.LocalDate;
import org.apache.fineract.portfolio.dashboard.data.DashboardData;

/**
 * Service interface for retrieving SACCO Dashboard Analytics data.
 */
public interface DashboardReadPlatformService {

    /**
     * Retrieves complete dashboard data for the given period.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data (null for all offices)
     * @return DashboardData containing all dashboard metrics
     */
    DashboardData retrieveDashboardData(LocalDate startDate, LocalDate endDate, Long officeId);

    /**
     * Retrieves dashboard data using default period (current month).
     * 
     * @return DashboardData containing all dashboard metrics
     */
    DashboardData retrieveDashboardData();

    /**
     * Retrieves only the Executive Snapshot section of the dashboard.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data
     * @return ExecutiveSnapshotData
     */
    DashboardData.ExecutiveSnapshotData retrieveExecutiveSnapshot(LocalDate startDate, LocalDate endDate, Long officeId);

    /**
     * Retrieves only the Portfolio Health section of the dashboard.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data
     * @return PortfolioHealthData
     */
    DashboardData.PortfolioHealthData retrievePortfolioHealth(LocalDate startDate, LocalDate endDate, Long officeId);

    /**
     * Retrieves only the Liquidity & Funding section of the dashboard.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data
     * @return LiquidityFundingData
     */
    DashboardData.LiquidityFundingData retrieveLiquidityFunding(LocalDate startDate, LocalDate endDate, Long officeId);

    /**
     * Retrieves only the Business Performance section of the dashboard.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data
     * @return BusinessPerformanceData
     */
    DashboardData.BusinessPerformanceData retrieveBusinessPerformance(LocalDate startDate, LocalDate endDate, Long officeId);

    /**
     * Retrieves only the Member & Savings Insights section of the dashboard.
     * 
     * @param officeId optional office ID to filter data
     * @return MemberSavingsInsightsData
     */
    DashboardData.MemberSavingsInsightsData retrieveMemberSavingsInsights(Long officeId);

    /**
     * Retrieves only the Group & Savings Insights section of the dashboard.
     *
     * @param officeId optional office ID to filter data
     * @return GroupSavingsInsightsData
     */
    DashboardData.GroupSavingsInsightsData retrieveGroupSavingsInsights(Long officeId);

    /**
     * Retrieves only the Profitability & Sustainability section of the dashboard.
     * 
     * @param startDate the start date of the reporting period
     * @param endDate the end date of the reporting period
     * @param officeId optional office ID to filter data
     * @return ProfitabilitySustainabilityData
     */
    DashboardData.ProfitabilitySustainabilityData retrieveProfitabilitySustainability(LocalDate startDate, LocalDate endDate, Long officeId);
}

