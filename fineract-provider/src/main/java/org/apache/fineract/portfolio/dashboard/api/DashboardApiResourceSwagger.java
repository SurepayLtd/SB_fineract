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
package org.apache.fineract.portfolio.dashboard.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Swagger documentation for Dashboard API responses.
 */
final class DashboardApiResourceSwagger {

    private DashboardApiResourceSwagger() {}

    @Schema(description = "GetDashboardResponse")
    public static final class GetDashboardResponse {

        @Schema(example = "2024-01-31")
        public LocalDate reportDate;
        @Schema(example = "2024-01-01")
        public LocalDate periodStartDate;
        @Schema(example = "2024-01-31")
        public LocalDate periodEndDate;
        public GetExecutiveSnapshotResponse executiveSnapshot;
        public GetPortfolioHealthResponse portfolioHealth;
        public GetLiquidityFundingResponse liquidityFunding;
        public GetBusinessPerformanceResponse businessPerformance;
        public GetMemberSavingsInsightsResponse memberSavingsInsights;
        public GetGroupSavingsInsightsResponse groupSavingsInsights;
        public GetProfitabilitySustainabilityResponse profitabilitySustainability;
        public GetShareInsightsResponse shareInsightsResponse;
    }

    @Schema(description = "GetExecutiveSnapshotResponse")
    public static final class GetExecutiveSnapshotResponse {

        @Schema(example = "50000000.00", description = "Total outstanding balance of all active loans")
        public BigDecimal totalLoanPortfolio;
        @Schema(example = "30000000.00", description = "Total member savings held by the SACCO")
        public BigDecimal totalDeposits;
        @Schema(example = "1500", description = "Total number of members with active accounts")
        public Long activeMembers;
        @Schema(example = "5000000.00", description = "Net cash movement over selected period (Collections + Deposits - Disbursements - Withdrawals)")
        public BigDecimal netPosition;
        @Schema(example = "8000000.00", description = "Total loan repayments collected")
        public BigDecimal totalCollections;
        @Schema(example = "3000000.00", description = "Total new deposits received")
        public BigDecimal totalNewDeposits;
        @Schema(example = "5000000.00", description = "Total loans disbursed")
        public BigDecimal totalDisbursements;
        @Schema(example = "1000000.00", description = "Total savings withdrawals")
        public BigDecimal totalWithdrawals;
    }

    @Schema(description = "GetPortfolioHealthResponse")
    public static final class GetPortfolioHealthResponse {

        @Schema(example = "5.25", description = "Portfolio at Risk > 30 days as percentage")
        public BigDecimal portfolioAtRisk30;
        @Schema(example = "2625000.00", description = "Outstanding balance of loans overdue > 30 days")
        public BigDecimal parAmount30;
        public ArrearsAgingResponse arrearsAging;
        @Schema(example = "92.50", description = "Actual Collections / Expected Collections as percentage")
        public BigDecimal collectionEfficiencyRatio;
        @Schema(example = "7400000.00", description = "Actual collections received")
        public BigDecimal actualCollections;
        @Schema(example = "8000000.00", description = "Expected collections based on schedule")
        public BigDecimal expectedCollections;
        @Schema(example = "85.00", description = "Loan Loss Provisions / PAR amount as percentage")
        public BigDecimal loanLossProvisionCoverageRatio;
        @Schema(example = "2231250.00", description = "Loan loss provisions amount")
        public BigDecimal loanLossProvisions;
    }

    @Schema(description = "ArrearsAgingResponse")
    public static final class ArrearsAgingResponse {

