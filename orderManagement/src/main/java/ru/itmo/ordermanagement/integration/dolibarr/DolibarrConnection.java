package ru.itmo.ordermanagement.integration.dolibarr;

import ru.itmo.ordermanagement.model.entity.Customer;
import ru.itmo.ordermanagement.model.entity.Order;

public interface DolibarrConnection extends AutoCloseable {
    Long createThirdparty(Customer customer);

    InvoiceResult createInvoice(Order order, Long thirdpartyId);

    @Override
    void close();

    record InvoiceResult(Long invoiceId, String invoiceRef) {
    }
}
