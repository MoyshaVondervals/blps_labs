package ru.itmo.ordermanagement.integration.dolibarr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.itmo.ordermanagement.model.entity.Customer;
import ru.itmo.ordermanagement.model.entity.Order;
import ru.itmo.ordermanagement.model.entity.OrderItem;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class DolibarrConnectionImpl implements DolibarrConnection {
    private final String baseUrl;
    private final String apiKey;
    private final BigDecimal defaultVatRate;
    private final Duration readTimeout;
    private final boolean validateOnCreate;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private boolean isClosed;

    public DolibarrConnectionImpl(String baseUrl,
                                  String apiKey,
                                  BigDecimal defaultVatRate,
                                  Duration connectTimeout,
                                  Duration readTimeout,
                                  boolean validateOnCreate) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.defaultVatRate = defaultVatRate;
        this.readTimeout = readTimeout;
        this.validateOnCreate = validateOnCreate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
    }

    @Override
    public Long createThirdparty(Customer customer) {
        if (isClosed) {
            throw new DolibarrInvoiceException("Dolibarr connection is closed");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", customer.getName());
            body.put("client", 1); // тут единичка потому что клиент
            body.put("email", customer.getEmail());
            body.put("phone", customer.getPhone());

            HttpResponse<String> response = sendJson("POST", "/thirdparties", body);
            ensureSuccess(response, "create thirdparty");
            Long thirdpartyId = extractId(response.body());
            if (thirdpartyId == null) {
                throw new DolibarrInvoiceException("Dolibarr did not return created thirdparty id");
            }
            return thirdpartyId;
        } catch (Exception e) {
            throw new DolibarrInvoiceException("Failed to create Dolibarr thirdparty", e);
        }
    }

    @Override
    public InvoiceResult createInvoice(Order order, Long thirdpartyId) {
        if (isClosed) {
            throw new DolibarrInvoiceException("Dolibarr connection is closed");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new DolibarrInvoiceException("Cannot create Dolibarr invoice without lines");
        }

        try {
            Long invoiceId = createInvoiceHeader(order, thirdpartyId);
            for (OrderItem line : order.getItems()) {
                addInvoiceLine(invoiceId, line);
            }
            if (validateOnCreate) {
                validateInvoice(invoiceId);
            }
            String ref = fetchInvoiceRef(invoiceId);
            return new InvoiceResult(invoiceId, ref);
        } catch (DolibarrInvoiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DolibarrInvoiceException("Failed to create Dolibarr invoice", e);
        }
    }

    @Override
    public void close() {
        isClosed = true;
    }


    private Long createInvoiceHeader(Order order, Long thirdpartyId) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("socid", thirdpartyId);
        body.put("note_private", "Order #" + order.getId());

        HttpResponse<String> response = sendJson("POST", "/invoices", body);
        ensureSuccess(response, "create invoice");
        Long invoiceId = extractId(response.body());
        if (invoiceId == null) {
            throw new DolibarrInvoiceException("Dolibarr did not return created invoice id");
        }
        return invoiceId;
    }

    private void addInvoiceLine(Long invoiceId, OrderItem line) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("desc", line.getProductName());
        body.put("qty", line.getQuantity());
        body.put("subprice", line.getPrice());
        body.put("tva_tx", defaultVatRate);
        body.put("localtax1_tx", BigDecimal.ZERO);
        body.put("localtax2_tx", BigDecimal.ZERO);
        body.put("remise_percent", BigDecimal.ZERO);
        body.put("price_base_type", "HT");
        body.put("info_bits", 0);
        body.put("product_type", 0);
        body.put("rang", -1);
        body.put("special_code", 0);
        body.put("array_options", Map.of());
        body.put("situation_percent", 100);

        HttpResponse<String> response = sendJson("POST", "/invoices/" + invoiceId + "/lines", body);
        ensureSuccess(response, "add invoice line");
    }

    private void validateInvoice(Long invoiceId) throws IOException, InterruptedException {
        HttpResponse<String> response = sendJson("POST", "/invoices/" + invoiceId + "/validate", Map.of());
        ensureSuccess(response, "validate invoice");
    }

    private String fetchInvoiceRef(Long invoiceId) throws IOException, InterruptedException {
        HttpResponse<String> response = send("GET", "/invoices/" + invoiceId, null);
        ensureSuccess(response, "fetch invoice");
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode ref = root.path("ref");
        return ref.isMissingNode() || ref.isNull() ? null : ref.asText();
    }

    private Long extractId(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root.isNumber()) {
            return root.longValue();
        }
        for (String field : new String[]{"id", "rowid", "invoice_id"}) {
            JsonNode value = root.path(field);
            if (value.isNumber()) {
                return value.longValue();
            }
            if (value.isTextual() && value.asText().matches("\\d+")) {
                return Long.parseLong(value.asText());
            }
        }
        return null;
    }

    private HttpResponse<String> sendJson(String method, String path, Object body) throws IOException, InterruptedException {
        return send(method, path, objectMapper.writeValueAsString(body));
    }

    private HttpResponse<String> send(String method, String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl(path)))
                .timeout(readTimeout)
                .header("DOLAPIKEY", apiKey)
                .header("Accept", "application/json");

        if (jsonBody == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String apiUrl(String path) {
        String encodedPath = path.startsWith("/") ? path : "/" + path;
        return baseUrl + "/api/index.php" + encodeSpaces(encodedPath);
    }

    private String encodeSpaces(String path) {
        return path.replace(" ", URLEncoder.encode(" ", StandardCharsets.UTF_8));
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new DolibarrInvoiceException(
                    "Dolibarr failed to " + operation + ": HTTP " + response.statusCode() + " " + response.body());
        }
    }


}
