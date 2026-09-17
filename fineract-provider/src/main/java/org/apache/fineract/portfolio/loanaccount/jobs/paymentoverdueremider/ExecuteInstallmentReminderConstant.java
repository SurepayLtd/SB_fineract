package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import org.apache.fineract.infrastructure.jobs.service.JobName;

public final class ExecuteInstallmentReminderConstant {

    private ExecuteInstallmentReminderConstant() {
    }

    public static final String JOB_NAME = JobName.EXECUTE_LOAN_INSTALLMENT_PAYMENT_REMINDER.name();

    public static final String PARTITIONER_STEP = "executeLoanInstallmentReminderPartitionerStep";

    public static final String WORKER_STEP = "executeLoanInstallmentReminderWorkerStep";

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
}
