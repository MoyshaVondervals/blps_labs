package ru.itmo.ordermanagement.integration.dolibarr;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.LocalTransaction;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionMetaData;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Duration;

public class DolibarrManagedConnection implements ManagedConnection {
    private final String baseUrl;
    private final String apiKey;
    private final String login;
    private final String password;
    private final BigDecimal defaultVatRate;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final boolean validateOnCreate;
    private PrintWriter logWriter;

    public DolibarrManagedConnection(String baseUrl,
                                     String apiKey,
                                     String login,
                                     String password,
                                     BigDecimal defaultVatRate,
                                     Duration connectTimeout,
                                     Duration readTimeout,
                                     boolean validateOnCreate) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.login = login;
        this.password = password;
        this.defaultVatRate = defaultVatRate;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.validateOnCreate = validateOnCreate;
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        return new DolibarrConnectionImpl(
                baseUrl,
                apiKey,
                login,
                password,
                defaultVatRate,
                connectTimeout,
                readTimeout,
                validateOnCreate
        );
    }

    @Override
    public void destroy() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (!(connection instanceof DolibarrConnection)) {
            throw new ResourceException("Unsupported Dolibarr connection handle");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        throw new ResourceException("Dolibarr JCA adapter does not support XA transactions");
    }

    @Override
    public LocalTransaction getLocalTransaction() {
        return new LocalTransaction() {
            @Override
            public void begin() {
            }

            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
            }
        };
    }

    @Override
    public ManagedConnectionMetaData getMetaData() {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() {
                return "Dolibarr ERP/CRM";
            }

            @Override
            public String getEISProductVersion() {
                return "REST API";
            }

            @Override
            public int getMaxConnections() {
                return 0;
            }

            @Override
            public String getUserName() {
                return "DOLAPIKEY";
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }
}
