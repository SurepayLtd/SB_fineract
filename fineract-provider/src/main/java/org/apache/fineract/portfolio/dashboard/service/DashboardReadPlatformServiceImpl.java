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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.dashboard.data.DashboardData;
import org.apache.fineract.portfolio.group.domain.GroupingTypeStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.savings.SavingsAccountTransactionType;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountStatusType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of DashboardReadPlatformService for SACCO Dashboard Analytics.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardReadPlatformServiceImpl implements DashboardReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;

    private static final int DORMANCY_PERIOD_DAYS = 60;
    private static final int SCALE = 4;

    @Override
    public DashboardData retrieveDashboardData(LocalDate startDate, LocalDate endDate, Long officeId) {
        this.context.authenticatedUser();

        final LocalDate reportDate = DateUtils.getBusinessLocalDate();

        return DashboardData.builder()
                .reportDate(reportDate)
                .periodStartDate(startDate)
                .periodEndDate(endDate)
                .executiveSnapshot(retrieveExecutiveSnapshot(startDate, endDate, officeId))
                .portfolioHealth(retrievePortfolioHealth(startDate, endDate, officeId))
                .liquidityFunding(retrieveLiquidityFunding(startDate, endDate, officeId))
                .businessPerformance(retrieveBusinessPerformance(startDate, endDate, officeId))
                .memberSavingsInsights(retrieveMemberSavingsInsights(officeId))
                .groupSavingsInsightsData(retrieveGroupSavingsInsights(officeId))
                .profitabilitySustainability(retrieveProfitabilitySustainability(startDate, endDate, officeId))
                .build();
    }

    @Override
    public DashboardData retrieveDashboardData() {
        final LocalDate today = DateUtils.getBusinessLocalDate();
        final LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        return retrieveDashboardData(startOfMonth, today, null);
    }

    @Override
    public DashboardData.ExecutiveSnapshotData retrieveExecutiveSnapshot(LocalDate startDate, LocalDate endDate, Long officeId) {
        // Total Loan Portfolio - Sum of all active loan balances
        BigDecimal totalLoanPortfolio = getTotalLoanPortfolio(officeId);

        // Total Deposits / Savings - Sum of all savings account balances
        BigDecimal totalDeposits = getMemberDeposits(officeId).add(getGroupDeposits(officeId));

        // Active Members - Count of active clients
        Long activeMembers = getActiveMembers(officeId);

        //Active Groups - Count of active Groups
        Long activeGroups = getActiveGroups(officeId);

        // Net Position calculations
        BigDecimal totalCollections = getTotalCollections(startDate, endDate, officeId);
        BigDecimal totalNewDeposits = getTotalNewDeposits(startDate, endDate, officeId);
        BigDecimal totalDisbursements = getTotalDisbursements(startDate, endDate, officeId);
        BigDecimal totalWithdrawals = getTotalWithdrawals(startDate, endDate, officeId);

        // Net Position = Total Collections + Deposits – (Loan Disbursements + Withdrawals)
        BigDecimal netPosition = totalCollections.add(totalNewDeposits)
                .subtract(totalDisbursements).subtract(totalWithdrawals);

        return DashboardData.ExecutiveSnapshotData.builder()
                .totalLoanPortfolio(totalLoanPortfolio)
                .totalDeposits(totalDeposits)
                .activeMembers(activeMembers)
                .activeGroups(activeGroups)
                .netPosition(netPosition)
                .totalCollections(totalCollections)
                .totalNewDeposits(totalNewDeposits)
                .totalDisbursements(totalDisbursements)
                .totalWithdrawals(totalWithdrawals)
                .build();
    }

    @Override
    public DashboardData.PortfolioHealthData retrievePortfolioHealth(LocalDate startDate, LocalDate endDate, Long officeId) {
        BigDecimal totalLoanPortfolio = getTotalLoanPortfolio(officeId);

        // PAR > 30 days
        BigDecimal parAmount30 = getParAmount(30, officeId);
        BigDecimal portfolioAtRisk30 = totalLoanPortfolio.compareTo(BigDecimal.ZERO) > 0
                ? parAmount30.divide(totalLoanPortfolio, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Collection Efficiency
        BigDecimal expectedCollections = getExpectedCollections(startDate, endDate, officeId);
        BigDecimal actualCollections = getTotalCollections(startDate, endDate, officeId);
        BigDecimal collectionEfficiencyRatio = expectedCollections.compareTo(BigDecimal.ZERO) > 0
                ? actualCollections.divide(expectedCollections, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Loan Loss Provision Coverage
        BigDecimal loanLossProvisions = getLoanLossProvisions(officeId);
        BigDecimal loanLossProvisionCoverageRatio = parAmount30.compareTo(BigDecimal.ZERO) > 0
                ? loanLossProvisions.divide(parAmount30, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return DashboardData.PortfolioHealthData.builder()
                .portfolioAtRisk30(portfolioAtRisk30)
                .parAmount30(parAmount30)
                .arrearsAging(getArrearsAgingData(officeId))
                .collectionEfficiencyRatio(collectionEfficiencyRatio)
                .actualCollections(actualCollections)
                .expectedCollections(expectedCollections)
                .loanLossProvisionCoverageRatio(loanLossProvisionCoverageRatio)
                .loanLossProvisions(loanLossProvisions)
                .build();
    }

    @Override
    public DashboardData.LiquidityFundingData retrieveLiquidityFunding(LocalDate startDate, LocalDate endDate, Long officeId) {
        // Available Cash components - simplified as actual GL data may vary
        BigDecimal cashOnHand = getCashOnHand(officeId);
        BigDecimal bankBalance = getBankBalance(officeId);
        BigDecimal mobileWalletBalance = getMobileWalletBalance(officeId);
        BigDecimal availableCash = cashOnHand.add(bankBalance).add(mobileWalletBalance);
        BigDecimal totalOverallDeposits = getMemberDeposits(officeId).add(getGroupDeposits(officeId));


        // Liquidity Ratio
        BigDecimal liquidAssets = availableCash;
        BigDecimal shortTermLiabilities = totalOverallDeposits; // Deposits are short-term liabilities
        BigDecimal liquidityRatio = shortTermLiabilities.compareTo(BigDecimal.ZERO) > 0
                ? liquidAssets.divide(shortTermLiabilities, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Loan-to-Deposit Ratio
        BigDecimal totalLoans = getTotalLoanPortfolio(officeId);
        BigDecimal totalDeposits = totalOverallDeposits;
        BigDecimal loanToDepositRatio = totalDeposits.compareTo(BigDecimal.ZERO) > 0
                ? totalLoans.divide(totalDeposits, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Expected Net Cash Flow
        BigDecimal expectedInflows = getExpectedInflows(startDate, endDate, officeId);
        BigDecimal expectedOutflows = getExpectedOutflows(officeId);
        BigDecimal expectedNetCashFlow = expectedInflows.subtract(expectedOutflows);

        return DashboardData.LiquidityFundingData.builder()
                .availableCash(availableCash)
                .cashOnHand(cashOnHand)
                .bankBalance(bankBalance)
                .mobileWalletBalance(mobileWalletBalance)
                .liquidityRatio(liquidityRatio)
                .liquidAssets(liquidAssets)
                .shortTermLiabilities(shortTermLiabilities)
                .loanToDepositRatio(loanToDepositRatio)
                .expectedNetCashFlow(expectedNetCashFlow)
                .expectedInflows(expectedInflows)
                .expectedOutflows(expectedOutflows)
                .build();
    }

    @Override
    public DashboardData.BusinessPerformanceData retrieveBusinessPerformance(LocalDate startDate, LocalDate endDate, Long officeId) {
        // MTD period
        LocalDate mtdStart = endDate.with(TemporalAdjusters.firstDayOfMonth());

        // Loans Disbursed MTD
        BigDecimal loansDisbursedMtd = getTotalDisbursements(mtdStart, endDate, officeId);
        Long loansDisbursedCountMtd = getDisbursementCount(mtdStart, endDate, officeId);

        // Collections MTD
        BigDecimal actualCollectionsMtd = getTotalCollections(mtdStart, endDate, officeId);
        BigDecimal targetCollectionsMtd = getCollectionTarget(mtdStart, endDate, officeId);
        BigDecimal collectionVsTargetPercentage = targetCollectionsMtd.compareTo(BigDecimal.ZERO) > 0
                ? actualCollectionsMtd.divide(targetCollectionsMtd, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Average Loan Size
        BigDecimal averageLoanSize = loansDisbursedCountMtd > 0
                ? loansDisbursedMtd.divide(BigDecimal.valueOf(loansDisbursedCountMtd), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardData.BusinessPerformanceData.builder()
                .loansDisbursedMtd(loansDisbursedMtd)
                .loansDisbursedCountMtd(loansDisbursedCountMtd)
                .actualCollectionsMtd(actualCollectionsMtd)
                .targetCollectionsMtd(targetCollectionsMtd)
                .collectionVsTargetPercentage(collectionVsTargetPercentage)
                .averageLoanSize(averageLoanSize)
                .build();
    }

    @Override
    public DashboardData.MemberSavingsInsightsData retrieveMemberSavingsInsights(Long officeId) {
        Long totalMembers = getTotalMembers(officeId);
        Long dormantMembers = getDormantMembers(officeId);
        Long membersWithActiveLoans = getMembersWithActiveLoans(officeId);
        BigDecimal clientSavings = getMemberDeposits(officeId);

        // Active Borrower Ratio
        BigDecimal activeBorrowerRatio = totalMembers > 0
                ? BigDecimal.valueOf(membersWithActiveLoans)
                        .divide(BigDecimal.valueOf(totalMembers), SCALE, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Average Savings per Member
        BigDecimal averageSavingsPerMember = totalMembers > 0
                ? clientSavings.divide(BigDecimal.valueOf(totalMembers), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardData.MemberSavingsInsightsData.builder()
                .dormantMembers(dormantMembers)
                .dormancyPeriodDays(DORMANCY_PERIOD_DAYS)
                .activeBorrowerRatio(activeBorrowerRatio)
                .membersWithActiveLoans(membersWithActiveLoans)
                .totalMembers(totalMembers)
                .averageSavingsPerMember(averageSavingsPerMember)
                .totalSavings(clientSavings)
                .build();
    }

    @Override
    public DashboardData.GroupSavingsInsightsData retrieveGroupSavingsInsights(Long officeId) {
        Long totalGroups = getTotalGroups(officeId);
        Long dormantGroups = getDormantGroups(officeId);
        Long groupsWithActiveLoans = getGroupsWithActiveLoans(officeId);
        BigDecimal groupDeposits = getGroupDeposits(officeId);


        // Active Group Borrower Ratio
        BigDecimal groupBorrowerRatio = totalGroups > 0
                ? BigDecimal.valueOf(groupsWithActiveLoans)
                .divide(BigDecimal.valueOf(totalGroups), SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;


        // Average Savings per Group
        BigDecimal averageSavingsPerGroup = totalGroups > 0
                ? groupDeposits.divide(BigDecimal.valueOf(totalGroups), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;


        return DashboardData.GroupSavingsInsightsData.builder()
                .dormantGroups(dormantGroups)
                .dormancyPeriodDays(DORMANCY_PERIOD_DAYS)
                .totalGroups(totalGroups)
                .totalSavings(groupDeposits)
                .averageSavingsPerGroup(averageSavingsPerGroup)
                .activeBorrowerRatio(groupBorrowerRatio)
                .build();
    }

    @Override
    public DashboardData.ProfitabilitySustainabilityData retrieveProfitabilitySustainability(
            LocalDate startDate, LocalDate endDate, Long officeId) {

        BigDecimal netIncome = getNetIncome(startDate, endDate, officeId);
        BigDecimal totalAssets = getTotalAssets(officeId);
        BigDecimal equity = getEquity(officeId);
        Long numberOfLoanOfficers = getNumberOfLoanOfficers(officeId);
        BigDecimal totalLoanPortfolio = getTotalLoanPortfolio(officeId);

        // Return on Assets
        BigDecimal returnOnAssets = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? netIncome.divide(totalAssets, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Capital Adequacy Ratio
        BigDecimal capitalAdequacyRatio = totalAssets.compareTo(BigDecimal.ZERO) > 0
                ? equity.divide(totalAssets, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Portfolio per Loan Officer
        BigDecimal portfolioPerLoanOfficer = numberOfLoanOfficers > 0
                ? totalLoanPortfolio.divide(BigDecimal.valueOf(numberOfLoanOfficers), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardData.ProfitabilitySustainabilityData.builder()
                .returnOnAssets(returnOnAssets)
                .netIncome(netIncome)
                .totalAssets(totalAssets)
                .capitalAdequacyRatio(capitalAdequacyRatio)
                .equity(equity)
                .portfolioPerLoanOfficer(portfolioPerLoanOfficer)
                .numberOfLoanOfficers(numberOfLoanOfficers)
                .build();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private BigDecimal getTotalLoanPortfolio(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(l.principal_outstanding_derived + l.interest_outstanding_derived ");
        sql.append("+ l.fee_charges_outstanding_derived + l.penalty_charges_outstanding_derived), 0) ");
        sql.append("FROM m_loan l ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON l.group_id = g.id ");
        }
        sql.append("WHERE l.loan_status_id = ? ");
        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                    LoanStatus.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, LoanStatus.ACTIVE.getValue());
    }

    private BigDecimal getMemberDeposits(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(sa.account_balance_derived), 0) ");
        sql.append("FROM m_savings_account sa ");
        if (officeId != null) {
            sql.append("JOIN m_client c ON sa.client_id = c.id ");
        }
        sql.append("WHERE sa.status_enum = ? ");
        if (officeId != null) {
            sql.append("AND c.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                    SavingsAccountStatusType.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                SavingsAccountStatusType.ACTIVE.getValue());
    }

    private BigDecimal getGroupDeposits(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(sa.account_balance_derived), 0) ");
        sql.append("FROM m_savings_account sa ");
        if (officeId != null) {
            sql.append("JOIN m_group g ON sa.group_id = g.id ");
        }
        sql.append("WHERE sa.status_enum = ? ");
        if (officeId != null) {
            sql.append("AND g.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                    SavingsAccountStatusType.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                SavingsAccountStatusType.ACTIVE.getValue());
    }

    private Long getActiveMembers(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT c.id) FROM m_client c ");
        sql.append("WHERE c.status_enum = ? ");
        if (officeId != null) {
            sql.append("AND c.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    ClientStatus.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, ClientStatus.ACTIVE.getValue());
    }

    private Long getActiveGroups(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT g.id) FROM m_group g ");
        sql.append("WHERE g.status_enum = ? ");
        if (officeId != null) {
            sql.append("AND g.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    GroupingTypeStatus.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, GroupingTypeStatus.ACTIVE.getValue());
    }

    private BigDecimal getTotalCollections(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(lt.amount), 0) FROM m_loan_transaction lt ");
        sql.append("JOIN m_loan l ON lt.loan_id = l.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON l.group_id = g.id ");
        }
        sql.append("WHERE lt.transaction_type_enum IN (?, ?, ?) ");
        sql.append("AND lt.is_reversed = false ");
        sql.append("AND lt.transaction_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(LoanTransactionType.REPAYMENT.getValue());
        params.add(LoanTransactionType.REPAYMENT_AT_DISBURSEMENT.getValue());
        params.add(LoanTransactionType.RECOVERY_REPAYMENT.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
    }

    private BigDecimal getTotalNewDeposits(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(st.amount), 0) FROM m_savings_account_transaction st ");
        sql.append("JOIN m_savings_account sa ON st.savings_account_id = sa.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON sa.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON sa.group_id = g.id ");
        }
        sql.append("WHERE st.transaction_type_enum = ? ");
        sql.append("AND st.is_reversed = false ");
        sql.append("AND st.transaction_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(SavingsAccountTransactionType.DEPOSIT.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
    }

    private BigDecimal getTotalDisbursements(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(lt.amount), 0) FROM m_loan_transaction lt ");
        sql.append("JOIN m_loan l ON lt.loan_id = l.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON g.id = l.group_id ");
        }
        sql.append("WHERE lt.transaction_type_enum = ? ");
        sql.append("AND lt.is_reversed = false ");
        sql.append("AND lt.transaction_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(LoanTransactionType.DISBURSEMENT.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
    }

    private BigDecimal getTotalWithdrawals(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(st.amount), 0) FROM m_savings_account_transaction st ");
        sql.append("JOIN m_savings_account sa ON st.savings_account_id = sa.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON sa.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON sa.group_id = g.id ");
        }
        sql.append("WHERE st.transaction_type_enum = ? ");
        sql.append("AND st.is_reversed = false ");
        sql.append("AND st.transaction_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(SavingsAccountTransactionType.WITHDRAWAL.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
    }

    private BigDecimal getParAmount(int daysOverdue, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(l.principal_outstanding_derived + l.interest_outstanding_derived ");
        sql.append("+ l.fee_charges_outstanding_derived + l.penalty_charges_outstanding_derived), 0) ");
        sql.append("FROM m_loan l ");
        sql.append("JOIN m_loan_arrears_aging laa ON l.id = laa.loan_id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g on g.id = l.group_id ");
        }
        sql.append("WHERE l.loan_status_id = ? ");
        sql.append("AND laa.overdue_since_date_derived <= DATE_SUB(CURDATE(), INTERVAL ? DAY) ");

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                    LoanStatus.ACTIVE.getValue(), daysOverdue, officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                LoanStatus.ACTIVE.getValue(), daysOverdue);
    }

    private DashboardData.ArrearsAgingData getArrearsAgingData(Long officeId) {
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate date30 = today.minusDays(30);
        LocalDate date60 = today.minusDays(60);
        LocalDate date90 = today.minusDays(90);

        StringBuilder baseSql = new StringBuilder();
        baseSql.append("SELECT COALESCE(SUM(laa.total_overdue_derived), 0), COUNT(*) ");
        baseSql.append("FROM m_loan l ");
        baseSql.append("JOIN m_loan_arrears_aging laa ON l.id = laa.loan_id ");
        if (officeId != null) {
            baseSql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            baseSql.append("LEFT JOIN m_group g ON l.group_id = g.id ");
        }
        baseSql.append("WHERE l.loan_status_id = ? AND laa.overdue_since_date_derived ");

        String officeFilter = officeId != null ? " AND COALESCE(c.office_id, g.office_id) = " + officeId : "";

        // 1-30 days
        String sql1to30 = baseSql.toString() + "> ? AND laa.overdue_since_date_derived <= ?" + officeFilter;
        Object[] result1to30 = this.jdbcTemplate.queryForObject(sql1to30,
                (rs, rowNum) -> new Object[]{rs.getBigDecimal(1), rs.getLong(2)},
                LoanStatus.ACTIVE.getValue(), date30, today);

        // 31-60 days
        String sql31to60 = baseSql.toString() + "> ? AND laa.overdue_since_date_derived <= ?" + officeFilter;
        Object[] result31to60 = this.jdbcTemplate.queryForObject(sql31to60,
                (rs, rowNum) -> new Object[]{rs.getBigDecimal(1), rs.getLong(2)},
                LoanStatus.ACTIVE.getValue(), date60, date30);

        // 61-90 days
        String sql61to90 = baseSql.toString() + "> ? AND laa.overdue_since_date_derived <= ?" + officeFilter;
        Object[] result61to90 = this.jdbcTemplate.queryForObject(sql61to90,
                (rs, rowNum) -> new Object[]{rs.getBigDecimal(1), rs.getLong(2)},
                LoanStatus.ACTIVE.getValue(), date90, date60);

        // 90+ days
        String sql90plus = baseSql.toString() + "<= ?" + officeFilter;
        Object[] result90plus = this.jdbcTemplate.queryForObject(sql90plus,
                (rs, rowNum) -> new Object[]{rs.getBigDecimal(1), rs.getLong(2)},
                LoanStatus.ACTIVE.getValue(), date90);

        BigDecimal bucket1To30 = (BigDecimal) result1to30[0];
        BigDecimal bucket31To60 = (BigDecimal) result31to60[0];
        BigDecimal bucket61To90 = (BigDecimal) result61to90[0];
        BigDecimal bucket90Plus = (BigDecimal) result90plus[0];

        return DashboardData.ArrearsAgingData.builder()
                .bucket1To30Days(bucket1To30)
                .count1To30Days((Long) result1to30[1])
                .bucket31To60Days(bucket31To60)
                .count31To60Days((Long) result31to60[1])
                .bucket61To90Days(bucket61To90)
                .count61To90Days((Long) result61to90[1])
                .bucket90PlusDays(bucket90Plus)
                .count90PlusDays((Long) result90plus[1])
                .totalArrears(bucket1To30.add(bucket31To60).add(bucket61To90).add(bucket90Plus))
                .totalCount((Long) result1to30[1] + (Long) result31to60[1] + (Long) result61to90[1] + (Long) result90plus[1])
                .build();
    }

    private BigDecimal getExpectedCollections(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(lrs.principal_amount + lrs.interest_amount ");
        sql.append("+ COALESCE(lrs.fee_charges_amount, 0) + COALESCE(lrs.penalty_charges_amount, 0)), 0) ");
        sql.append("FROM m_loan_repayment_schedule lrs ");
        sql.append("JOIN m_loan l ON lrs.loan_id = l.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON l.group_id = g.id ");
        }
        sql.append("WHERE l.loan_status_id = ? ");
        sql.append("AND lrs.duedate BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(LoanStatus.ACTIVE.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class, params.toArray());
    }

    private BigDecimal getLoanLossProvisions(Long officeId) {
        // This would typically come from GL accounts - simplified here
        return BigDecimal.ZERO;
    }

    private BigDecimal getCashOnHand(Long officeId) {
        // This would come from GL accounts (Cash accounts)
        return BigDecimal.ZERO;
    }

    private BigDecimal getBankBalance(Long officeId) {
        // This would come from GL accounts (Bank accounts)
        return BigDecimal.ZERO;
    }

    private BigDecimal getMobileWalletBalance(Long officeId) {
        // This would come from GL accounts (Mobile wallet accounts)
        return BigDecimal.ZERO;
    }

    private BigDecimal getExpectedInflows(LocalDate startDate, LocalDate endDate, Long officeId) {
        // Expected inflows = scheduled repayments due
        return getExpectedCollections(startDate, endDate, officeId);
    }

    private BigDecimal getExpectedOutflows(Long officeId) {
        // Approved but pending disbursements
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COALESCE(SUM(l.approved_principal), 0) FROM m_loan l ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON l.group_id = g.id ");

        }
        sql.append("WHERE l.loan_status_id = ? ");

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                    LoanStatus.APPROVED.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), BigDecimal.class,
                LoanStatus.APPROVED.getValue());
    }

    private Long getDisbursementCount(LocalDate startDate, LocalDate endDate, Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT lt.loan_id) FROM m_loan_transaction lt ");
        sql.append("JOIN m_loan l ON lt.loan_id = l.id ");
        if (officeId != null) {
            sql.append("LEFT JOIN m_client c ON l.client_id = c.id ");
            sql.append("LEFT JOIN m_group g ON l.group_id = g.id ");
        }
        sql.append("WHERE lt.transaction_type_enum = ? ");
        sql.append("AND lt.is_reversed = false ");
        sql.append("AND lt.transaction_date BETWEEN ? AND ? ");

        List<Object> params = new ArrayList<>();
        params.add(LoanTransactionType.DISBURSEMENT.getValue());
        params.add(startDate);
        params.add(endDate);

        if (officeId != null) {
            sql.append("AND COALESCE(c.office_id, g.office_id) = ? ");
            params.add(officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    }

    private BigDecimal getCollectionTarget(LocalDate startDate, LocalDate endDate, Long officeId) {
        // Collection target - could be from configuration or expected collections
        return getExpectedCollections(startDate, endDate, officeId);
    }

    private Long getTotalMembers(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM m_client c ");
        sql.append("WHERE c.status_enum IN (?, ?) ");

        if (officeId != null) {
            sql.append("AND c.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    ClientStatus.ACTIVE.getValue(), ClientStatus.PENDING.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                ClientStatus.ACTIVE.getValue(), ClientStatus.PENDING.getValue());
    }
    private Long getTotalGroups(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM m_group g ");
        sql.append("WHERE g.status_enum IN (?, ?) ");

        if (officeId != null) {
            sql.append("AND g.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    GroupingTypeStatus.ACTIVE.getValue(), GroupingTypeStatus.PENDING.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                GroupingTypeStatus.ACTIVE.getValue(), GroupingTypeStatus.PENDING.getValue());
    }

    private Long getDormantMembers(Long officeId) {
        LocalDate dormantDate = DateUtils.getBusinessLocalDate().minusDays(DORMANCY_PERIOD_DAYS);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT c.id) FROM m_client c ");
        sql.append("WHERE c.status_enum = ? ");
        sql.append("AND c.id NOT IN ( ");
        sql.append("  SELECT DISTINCT l.client_id FROM m_loan l ");
        sql.append("  JOIN m_loan_transaction lt ON l.id = lt.loan_id ");
        sql.append("  WHERE lt.transaction_date > ? AND lt.is_reversed = false ");
        sql.append("  UNION ");
        sql.append("  SELECT DISTINCT sa.client_id FROM m_savings_account sa ");
        sql.append("  JOIN m_savings_account_transaction st ON sa.id = st.savings_account_id ");
        sql.append("  WHERE st.transaction_date > ? AND st.is_reversed = false ");
        sql.append(") ");

        if (officeId != null) {
            sql.append("AND c.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    ClientStatus.ACTIVE.getValue(), dormantDate, dormantDate, officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                ClientStatus.ACTIVE.getValue(), dormantDate, dormantDate);
    }
    private Long getDormantGroups(Long officeId) {
        LocalDate dormantDate = DateUtils.getBusinessLocalDate().minusDays(DORMANCY_PERIOD_DAYS);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT g.id) FROM m_group g ");
        sql.append("WHERE g.status_enum = ? ");
        sql.append("AND g.id NOT IN ( ");
        sql.append("  SELECT DISTINCT l.group_id FROM m_loan l ");
        sql.append("  JOIN m_loan_transaction lt ON l.id = lt.loan_id ");
        sql.append("  WHERE lt.transaction_date > ? AND lt.is_reversed = false ");
        sql.append("  UNION ");
        sql.append("  SELECT DISTINCT sa.group_id FROM m_savings_account sa ");
        sql.append("  JOIN m_savings_account_transaction st ON sa.id = st.savings_account_id ");
        sql.append("  WHERE st.transaction_date > ? AND st.is_reversed = false ");
        sql.append(") ");

        if (officeId != null) {
            sql.append("AND g.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    GroupingTypeStatus.ACTIVE.getValue(), dormantDate, dormantDate, officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                GroupingTypeStatus.ACTIVE.getValue(), dormantDate, dormantDate);
    }

    private Long getMembersWithActiveLoans(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT l.client_id) FROM m_loan l ");
        if (officeId != null) {
            sql.append("JOIN m_client c ON l.client_id = c.id ");
        }
        sql.append("WHERE l.loan_status_id = ? ");

        if (officeId != null) {
            sql.append("AND c.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    LoanStatus.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, LoanStatus.ACTIVE.getValue());
    }

    private Long getGroupsWithActiveLoans(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(DISTINCT l.group_id) FROM m_loan l ");
        if (officeId != null) {
            sql.append("JOIN m_group g ON l.group_id = g.id ");
        }
        sql.append("WHERE l.loan_status_id = ? ");

        if (officeId != null) {
            sql.append("AND g.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class,
                    LoanStatus.ACTIVE.getValue(), officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, LoanStatus.ACTIVE.getValue());
    }

    private BigDecimal getNetIncome(LocalDate startDate, LocalDate endDate, Long officeId) {
        // This would come from GL accounts - simplified
        return BigDecimal.ZERO;
    }

    private BigDecimal getTotalAssets(Long officeId) {
        // Total Assets = Loan Portfolio + Cash + Other Assets
        BigDecimal loanPortfolio = getTotalLoanPortfolio(officeId);
        BigDecimal cash = getCashOnHand(officeId).add(getBankBalance(officeId)).add(getMobileWalletBalance(officeId));
        return loanPortfolio.add(cash);
    }

    private BigDecimal getEquity(Long officeId) {
        // This would come from GL accounts - simplified
        return BigDecimal.ZERO;
    }

    private Long getNumberOfLoanOfficers(Long officeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM m_staff s ");
        sql.append("WHERE s.is_loan_officer = true AND s.is_active = true ");

        if (officeId != null) {
            sql.append("AND s.office_id = ? ");
            return this.jdbcTemplate.queryForObject(sql.toString(), Long.class, officeId);
        }
        return this.jdbcTemplate.queryForObject(sql.toString(), Long.class);
    }
}