        @Schema(example = "500000.00", description = "Overdue amount 1-30 days")
        public BigDecimal bucket1To30Days;
        @Schema(example = "25", description = "Count of loans 1-30 days overdue")
        public Long count1To30Days;
        @Schema(example = "750000.00", description = "Overdue amount 31-60 days")
        public BigDecimal bucket31To60Days;
        @Schema(example = "15", description = "Count of loans 31-60 days overdue")
        public Long count31To60Days;
        @Schema(example = "625000.00", description = "Overdue amount 61-90 days")
        public BigDecimal bucket61To90Days;
        @Schema(example = "10", description = "Count of loans 61-90 days overdue")
        public Long count61To90Days;
        @Schema(example = "1250000.00", description = "Overdue amount 90+ days")
        public BigDecimal bucket90PlusDays;
        @Schema(example = "20", description = "Count of loans 90+ days overdue")
        public Long count90PlusDays;
        @Schema(example = "3125000.00", description = "Total arrears amount")
        public BigDecimal totalArrears;
        @Schema(example = "70", description = "Total count of loans in arrears")
        public Long totalCount;
    }

    @Schema(description = "GetLiquidityFundingResponse")
    public static final class GetLiquidityFundingResponse {

        @Schema(example = "5000000.00", description = "Cash + Bank + Mobile Wallets")
        public BigDecimal availableCash;
        @Schema(example = "1000000.00", description = "Cash on hand")
        public BigDecimal cashOnHand;
        @Schema(example = "3500000.00", description = "Bank balance")
        public BigDecimal bankBalance;
        @Schema(example = "500000.00", description = "Mobile wallet balance")
        public BigDecimal mobileWalletBalance;
        @Schema(example = "16.67", description = "Liquid Assets / Short-Term Liabilities as percentage")
        public BigDecimal liquidityRatio;
        @Schema(example = "5000000.00", description = "Total liquid assets")
        public BigDecimal liquidAssets;
        @Schema(example = "30000000.00", description = "Short-term liabilities (deposits)")
        public BigDecimal shortTermLiabilities;
        @Schema(example = "166.67", description = "Total Loans / Total Deposits as percentage")
        public BigDecimal loanToDepositRatio;
        @Schema(example = "3000000.00", description = "Expected Inflows - Expected Outflows")
        public BigDecimal expectedNetCashFlow;
        @Schema(example = "8000000.00", description = "Expected inflows (scheduled repayments)")
        public BigDecimal expectedInflows;
        @Schema(example = "5000000.00", description = "Expected outflows (approved pending disbursements)")
        public BigDecimal expectedOutflows;
    }

    @Schema(description = "GetBusinessPerformanceResponse")
    public static final class GetBusinessPerformanceResponse {

        @Schema(example = "5000000.00", description = "Total loans disbursed month-to-date")
        public BigDecimal loansDisbursedMtd;
        @Schema(example = "45", description = "Number of loans disbursed month-to-date")
        public Long loansDisbursedCountMtd;
        @Schema(example = "7400000.00", description = "Actual collections month-to-date")
        public BigDecimal actualCollectionsMtd;
        @Schema(example = "8000000.00", description = "Target collections month-to-date")
        public BigDecimal targetCollectionsMtd;
        @Schema(example = "92.50", description = "Collections vs Target as percentage")
        public BigDecimal collectionVsTargetPercentage;
        @Schema(example = "111111.11", description = "Average loan size disbursed")
        public BigDecimal averageLoanSize;
    }

    @Schema(description = "GetMemberSavingsInsightsResponse")
    public static final class GetMemberSavingsInsightsResponse {

        @Schema(example = "150", description = "Members with no transactions over 60 days")
        public Long dormantMembers;
        @Schema(example = "60", description = "Dormancy period in days")
        public Integer dormancyPeriodDays;
        @Schema(example = "45.50", description = "Members with Active Loans / Total Members as percentage")
        public BigDecimal activeBorrowerRatio;
        @Schema(example = "682", description = "Number of members with active loans")
        public Long membersWithActiveLoans;
        @Schema(example = "1500", description = "Total number of members")
        public Long totalMembers;
        @Schema(example = "20000.00", description = "Total Individual Savings / Total Members")
        public BigDecimal averageSavingsPerMember;
        @Schema(example = "30000000.00", description = "Total savings balance")
        public BigDecimal totalSavings;
    }

