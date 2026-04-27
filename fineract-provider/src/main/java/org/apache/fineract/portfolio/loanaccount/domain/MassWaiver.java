package org.apache.fineract.portfolio.loanaccount.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "m_mass_charge_waiver")
@Getter
public class MassWaiver extends AbstractPersistableCustom<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "requested_amount", precision = 19, scale = 6)
    private BigDecimal requestedAmount;

    @Column(name = "total_waived", precision = 19, scale = 6)
    private BigDecimal totalWaived;

    @Column(name = "remaining_amount", precision = 19, scale = 6)
    private BigDecimal remainingAmount;

    @Column(name = "charges_affected")
    private Integer chargesAffected;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;


    @OneToMany(mappedBy = "massWaiver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MassWaiverDetail> details = new ArrayList<>();

    @PrePersist
    public void onCreate(){
        this.createdDate = LocalDateTime.now();
    }

    protected MassWaiver(){}


    public MassWaiver(Client client, Loan loan, BigDecimal requestedAmount, BigDecimal totalWaived,
                      BigDecimal remainingAmount, Integer chargesAffected) {
        this.client = client;
        this.loan = loan;
        this.requestedAmount = requestedAmount;
        this.totalWaived = totalWaived;
        this.remainingAmount = remainingAmount;
        this.chargesAffected = chargesAffected;
    }

    // ===== Factory =====
    public static MassWaiver create(Client client, Loan loan, BigDecimal requestedAmount, BigDecimal totalWaived,
                                    BigDecimal remainingAmount, Integer chargesAffected) {
        return new MassWaiver(client, loan, requestedAmount , totalWaived, remainingAmount, chargesAffected);
    }

    public void addDetail(MassWaiverDetail detail) {
        this.details.add(detail);
        detail.setMassWaiver(this);
    }

    public void setChargesAffected(Integer chargesAffected) {
        this.chargesAffected = chargesAffected;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public void setTotalWaived(BigDecimal totalWaived) {
        this.totalWaived = totalWaived;
    }
}