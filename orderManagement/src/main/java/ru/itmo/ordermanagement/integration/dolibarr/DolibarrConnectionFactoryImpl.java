package ru.itmo.ordermanagement.integration.dolibarr;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ManagedConnectionFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DolibarrConnectionFactoryImpl implements DolibarrConnectionFactory {
    private final ManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;


    @Override
    public DolibarrConnection getConnection() {
        try {
            return (DolibarrConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
        } catch (ResourceException e) {
            String detail = e.getMessage() == null || e.getMessage().isBlank()
                    ? "unknown ResourceException"
                    : e.getMessage();
            throw new DolibarrInvoiceException("Cannot allocate Dolibarr JCA connection: " + detail, e);
        }
    }
}