    @Schema(description = "GetGroupSavingsInsightsResponse")
    public static final class GetGroupSavingsInsightsResponse {

        @Schema(example = "150", description = "Groups with no transactions over 60 days")
        public Long dormantGroups;
        @Schema(example = "60", description = "Dormancy period in days")
        public Integer dormancyPeriodDays;
        @Schema(example = "45.50", description = "Members with Active Loans / Total Members as percentage")
        public BigDecimal groupBorrowerRatio;
        @Schema(example = "682", description = "Number of groups with active loans")
        public Long groupsWithActiveLoans;
        @Schema(example = "1500", description = "Total number of groups")
        public Long totalGroups;
        @Schema(example = "20000.00", description = "Total Group Savings / Total Groups")
        public BigDecimal averageSavingsPerGroup;
        @Schema(example = "30000000.00", description = "Total savings balance")
        public BigDecimal totalSavings;
    }

    @Schema(description = "GetShareInsightsResponse")
    public static final class GetShareInsightsResponse {

        @Schema(example = "150", description = "Total Share Accounts in the Sacco")
        public Long totalShareAccounts;
        @Schema(example = "60", description = "Active share accounts in the sacco")
        public Long activeShareAccounts;
        @Schema(example = "45", description = "Members with Active Shares")
        public Long membersWithShares;
        @Schema(example = "6.O", description = "Members with Shares/Total Sacco clients")
        public BigDecimal shareParticipationRate;
        @Schema(example = "1500", description = "Total approved shares of the sacco")
        public Long totalApprovedShares;
        @Schema(example = "1500", description = "Total pending shares of the sacco")
        public Long totalPendingShares;
        @Schema(example = "20000.00", description = "Total approved shares * unit price per share")
        public BigDecimal totalShareCapitalValue;
        @Schema(example = "20", description = "Total share products of the sacco")
        public Long totalShareProducts;
        @Schema(example = "15", description = "Total issued shares from all share products of the sacco")
        public Long totalIssuedShares;
        @Schema(example = "150", description = "Total subscribed shares from all share products of the sacco")
        public Long totalSubscribedShares;
        @Schema(example = "19", description = "Total issued shares - Total subscribed shares")
        public Long availableShares;
        @Schema(example = "19", description = "Client with highest shares")
        public Long largestShareholderShares;
        @Schema(example = "John Doe", description = "Client Details with highest shares")
        public String largestShareholderName;
        @Schema(example = "3000.00", description = "Client with highest Shares/Total Approved Shares %")
        public Double ownershipConcentrationPercentage;
        @Schema(example = "19", description = "Shares issued this month")
        public Long newShareAccountsThisMonth;
        @Schema(example = "19", description = "Value of Shares purchased this month")
        public Long sharesPurchasedThisMonth;
        @Schema(example = "19", description = "Share Capital Growth for current month")
        public BigDecimal shareCapitalGrowthThisMonth;
    }

    @Schema(description = "GetProfitabilitySustainabilityResponse")
    public static final class GetProfitabilitySustainabilityResponse {

        @Schema(example = "2.50", description = "Net Income / Total Assets as percentage")
        public BigDecimal returnOnAssets;
        @Schema(example = "1375000.00", description = "Net income for the period")
        public BigDecimal netIncome;
        @Schema(example = "55000000.00", description = "Total assets")
        public BigDecimal totalAssets;
        @Schema(example = "18.18", description = "Equity / Total Assets as percentage")
        public BigDecimal capitalAdequacyRatio;
        @Schema(example = "10000000.00", description = "Total equity")
        public BigDecimal equity;
        @Schema(example = "5000000.00", description = "Total Loan Portfolio / Number of Loan Officers")
        public BigDecimal portfolioPerLoanOfficer;
        @Schema(example = "10", description = "Number of loan officers")
        public Long numberOfLoanOfficers;
    }
}

