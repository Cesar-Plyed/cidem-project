# =====================================================================
# integrity-core — Backend Spring Boot (monolito modular)
# Multi-stage build: compila con Maven+JDK, corre solo con el JRE
# =====================================================================

# ---------- Etapa 1: build ----------
FROM docker.io/library/eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copiamos primero solo lo necesario para resolver dependencias
# (aprovecha la cache de Docker/Podman si el pom.xml no cambia)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Ahora sí el código fuente
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ---------- Etapa 2: runtime ----------
FROM docker.io/library/eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Usuario no-root para correr la app
RUN groupadd -r spring && useradd -r -g spring spring
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]