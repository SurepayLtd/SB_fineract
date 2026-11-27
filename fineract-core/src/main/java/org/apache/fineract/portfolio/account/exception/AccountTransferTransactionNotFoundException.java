package org.apache.fineract.portfolio.account.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class AccountTransferTransactionNotFoundException extends AbstractPlatformResourceNotFoundException {


    public AccountTransferTransactionNotFoundException(final Long id) {
        super("error.msg.accounttransfertransaction.id.invalid", "Account transfer transaction with identifier " + id + " does not exist", id);
    }
}
