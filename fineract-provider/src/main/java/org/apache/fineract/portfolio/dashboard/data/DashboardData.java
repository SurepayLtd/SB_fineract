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
package org.apache.fineract.portfolio.dashboard.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Dashboard Analytics Data Transfer Object for SACCO SureBanker
 */
@Getter
@Builder
public class DashboardData implements Serializable {

    private static final long serialVersionUID = 1L;

    // Report metadata
    private final LocalDate reportDate;
    private final LocalDate periodStartDate;
    private final LocalDate periodEndDate;

    // 1. Executive Snapshot
    private final ExecutiveSnapshotData executiveSnapshot;

    // 2. Core Risk & Portfolio Health
    private final PortfolioHealthData portfolioHealth;

    // 3. Liquidity & Funding
    private final LiquidityFundingData liquidityFunding;

    // 4. Business Performance
    private final BusinessPerformanceData businessPerformance;

    // 5. Member & Savings Insights
    private final MemberSavingsInsightsData memberSavingsInsights;

    // 6. Group & Savings Insights
    private final GroupSavingsInsightsData groupSavingsInsightsData;

    // 7. Profitability & Sustainability
    private final ProfitabilitySustainabilityData profitabilitySustainability;

    /**
     * 1. Executive Snapshot - High-level absolute values showing overall SACCO position
     */
    @Getter
    @Builder
    public static class ExecutiveSnapshotData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Total Loan Portfolio - Sum of all active loan balances
        private final BigDecimal totalLoanPortfolio;

        // Total Deposits / Savings - Sum of all savings account balances
        private final BigDecimal totalDeposits;

        // Active Members - Count of active clients
        private final Long activeMembers;

        // Active Groups - Count of active groups
        private final Long activeGroups;

        // Net Position (Inflow – Outflow) - Total Collections + Deposits – (Loan Disbursements + Withdrawals)
        private final BigDecimal netPosition;

