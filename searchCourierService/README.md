# searchCourierService

Kafka consumer service that assigns an available courier to an order in `SEARCHING_COURIER` status and publishes a notification event.

## Run locally

```bash
mvn -q -DskipTests=false test
mvn -q -DskipTests=true spring-boot:run
```

## Docker

Build and run via the root `docker-compose.yml`.

