package org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider;

import org.apache.fineract.infrastructure.jobs.service.JobName;

public final class ExecuteBatchJobConstant {

    private ExecuteBatchJobConstant() {
    }

    public static final String LOAN_INSTALLMENT_JOB_NAME = JobName.EXECUTE_LOAN_INSTALLMENT_PAYMENT_REMINDER.name();

    public static final String LOAN_INSTALLMENT_PARTITIONER_STEP = "executeLoanInstallmentReminderPartitionerStep";

    public static final String LOAN_INSTALLMENT_WORKER_STEP = "executeLoanInstallmentReminderWorkerStep";

    public static final String LOAN_INSTALLMENT_OVERDUE_JOB_NAME = JobName.EXECUTE_LOAN_INSTALLMENT_OVERDUE_REMINDER.name();

    public static final String LOAN_INSTALLMENT_OVERDUE_WORKER_STEP = "executeLoanInstallmentOverdueReminderWorkerStep";

    public static final String LOAN_INSTALLMENT_OVERDUE_PARTITIONER_STEP = "executeLoanInstallmentOverdueReminderPartitionerStep";

    public static final String SAVINGS_DORMANCY_JOB_NAME = JobName.EXECUTE_DORMANT_ACCOUNTS_SMS.name();

    public static final String SAVINGS_DORMANCY_PARTITIONER_STEP = "executeDormantSavingsAccountReminderPartitionerStep";

    public static final String SAVINGS_DORMANCY_WORKER_STEP = "executeDormantSavingsAccountReminderWorkerStep";

    public static final String PARTITION_PREFIX = "partition_";

    public static final String MIN_ACCOUNT_KEY = "minAccountKey";

    public static final String MAX_ACCOUNT_KEY = "maxAccountKey";

    public static final String PARTITION_KEY = "partition";

    public static final String TODAY = "today";

    public static final String PAGE_SIZE = "pageSize";

    public static final int T_7 = 7;
    public static final int T_3 = 3;
    public static final int T_1 = 1;
    public static final int T_0 = 0;

    public static final int D1 = 1;
    public static final int D7 = 7;
    public static final int D30 = 30;
    public static final int D60 = 60;
    public static final int D90 = 90;
}
