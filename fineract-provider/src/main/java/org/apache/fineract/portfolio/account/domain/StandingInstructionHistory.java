
package org.apache.fineract.portfolio.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "m_account_transfer_standing_instructions_history")
@Getter
@Setter
public class StandingInstructionHistory extends AbstractPersistableCustom<Long> {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";

    @Column(name = "standing_instruction_id", nullable = false)
    private Long standingInstructionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "execution_time", nullable = false)
    private LocalDateTime executionTime;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "error_log", length = 500)
    private String errorLog;

    protected StandingInstructionHistory() {
        //
    }

    private StandingInstructionHistory(Long standingInstructionId, String status, BigDecimal amount, String errorLog) {
        this.standingInstructionId = standingInstructionId;
        this.status = status;
        this.executionTime = DateUtils.getLocalDateTimeOfTenant();
        this.amount = amount;
        this.errorLog = errorLog;
    }

    public static StandingInstructionHistory success(final Long standingInstructionId, final BigDecimal transferredAmount) {
        return new StandingInstructionHistory(standingInstructionId, STATUS_SUCCESS, transferredAmount, null);
    }

    public static StandingInstructionHistory failed(final Long standingInstructionId, final String errorLog) {
        // Nothing moved on a failed run, so the recorded amount is zero (not the attempted amount).
        return new StandingInstructionHistory(standingInstructionId, STATUS_FAILED, BigDecimal.ZERO, trimToColumn(errorLog));
    }

    private static String trimToColumn(final String errorLog) {
        if (errorLog == null) {
            return null;
        }
        return errorLog.length() > 500 ? errorLog.substring(0, 500) : errorLog;
    }
}
