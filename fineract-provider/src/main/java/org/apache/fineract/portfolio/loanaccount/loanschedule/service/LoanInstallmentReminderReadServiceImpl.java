package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderPartition;
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
public class LoanInstallmentReminderReadServiceImpl implements LoanInstallmentReminderReadService{

    private final JdbcTemplate jdbcTemplate;

    /**
     * The source account for this job is the loan account.
     *
     * Loan account ids are therefore used as the partition key.
     *
     * One loan account can never be split across two partitions.
     */
    private static final String SOURCE_ACCOUNT_KEY = "rs.loan_id";

    private String dueInstallmentPredicate() {

        return """
                rs.duedate IN (?, ?, ?, ?)
                """;
    }

    @Override
    public List<LoanInstallmentReminderPartition> retrieveDuePartitions(final int partitionSize) {

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
        sqlBuilder.append("SUM(installmentCount) AS installmentCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append("FLOOR((ROW_NUMBER() OVER (ORDER BY accountKey) - 1) / ?) AS page, ");
        sqlBuilder.append("accountKey, ");
        sqlBuilder.append("installmentCount ");
        sqlBuilder.append("FROM ( ");

        sqlBuilder.append("SELECT ");
        sqlBuilder.append(SOURCE_ACCOUNT_KEY).append(" AS accountKey, ");
        sqlBuilder.append("COUNT(*) AS installmentCount ");
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
                (rs, rowNum) -> new LoanInstallmentReminderPartition(
                        rs.getLong("minKey"),
                        rs.getLong("maxKey"),
                        rs.getLong("page"),
                        rs.getLong("installmentCount")),
                params.toArray());
    }

    @Override
    public List<LoanInstallmentReminderData> retrieveDuePage(final Long minAccountKey, final Long maxAccountKey, final LocalDate afterDueDate, final Long afterId, final int limit) {

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

    protected static class LoanInstallmentReminderMapper implements RowMapper<LoanInstallmentReminderData> {

        public String schema() {

            return """
            rs.id AS installment_id,
            rs.loan_id,
            l.client_id,
            rs.installment,
            rs.duedate,
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
                    rs.getString("mobile_no"),
                    rs.getString("client_name")
            );
        }
    }
}
