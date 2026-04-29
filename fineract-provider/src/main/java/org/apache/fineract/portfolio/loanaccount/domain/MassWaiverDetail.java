package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "m_mass_charge_waiver_detail")
@Getter
public class MassWaiverDetail extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "mass_waiver_id", nullable = false)
    private MassWaiver massWaiver;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_charge_id", nullable = false)
    private LoanCharge loanCharge;

    @Column(name = "waived_amount", precision = 19, scale = 6, nullable = false)
    private BigDecimal waivedAmount;

    @Column(name = "is_fully_waived", nullable = false)
    private boolean isFullyWaived;


    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;



    protected MassWaiverDetail(){}

    public MassWaiverDetail(MassWaiver massWaiver, LoanCharge loanCharge, BigDecimal waivedAmount,
                            boolean isFullyWaived, LocalDate dueDate) {
        this.massWaiver = massWaiver;
        this.loanCharge = loanCharge;
        this.waivedAmount = waivedAmount;
        this.isFullyWaived = isFullyWaived;
        this.dueDate = dueDate;
    }

    // ===== Factory =====
    public static MassWaiverDetail create(MassWaiver massWaiver, LoanCharge loanCharge,
                                          BigDecimal waivedAmount, boolean isFullyWaived, LocalDate dueDate
                                              ) {
        return new MassWaiverDetail(massWaiver, loanCharge, waivedAmount, isFullyWaived, dueDate);
    }

    public void setMassWaiver(MassWaiver massWaiver) {
        this.massWaiver = massWaiver;
    }
}
