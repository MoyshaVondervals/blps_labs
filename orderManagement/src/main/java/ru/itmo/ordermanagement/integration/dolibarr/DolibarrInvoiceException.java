package ru.itmo.ordermanagement.integration.dolibarr;

public class DolibarrInvoiceException extends RuntimeException {
    public DolibarrInvoiceException(String message) {
        super(message);
    }

    public DolibarrInvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
