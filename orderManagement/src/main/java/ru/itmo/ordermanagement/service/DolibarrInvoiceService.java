package ru.itmo.ordermanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.ordermanagement.integration.dolibarr.DolibarrConnection;
import ru.itmo.ordermanagement.integration.dolibarr.DolibarrConnectionFactory;
import ru.itmo.ordermanagement.model.entity.Customer;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.repository.CustomerRepository;

@Service
@RequiredArgsConstructor
public class DolibarrInvoiceService {
    private final DolibarrConnectionFactory connectionFactory;
    private final CustomerRepository customerRepository;


    public DolibarrConnection.InvoiceResult createInvoice(Order order) {
        try (DolibarrConnection connection = connectionFactory.getConnection()) {
            Long thirdpartyId = getThirdpartyId(order.getCustomer(), connection);
            return connection.createInvoice(order, thirdpartyId);
        }
    }

    private Long getThirdpartyId(Customer customer, DolibarrConnection connection) {
        if (customer.getDolibarrThirdpartyId() != null) {
            return customer.getDolibarrThirdpartyId();
        }

        Long thirdpartyId = connection.createThirdparty(customer);
        customer.setDolibarrThirdpartyId(thirdpartyId);
        customerRepository.save(customer);
        return thirdpartyId;
    }
}
