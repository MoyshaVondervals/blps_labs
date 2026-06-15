package ru.itmo.ordermanagement.integration.dolibarr;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serial;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

public class DolibarrManagedConnectionFactory implements ManagedConnectionFactory {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String baseUrl;
    private final String apiKey;
    private final String login;
    private final String password;
    private final BigDecimal defaultVatRate;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final boolean validateOnCreate;
    private PrintWriter logWriter;

    //todo
    public DolibarrManagedConnectionFactory(DolibarrProperties properties) {
        this.baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        this.apiKey = properties.getApiKey();
        this.login = properties.getLogin();
        this.password = properties.getPassword();
        this.defaultVatRate = properties.getDefaultVatRate();
        this.connectTimeout = properties.getConnectTimeout();
        this.readTimeout = properties.getReadTimeout();
        this.validateOnCreate = properties.isValidateOnCreate();
    }

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) {
        return new DolibarrConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() {
        return new DolibarrConnectionFactoryImpl(this, new DolibarrLocalConnectionManager());
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject,
                                                     ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ResourceException("Dolibarr baseUrl is not configured");
        }
        if ((apiKey == null || apiKey.isBlank()) && (login == null || login.isBlank()
                || password == null || password.isBlank())) {
            throw new ResourceException("Dolibarr apiKey or login/password is not configured");
        }
        return new DolibarrManagedConnection(
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
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo connectionRequestInfo) {
        return null;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) {
            return null;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
