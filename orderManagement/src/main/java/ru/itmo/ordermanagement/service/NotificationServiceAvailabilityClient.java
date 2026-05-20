package ru.itmo.ordermanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
public class NotificationServiceAvailabilityClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${app.notification-service.base-url}")
    private String notificationServiceBaseUrl;

    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(notificationServiceBaseUrl + "/api/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 300;
        } catch (Exception e) {
            log.warn("Notification service is unavailable", e);
            return false;
        }
    }
}
