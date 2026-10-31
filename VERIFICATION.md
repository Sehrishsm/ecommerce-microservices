# Verification checklist

- [ ] `java -version` reports JDK 21.
- [ ] `mvn -version` reports Maven and Java 21.
- [ ] `mvn validate` succeeds from the repository root.
- [ ] `mvn clean package` succeeds and builds all nine modules.
- [ ] Product service starts on port 8083.
- [ ] `http://localhost:8083/actuator/health` returns `{"status":"UP"}`.
- [ ] Eureka dashboard loads at `http://localhost:8761/`.
- [ ] Config Server health responds at `http://localhost:8888/actuator/health`.
- [ ] Gateway health responds at `http://localhost:8080/actuator/health`.
- [ ] No business logic, shared domain entities, databases, Kafka topics, or gateway routes have been added yet.
