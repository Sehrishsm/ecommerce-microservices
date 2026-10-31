# E-Commerce Microservices — starter structure

This repository is a Maven multi-module monorepo. It contains nine independently runnable Spring Boot applications. No business logic, databases, messaging, or service-to-service routes are implemented yet.

## Prerequisites
- JDK 21
- Maven 3.9+ (or generate and commit the Maven Wrapper with `mvn wrapper:wrapper`)

## Build
Run from the repository root:

```bash
mvn validate
mvn clean package
```

## Run one application
Run each command from the repository root in its own terminal:

```bash
mvn -pl services/api-gateway spring-boot:run
mvn -pl services/auth-service spring-boot:run
mvn -pl services/user-service spring-boot:run
mvn -pl services/product-service spring-boot:run
mvn -pl services/order-service spring-boot:run
mvn -pl services/payment-service spring-boot:run
mvn -pl services/notification-service spring-boot:run
mvn -pl infrastructure/service-discovery spring-boot:run
mvn -pl infrastructure/config-server spring-boot:run
```

Health endpoints (once each service is running):
- Gateway: http://localhost:8080/actuator/health
- Auth: http://localhost:8081/actuator/health
- User: http://localhost:8082/actuator/health
- Product: http://localhost:8083/actuator/health
- Order: http://localhost:8084/actuator/health
- Payment: http://localhost:8085/actuator/health
- Notification: http://localhost:8086/actuator/health
- Eureka dashboard: http://localhost:8761/
- Config Server health: http://localhost:8888/actuator/health

The Config Server native repository points to `../../config-repo` relative to its module directory. Run Maven commands from the root as shown. Create `config-repo/` before starting it.

## Version note
The starter baseline uses Spring Boot 3.5.6 and Spring Cloud 2025.0.0, a compatible pairing. Before a production project, update both to the latest supported patch versions in the same compatible release trains.
