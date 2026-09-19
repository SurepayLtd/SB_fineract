package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import liquibase.sqlgenerator.SqlGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchSmSReadServiceImpl implements BatchSmSReadService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;


    /**
     * The source account for this job is the loan account.
     *
     * Loan account ids are therefore used as the partition key.
     *
     * One loan account can never be split across two partitions.
     */
    private static final String SOURCE_ACCOUNT_KEY = "rs.loan_id";
    private static final String SAVINGS_ACCOUNT_KEY = "sa.id";

    private String dueInstallmentPredicate() {

        return """
                rs.duedate IN (?, ?, ?, ?)
                """;
    }

    private String overDueInstallmentPredicate() {

        return """
                rs.duedate IN (?, ?, ?, ?, ?)
                """;
    }

    @Override
    public List<JobPartition> retrieveLoanDuePartitions(final int partitionSize) {

        final LocalDate businessDate = DateUtils.getBusinessLocalDate();

        final List<Object> params = new ArrayList<>();
        params.add(partitionSize);

        params.add(businessDate.plusDays(7));
        params.add(businessDate.plusDays(3));
        params.add(businessDate.plusDays(1));
        params.add(businessDate);

        final StringBuilder sqlBuilder = new StringBuilder(800);

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("MIN(accountKey) AS minKey, ");
        sqlBuilder.append("MAX(accountKey) AS maxKey, ");
        sqlBuilder.append("page, ");
        sqlBuilder.append("SUM(partitionCount) AS partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("FLOOR((ROW_NUMBER() OVER (ORDER BY accountKey) - 1) / ?) AS page, ");
        sqlBuilder.append("accountKey, ");
        sqlBuilder.append("partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(SOURCE_ACCOUNT_KEY).append(" AS accountKey, ");
        sqlBuilder.append("COUNT(*) AS partitionCount ");
        sqlBuilder.append("FROM m_loan_repayment_schedule rs ");
        sqlBuilder.append("JOIN m_loan l ON l.id = rs.loan_id ");
        sqlBuilder.append("JOIN m_client c ON c.id = l.client_id ");

        sqlBuilder.append("WHERE ");
        sqlBuilder.append(dueInstallmentPredicate());

        sqlBuilder.append(" AND l.loan_status_id = 300 ");
        sqlBuilder.append(" AND c.mobile_no IS NOT NULL ");
        sqlBuilder.append(" AND TRIM(c.mobile_no) <> '' ");

        sqlBuilder.append(" AND (");
        sqlBuilder.append("COALESCE(rs.principal_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_amount,0)");
        sqlBuilder.append(") > (");
        sqlBuilder.append("COALESCE(rs.principal_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_completed_derived,0)");
        sqlBuilder.append(") ");

        sqlBuilder.append("GROUP BY ").append(SOURCE_ACCOUNT_KEY);

        sqlBuilder.append(") accounts ");
        sqlBuilder.append(") buckets ");
        sqlBuilder.append("GROUP BY page ");
        sqlBuilder.append("ORDER BY page");

        return jdbcTemplate.query(sqlBuilder.toString(),
                (rs, rowNum) -> new JobPartition(
                        rs.getLong("minKey"),
                        rs.getLong("maxKey"),
                        rs.getLong("page"),
                        rs.getLong("partitionCount")),
                params.toArray());
    }

    @Override
    public List<LoanInstallmentReminderData> retrieveLoanDuePage(final Long minAccountKey, final Long maxAccountKey, final LocalDate afterDueDate, final Long afterId, final int limit) {

        final LocalDate businessDate = DateUtils.getBusinessLocalDate();

        final List<Object> params = new ArrayList<>();

        final StringBuilder sqlBuilder = new StringBuilder(1200);

        LoanInstallmentReminderMapper mp = new LoanInstallmentReminderMapper();

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(mp.schema());

        sqlBuilder.append(" WHERE ");
        sqlBuilder.append(dueInstallmentPredicate());

        params.add(businessDate.plusDays(7));
        params.add(businessDate.plusDays(3));
        params.add(businessDate.plusDays(1));
        params.add(businessDate);

        if (minAccountKey != null && maxAccountKey != null) {
            sqlBuilder.append(" AND ").append(SOURCE_ACCOUNT_KEY).append(" BETWEEN ? AND ? ");
            params.add(minAccountKey);
            params.add(maxAccountKey);
        }

        sqlBuilder.append(" AND l.loan_status_id = 300 ");
        sqlBuilder.append(" AND c.mobile_no IS NOT NULL ");
        sqlBuilder.append(" AND TRIM(c.mobile_no) <> '' ");

        sqlBuilder.append(" AND (");
        sqlBuilder.append("COALESCE(rs.principal_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_amount,0)");
        sqlBuilder.append(") > (");
        sqlBuilder.append("COALESCE(rs.principal_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_completed_derived,0)");
        sqlBuilder.append(") ");

        /*
         * Keyset pagination using (duedate, installment_id)
         */
        if (afterDueDate != null && afterId != null) {
            sqlBuilder.append(" AND (");
            sqlBuilder.append("rs.duedate > ? ");
            sqlBuilder.append("OR (rs.duedate = ? AND rs.id > ?)");
            sqlBuilder.append(") ");

            params.add(afterDueDate);
            params.add(afterDueDate);
            params.add(afterId);
        }

        sqlBuilder.append(" ORDER BY rs.duedate ASC, rs.id ASC ");
        sqlBuilder.append(" LIMIT ?");

        params.add(limit);

        return jdbcTemplate.query(sqlBuilder.toString(), mp, params.toArray());
    }

    @Override
    public List<JobPartition> retrieveLoanOverDuePartitions(int partitionSize) {
        final LocalDate businessDate = DateUtils.getBusinessLocalDate();

        final List<Object> params = new ArrayList<>();
        params.add(partitionSize);

        params.add(businessDate.minusDays(90));
        params.add(businessDate.minusDays(60));
        params.add(businessDate.minusDays(30));
        params.add(businessDate.minusDays(7));
        params.add(businessDate.minusDays(1));

        final StringBuilder sqlBuilder = new StringBuilder(800);

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("MIN(accountKey) AS minKey, ");
        sqlBuilder.append("MAX(accountKey) AS maxKey, ");
        sqlBuilder.append("page, ");
        sqlBuilder.append("SUM(partitionCount) AS partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("FLOOR((ROW_NUMBER() OVER (ORDER BY accountKey) - 1) / ?) AS page, ");
        sqlBuilder.append("accountKey, ");
        sqlBuilder.append("partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(SOURCE_ACCOUNT_KEY).append(" AS accountKey, ");
        sqlBuilder.append("COUNT(*) AS partitionCount ");
        sqlBuilder.append("FROM m_loan_repayment_schedule rs ");
        sqlBuilder.append("JOIN m_loan l ON l.id = rs.loan_id ");
        sqlBuilder.append("JOIN m_client c ON c.id = l.client_id ");

        sqlBuilder.append("WHERE ");
        sqlBuilder.append(overDueInstallmentPredicate());

        sqlBuilder.append(" AND l.loan_status_id = 300 ");
        sqlBuilder.append(" AND c.mobile_no IS NOT NULL ");
        sqlBuilder.append(" AND TRIM(c.mobile_no) <> '' ");

        sqlBuilder.append(" AND (");
        sqlBuilder.append("COALESCE(rs.principal_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_amount,0)");
        sqlBuilder.append(") > (");
        sqlBuilder.append("COALESCE(rs.principal_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_completed_derived,0)");
        sqlBuilder.append(") ");

        sqlBuilder.append("GROUP BY ").append(SOURCE_ACCOUNT_KEY);

        sqlBuilder.append(") accounts ");
        sqlBuilder.append(") buckets ");
        sqlBuilder.append("GROUP BY page ");
        sqlBuilder.append("ORDER BY page");

        return jdbcTemplate.query(sqlBuilder.toString(),
                (rs, rowNum) -> new JobPartition(
                        rs.getLong("minKey"),
                        rs.getLong("maxKey"),
                        rs.getLong("page"),
                        rs.getLong("partitionCount")),
                params.toArray());
    }

    @Override
    public List<LoanInstallmentOverdueReminderData> retrieveLoanOverDuePage(Long minAccountKey, Long maxAccountKey, LocalDate afterDueDate, Long afterId, int limit) {
        final LocalDate businessDate = DateUtils.getBusinessLocalDate();

        final List<Object> params = new ArrayList<>();

        final StringBuilder sqlBuilder = new StringBuilder(1200);

        LoanInstallmentOverDueMapper mp = new LoanInstallmentOverDueMapper();

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(mp.schema());

        sqlBuilder.append(" WHERE ");
        sqlBuilder.append(overDueInstallmentPredicate());

        params.add(businessDate.minusDays(90)); // D+90
        params.add(businessDate.minusDays(60)); // D+60
        params.add(businessDate.minusDays(30)); // D+30
        params.add(businessDate.minusDays(7));  // D+7
        params.add(businessDate.minusDays(1));  // D+1

        if (minAccountKey != null && maxAccountKey != null) {
            sqlBuilder.append(" AND ").append(SOURCE_ACCOUNT_KEY).append(" BETWEEN ? AND ? ");
            params.add(minAccountKey);
            params.add(maxAccountKey);
        }

        sqlBuilder.append(" AND l.loan_status_id = 300 ");
        sqlBuilder.append(" AND c.mobile_no IS NOT NULL ");
        sqlBuilder.append(" AND TRIM(c.mobile_no) <> '' ");

        sqlBuilder.append(" AND (");
        sqlBuilder.append("COALESCE(rs.principal_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_amount,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_amount,0)");
        sqlBuilder.append(") > (");
        sqlBuilder.append("COALESCE(rs.principal_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.interest_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.fee_charges_completed_derived,0)");
        sqlBuilder.append(" + COALESCE(rs.penalty_charges_completed_derived,0)");
        sqlBuilder.append(") ");

        /*
         * Keyset pagination using (duedate, installment_id)
         */
        if (afterDueDate != null && afterId != null) {
            sqlBuilder.append(" AND (");
            sqlBuilder.append("rs.duedate > ? ");
            sqlBuilder.append("OR (rs.duedate = ? AND rs.id > ?)");
            sqlBuilder.append(") ");

            params.add(afterDueDate);
            params.add(afterDueDate);
            params.add(afterId);
        }

        sqlBuilder.append(" ORDER BY rs.duedate ASC, rs.id ASC ");
        sqlBuilder.append(" LIMIT ?");

        params.add(limit);

        return jdbcTemplate.query(sqlBuilder.toString(), mp, params.toArray());
    }

    @Override
    public List<JobPartition> retrieveDormantPartitions(final int partitionSize) {

        final StringBuilder sqlBuilder = new StringBuilder(800);

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("MIN(accountKey) AS minKey, ");
        sqlBuilder.append("MAX(accountKey) AS maxKey, ");
        sqlBuilder.append("page, ");
        sqlBuilder.append("SUM(partitionCount) AS partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("FLOOR((ROW_NUMBER() OVER (ORDER BY accountKey) - 1) / ?) AS page, ");
        sqlBuilder.append("accountKey, ");
        sqlBuilder.append("partitionCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(SAVINGS_ACCOUNT_KEY).append(" AS accountKey, ");
        sqlBuilder.append("COUNT(*) AS partitionCount ");

        sqlBuilder.append("FROM m_savings_account sa ");

        sqlBuilder.append("JOIN m_savings_product sp ");
        sqlBuilder.append("ON sp.id = sa.product_id ");

        sqlBuilder.append("WHERE sa.status_enum = 300 ");
        sqlBuilder.append("AND sa.sub_status_enum IN (0, 100, 200) ");

        sqlBuilder.append("GROUP BY ").append(SAVINGS_ACCOUNT_KEY);

        sqlBuilder.append(") accounts ");
        sqlBuilder.append(") buckets ");

        sqlBuilder.append("GROUP BY page ");
        sqlBuilder.append("ORDER BY page");

        return jdbcTemplate.query(
                sqlBuilder.toString(),
                (rs, rowNum) -> new JobPartition(
                        rs.getLong("minKey"),
                        rs.getLong("maxKey"),
                        rs.getLong("page"),
                        rs.getLong("partitionCount")
                ),
                partitionSize
        );
    }

    @Override
    public List<SavingsDormancyReminderData> retrieveDormancyPage(Long minAccountKey, Long maxAccountKey, Integer afterReminderDays, Long afterSavingsId, int limit) {
        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        String lastTransactionDate = """
            (
                select COALESCE(
                    max(sat.transaction_date),
                    sa.activatedon_date
                )
                from m_savings_account_transaction sat
                where sat.is_reversed = false
                  and sat.is_reversal = false
                  and sat.transaction_type_enum in (1, 2)
                  and sat.savings_account_id = sa.id
            )
            """;

        StringBuilder sql = new StringBuilder();

        sql.append("select x.savings_id, ");
        sql.append("x.client_id, ");
        sql.append("x.account_number, ");
        sql.append("x.mobile_no, ");
        sql.append("x.client_name, ");
        sql.append("x.last_transaction_date, ");
        sql.append("x.inactive_days, ");
        sql.append("x.reminder_days ");

        sql.append("from ( ");

        /*
         * DEFAULT DORMANCY
         */
        sql.append("select sa.id as savings_id, ");
        sql.append("c.id as client_id, ");
        sql.append("sa.account_no as account_number, ");
        sql.append("c.mobile_no as mobile_no, ");
        sql.append("c.display_name as client_name, ");
        sql.append(lastTransactionDate);
        sql.append(" as last_transaction_date, ");

        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" as inactive_days, ");

        sql.append("case ");
        sql.append("when ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= 90 then 90 ");
        sql.append("when ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= 60 then 60 ");
        sql.append("when ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= 30 then 30 ");
        sql.append("end as reminder_days ");

        sql.append("from m_savings_account sa ");
        sql.append("inner join m_savings_product sp on sp.id = sa.product_id ");
        sql.append("inner join m_client c on c.id = sa.client_id ");

        sql.append("where sp.is_dormancy_tracking_active = false ");
        sql.append("and sa.status_enum = 300 ");
        sql.append("and sa.sub_status_enum = 0 ");

        sql.append("union all ");

        /*
         * DORMANCY ENABLED - INACTIVE
         */
        sql.append("select sa.id as savings_id, ");
        sql.append("c.id as client_id, ");
        sql.append("sa.account_no as account_number, ");
        sql.append("c.mobile_no as mobile_no, ");
        sql.append("c.display_name as client_name, ");
        sql.append(lastTransactionDate);
        sql.append(" as last_transaction_date, ");

        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" as inactive_days, ");

        sql.append("30 as reminder_days ");

        sql.append("from m_savings_account sa ");
        sql.append("inner join m_savings_product sp ");
        sql.append("on sa.product_id = sp.id ");
        sql.append("and sp.is_dormancy_tracking_active = true ");

        sql.append("inner join m_client c on c.id = sa.client_id ");

        sql.append("where sa.status_enum = 300 ");
        sql.append("and sa.sub_status_enum = 0 ");
        sql.append("and ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= sp.days_to_inactive ");

        sql.append("union all ");

        /*
         * =========================================================
         * DORMANCY ENABLED - DORMANT
         * =========================================================
         */
        sql.append("select sa.id as savings_id, ");
        sql.append("c.id as client_id, ");
        sql.append("sa.account_no as account_number, ");
        sql.append("c.mobile_no as mobile_no, ");
        sql.append("c.display_name as client_name, ");
        sql.append(lastTransactionDate);
        sql.append(" as last_transaction_date, ");

        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" as inactive_days, ");

        sql.append("60 as reminder_days ");

        sql.append("from m_savings_account sa ");
        sql.append("inner join m_savings_product sp ");
        sql.append("on sa.product_id = sp.id ");
        sql.append("and sp.is_dormancy_tracking_active = true ");

        sql.append("inner join m_client c on c.id = sa.client_id ");

        sql.append("where sa.status_enum = 300 ");
        sql.append("and sa.sub_status_enum = 100 ");
        sql.append("and ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= sp.days_to_dormancy ");

        sql.append("union all ");

        /*
         * DORMANCY ENABLED - ESCHEAT
         */
        sql.append("select sa.id as savings_id, ");
        sql.append("c.id as client_id, ");
        sql.append("sa.account_no as account_number, ");
        sql.append("c.mobile_no as mobile_no, ");
        sql.append("c.display_name as client_name, ");
        sql.append(lastTransactionDate);
        sql.append(" as last_transaction_date, ");

        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" as inactive_days, ");

        sql.append("90 as reminder_days ");

        sql.append("from m_savings_account sa ");
        sql.append("inner join m_savings_product sp ");
        sql.append("on sa.product_id = sp.id ");
        sql.append("and sp.is_dormancy_tracking_active = true ");

        sql.append("inner join m_client c on c.id = sa.client_id ");

        sql.append("where sa.status_enum = 300 ");
        sql.append("and sa.sub_status_enum = 200 ");
        sql.append("and ");
        sql.append(sqlGenerator.dateDiff("?", lastTransactionDate));
        sql.append(" >= sp.days_to_escheat ");

        sql.append(") x ");

        sql.append("where x.savings_id between ? and ? ");
        sql.append("and x.reminder_days is not null ");

        if (afterReminderDays != null && afterSavingsId != null) {
            sql.append("and ( ");
            sql.append("x.reminder_days > ? ");
            sql.append("or (x.reminder_days = ? and x.savings_id > ?) ");
            sql.append(") ");
        }

        sql.append("order by x.reminder_days, x.savings_id ");
        sql.append("limit ? ");

        /*
         * Parameters must follow the exact order of the
         * dateDiff("?") occurrences above.
         */
        List<Object> params = new ArrayList<>();

        // Default dormancy
        params.add(businessDate);
        params.add(businessDate);
        params.add(businessDate);
        params.add(businessDate);

        // Inactive
        params.add(businessDate);
        params.add(businessDate);

        // Dormant
        params.add(businessDate);
        params.add(businessDate);

        // Escheat
        params.add(businessDate);
        params.add(businessDate);

        params.add(minAccountKey);
        params.add(maxAccountKey);

        if (afterReminderDays != null && afterSavingsId != null) {
            params.add(afterReminderDays);
            params.add(afterReminderDays);
            params.add(afterSavingsId);
        }

        params.add(limit);

        return this.jdbcTemplate.query(sql.toString(), new SavingsDormancyReminderMapper(), params.toArray()
        );
    }

    protected static class LoanInstallmentReminderMapper implements RowMapper<LoanInstallmentReminderData> {

        public String schema() {

            return """
            rs.id AS installment_id,
            rs.loan_id,
            l.client_id,
            rs.installment,
            rs.duedate,
            l.total_outstanding_derived,
            CASE
                WHEN rs.duedate = DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 7
                WHEN rs.duedate = DATE_ADD(CURDATE(), INTERVAL 3 DAY) THEN 3
                WHEN rs.duedate = DATE_ADD(CURDATE(), INTERVAL 1 DAY) THEN 1
                ELSE 0
            END AS reminder_days,
            rs.principal_amount,
            rs.interest_amount,
            rs.fee_charges_amount,
            rs.penalty_charges_amount,
            (
                COALESCE(rs.principal_amount,0)
                + COALESCE(rs.interest_amount,0)
                + COALESCE(rs.fee_charges_amount,0)
                + COALESCE(rs.penalty_charges_amount,0)
            ) AS total_due,
            c.mobile_no,
            c.display_name AS client_name
            FROM m_loan_repayment_schedule rs
            JOIN m_loan l ON l.id = rs.loan_id
            JOIN m_client c ON c.id = l.client_id
            """;
            //CONCAT_WS(' ', c.firstname, c.middlename, c.lastname)
        }

        @Override
        public LoanInstallmentReminderData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            return new LoanInstallmentReminderData(
                    rs.getLong("loan_id"),
                    rs.getLong("client_id"),
                    rs.getLong("installment_id"),
                    rs.getInt("installment"),
                    rs.getObject("duedate", LocalDate.class),
                    rs.getInt("reminder_days"),
                    rs.getBigDecimal("principal_amount"),
                    rs.getBigDecimal("interest_amount"),
                    rs.getBigDecimal("fee_charges_amount"),
                    rs.getBigDecimal("penalty_charges_amount"),
                    rs.getBigDecimal("total_due"),
                    rs.getBigDecimal("total_outstanding_derived"),
                    rs.getString("mobile_no"),
                    rs.getString("client_name")
            );
        }
    }

    protected static class SavingsDormancyReminderMapper implements RowMapper<SavingsDormancyReminderData> {

        @Override
        public SavingsDormancyReminderData mapRow(ResultSet rs, int rowNum) throws SQLException {

            return new SavingsDormancyReminderData(
                    rs.getLong("savings_id"),
                    rs.getObject("client_id", Long.class),
                    rs.getString("account_number"),
                    rs.getString("mobile_no"),
                    rs.getString("client_name"),
                    rs.getObject("last_transaction_date", java.sql.Date.class) != null
                            ? rs.getDate("last_transaction_date").toLocalDate()
                            : null,
                    rs.getLong("inactive_days"),
                    rs.getInt("reminder_days")
            );
        }
    }

    protected class LoanInstallmentOverDueMapper implements RowMapper<LoanInstallmentOverdueReminderData> {

        public String schema() {

            return """
                    rs.id AS installment_id,
                    rs.loan_id,
                    l.client_id,
                    rs.installment,
                    rs.duedate,
                    CASE
                    WHEN rs.duedate = DATE_SUB(CURDATE(), INTERVAL 90 DAY) THEN 90
                    WHEN rs.duedate = DATE_SUB(CURDATE(), INTERVAL 60 DAY) THEN 60
                    WHEN rs.duedate = DATE_SUB(CURDATE(), INTERVAL 30 DAY) THEN 30
                    WHEN rs.duedate = DATE_SUB(CURDATE(), INTERVAL 7 DAY) THEN 7
                    WHEN rs.duedate = DATE_SUB(CURDATE(), INTERVAL 1 DAY) THEN 1
                    ELSE 0
                    END AS overdue_days,
                    rs.principal_amount,
                    rs.interest_amount,
                    rs.fee_charges_amount,
                    rs.penalty_charges_amount,
                    (
                    COALESCE(rs.principal_amount,0)
                    + COALESCE(rs.interest_amount,0)
                    + COALESCE(rs.fee_charges_amount,0)
                    + COALESCE(rs.penalty_charges_amount,0)
                    ) AS total_due,
                    c.mobile_no,
                    c.display_name AS client_name
                    FROM m_loan_repayment_schedule rs
                    JOIN m_loan l ON l.id = rs.loan_id
                    JOIN m_client c ON c.id = l.client_id
            """;
        }

        @Override
        public LoanInstallmentOverdueReminderData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            return new LoanInstallmentOverdueReminderData(
                    rs.getLong("loan_id"),
                    rs.getLong("client_id"),
                    rs.getLong("installment_id"),
                    rs.getInt("installment"),
                    rs.getObject("duedate", LocalDate.class),
                    rs.getInt("overdue_days"),
                    rs.getBigDecimal("principal_amount"),
                    rs.getBigDecimal("interest_amount"),
                    rs.getBigDecimal("fee_charges_amount"),
                    rs.getBigDecimal("penalty_charges_amount"),
                    rs.getBigDecimal("total_due"),
                    rs.getString("mobile_no"),
                    rs.getString("client_name")
            );
        }
    }

}