        // Supporting details
        private final BigDecimal totalCollections;
        private final BigDecimal totalNewDeposits;
        private final BigDecimal totalDisbursements;
        private final BigDecimal totalWithdrawals;
    }

    /**
     * 2. Core Risk & Portfolio Health - Measures quality and risk of the loan portfolio
     */
    @Getter
    @Builder
    public static class PortfolioHealthData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Portfolio at Risk (PAR > 30 days) - Outstanding balance of loans overdue >30 days ÷ Total loan portfolio
        private final BigDecimal portfolioAtRisk30;
        private final BigDecimal parAmount30;

        // Arrears Aging Distribution
        private final ArrearsAgingData arrearsAging;

        // Collection Efficiency Ratio - Actual Collections ÷ Expected Collections
        private final BigDecimal collectionEfficiencyRatio;
        private final BigDecimal actualCollections;
        private final BigDecimal expectedCollections;

        // Loan Loss Provision Coverage Ratio - Loan Loss Provisions ÷ PAR amount
        private final BigDecimal loanLossProvisionCoverageRatio;
        private final BigDecimal loanLossProvisions;
    }

    /**
     * Arrears Aging Distribution by time buckets
     */
    @Getter
    @Builder
    public static class ArrearsAgingData implements Serializable {
        private static final long serialVersionUID = 1L;

        // 1-30 days overdue
        private final BigDecimal bucket1To30Days;
        private final Long count1To30Days;

        // 31-60 days overdue
        private final BigDecimal bucket31To60Days;
        private final Long count31To60Days;

        // 61-90 days overdue
        private final BigDecimal bucket61To90Days;
        private final Long count61To90Days;

        // 90+ days overdue
        private final BigDecimal bucket90PlusDays;
        private final Long count90PlusDays;

        // Total arrears
        private final BigDecimal totalArrears;
        private final Long totalCount;
    }

    /**
     * 3. Liquidity & Funding - Ability to meet obligations and fund operations
     */
    @Getter
    @Builder
    public static class LiquidityFundingData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Available Cash (Cash + Bank + Mobile Wallets)
        private final BigDecimal availableCash;
        private final BigDecimal cashOnHand;
        private final BigDecimal bankBalance;
        private final BigDecimal mobileWalletBalance;

        // Liquidity Ratio - Liquid Assets ÷ Short-Term Liabilities
        private final BigDecimal liquidityRatio;
        private final BigDecimal liquidAssets;
        private final BigDecimal shortTermLiabilities;

        // Loan-to-Deposit Ratio (LDR) - Total Loans ÷ Total Deposits
        private final BigDecimal loanToDepositRatio;

        // Expected Net Cash Flow - Expected Inflows – Expected Outflows
        private final BigDecimal expectedNetCashFlow;
        private final BigDecimal expectedInflows;  // scheduled repayments
        private final BigDecimal expectedOutflows; // approved pending disbursements
    }

    /**
     * 4. Business Performance - Operational activity and execution
     */
    @Getter
    @Builder
    public static class BusinessPerformanceData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Loans Disbursed (MTD) - Sum of disbursed loan amounts (month-to-date)
        private final BigDecimal loansDisbursedMtd;
        private final Long loansDisbursedCountMtd;

        // Collections vs Target (MTD) - Actual Collections vs Defined Target
        private final BigDecimal actualCollectionsMtd;
        private final BigDecimal targetCollectionsMtd;
        private final BigDecimal collectionVsTargetPercentage;

        // Average Loan Size - Total Disbursed Amount ÷ Number of Loans Disbursed
        private final BigDecimal averageLoanSize;
    }

    /**
     * 5. Member & Savings Insights - Member engagement and savings behaviour
     */
    @Getter
    @Builder
    public static class MemberSavingsInsightsData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Dormant Members - Members with no transactions over 60 days
        private final Long dormantMembers;
        private final Integer dormancyPeriodDays;

        // Active Borrower Ratio - Members with Active Loans ÷ Total Members
        private final BigDecimal activeBorrowerRatio;
        private final Long membersWithActiveLoans;
        private final Long groupsWithActiveLoans;
        private final Long totalMembers;
        private final Long totalGroups;

        // Average Savings per Member - Total Savings ÷ Total Members
        private final BigDecimal averageSavingsPerMember;
        private final BigDecimal totalSavings;
    }

    /**
     * 6. Profitability & Sustainability - Long-term financial health (Ratios Only)
     */
    @Getter
    @Builder
    public static class ProfitabilitySustainabilityData implements Serializable {
        private static final long serialVersionUID = 1L;

        // Return on Assets (ROA) - Net Income ÷ Total Assets
        private final BigDecimal returnOnAssets;
        private final BigDecimal netIncome;
        private final BigDecimal totalAssets;

        // Capital Adequacy Ratio - Equity ÷ Total Assets
        private final BigDecimal capitalAdequacyRatio;
        private final BigDecimal equity;

        // Portfolio per Loan Officer - Total Loan Portfolio ÷ Number of Loan Officers
        private final BigDecimal portfolioPerLoanOfficer;
        private final Long numberOfLoanOfficers;
    }

    /**
     * 8. Group & Savings Insights - Group engagement and savings behaviour
     */
    @Getter
    @Builder
    public static class GroupSavingsInsightsData implements Serializable{
        private static final long serialVersionUID = 1L;

        // Dormant Groups - Groups with no transactions over 60 days
        private final Long dormantGroups;
        private final Integer dormancyPeriodDays;

        // Active Borrower Ratio - Groups with Active Loans ÷ Total Groups
        private final BigDecimal activeBorrowerRatio;
        private final Long groupsWithActiveLoans;
        private final Long totalGroups;

        // Average Savings per Group - Total Savings ÷ Total Groups
        private final BigDecimal averageSavingsPerGroup;
        private final BigDecimal totalSavings;
    }
}

