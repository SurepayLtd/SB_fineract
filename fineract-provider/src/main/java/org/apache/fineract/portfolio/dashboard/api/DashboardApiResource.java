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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.dashboard.data.DashboardData;
import org.apache.fineract.portfolio.dashboard.service.DashboardReadPlatformService;
import org.springframework.stereotype.Component;

/**
 * REST API Resource for SACCO Dashboard Analytics (SureBanker Dashboard).
 */
@Path("/v1/dashboard")
@Component
@Tag(name = "Dashboard", description = "SACCO Dashboard Analytics API providing comprehensive metrics in a single endpoint:\n\n"
        + "**1. Executive Snapshot** - High-level absolute values\n"
        + "   - Total Loan Portfolio, Total Deposits, Active Members, Net Position\n\n"
        + "**2. Core Risk & Portfolio Health** - Loan portfolio quality and risk\n"
        + "   - PAR > 30 days, Arrears Aging (1-30, 31-60, 61-90, 90+ days), Collection Efficiency, Loan Loss Provision Coverage\n\n"
        + "**3. Liquidity & Funding** - Ability to meet obligations\n"
        + "   - Available Cash, Liquidity Ratio, Loan-to-Deposit Ratio, Expected Net Cash Flow\n\n"
        + "**4. Business Performance** - Operational activity\n"
        + "   - Loans Disbursed MTD, Collections vs Target, Average Loan Size\n\n"
        + "**5. Member & Savings Insights** - Member engagement\n"
        + "   - Dormant Members, Active Borrower Ratio, Average Savings per Member\n\n"
        + "**6. Group & Savings Insights** - Group engagement\n"
        + "   - Dormant Groups, Group Borrower Ratio, Average Savings per Group\n\n"
        + "**7. Profitability & Sustainability** - Long-term financial health\n"
        + "   - ROA, Capital Adequacy Ratio, Portfolio per Loan Officer")
@RequiredArgsConstructor
public class DashboardApiResource {

    private static final String RESOURCE_NAME_FOR_PERMISSIONS = "DASHBOARD";

    private final PlatformSecurityContext context;
    private final DashboardReadPlatformService dashboardReadPlatformService;
    private final DefaultToApiJsonSerializer<DashboardData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;

    @GET
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve Complete Dashboard Analytics",
            description = "Retrieves all SACCO dashboard analytics data in a single API call.\n\n"
            + "## Response Sections\n\n"
            + "### 1. Executive Snapshot\n"
            + "- **totalLoanPortfolio**: Sum of all active loan balances\n"
            + "- **totalDeposits**: Sum of all savings account balances\n"
            + "- **activeMembers**: Count of active clients\n"
            + "- **netPosition**: Total Collections + Deposits - (Disbursements + Withdrawals)\n\n"
            + "### 2. Portfolio Health\n"
            + "- **portfolioAtRisk30**: PAR > 30 days as percentage\n"
            + "- **arrearsAging**: Distribution by buckets (1-30, 31-60, 61-90, 90+ days)\n"
            + "- **collectionEfficiencyRatio**: Actual Collections / Expected Collections\n"
            + "- **loanLossProvisionCoverageRatio**: Provisions / PAR amount\n\n"
            + "### 3. Liquidity & Funding\n"
            + "- **availableCash**: Cash + Bank + Mobile Wallets\n"
            + "- **liquidityRatio**: Liquid Assets / Short-Term Liabilities\n"
            + "- **loanToDepositRatio**: Total Loans / Total Deposits\n"
            + "- **expectedNetCashFlow**: Expected Inflows - Expected Outflows\n\n"
            + "### 4. Business Performance\n"
            + "- **loansDisbursedMtd**: Total disbursed amount (month-to-date)\n"
            + "- **collectionVsTargetPercentage**: Actual vs Target collections\n"
            + "- **averageLoanSize**: Total Disbursed / Number of Loans\n\n"
            + "### 5. Member & Savings Insights\n"
            + "- **dormantMembers**: Members inactive for 60+ days\n"
            + "- **activeBorrowerRatio**: Members with Active Loans / Total Members\n"
            + "- **averageSavingsPerMember**: Total Member Savings / Total Members\n\n"
            + "### 6. Group & Savings Insights\n"
            + "- **dormantGroups**: Groups inactive for 60+ days\n"
            + "- **activeBorrowerRatio**: Groups with Active Loans / Total Groups\n"
            + "- **averageSavingsPerGroups**: Total Groups Savings / Total Groups\n\n"
            + "### 7. Profitability & Sustainability\n"
            + "- **returnOnAssets**: Net Income / Total Assets\n"
            + "- **capitalAdequacyRatio**: Equity / Total Assets\n"
            + "- **portfolioPerLoanOfficer**: Total Portfolio / Number of Loan Officers\n\n"
            + "## Example Requests\n"
            + "- `GET /v1/dashboard` - Default: current month data for all offices\n"
            + "- `GET /v1/dashboard?startDate=2024-01-01&endDate=2024-01-31` - Custom date range\n"
            + "- `GET /v1/dashboard?officeId=1` - Specific office only\n"
            + "- `GET /v1/dashboard?startDate=2024-01-01&endDate=2024-01-31&officeId=1` - Custom range + office")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = DashboardApiResourceSwagger.GetDashboardResponse.class))) })
    public String retrieveDashboard(@Context final UriInfo uriInfo,
            @QueryParam("startDate") @Parameter(description = "Start date for the reporting period (format: yyyy-MM-dd). Defaults to first day of current month.") final String startDateStr,
            @QueryParam("endDate") @Parameter(description = "End date for the reporting period (format: yyyy-MM-dd). Defaults to today.") final String endDateStr,
            @QueryParam("officeId") @Parameter(description = "Office ID to filter data. If not provided, data for all offices is returned.") final Long officeId) {

        this.context.authenticatedUser().validateHasReadPermission(RESOURCE_NAME_FOR_PERMISSIONS);

        DashboardData dashboardData;

        if (startDateStr != null && endDateStr != null) {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            dashboardData = this.dashboardReadPlatformService.retrieveDashboardData(startDate, endDate, officeId);
        } else if (officeId != null) {
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            dashboardData = this.dashboardReadPlatformService.retrieveDashboardData(startOfMonth, today, officeId);
        } else {
            dashboardData = this.dashboardReadPlatformService.retrieveDashboardData();
        }

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, dashboardData);
    }
}

