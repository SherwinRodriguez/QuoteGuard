# syntax=docker/dockerfile:1

# ---- Build stage ----
# NOTE: pom.xml's parent is spring-boot-starter-parent:4.0.0-SNAPSHOT,
# pulled from Spring's snapshot repository (see repositories in pom.xml).
# This means every build that invalidates the dependency-resolution layer
# below pulls from a repo whose contents can change without a version bump -
# not reproducible, and a real CI reliability risk. This is a known,
# deliberately-not-fixed issue (see QUOTEGUARD_ARCHITECTURE.md and the
# engineering audit) - changing it requires verifying every API this
# session's code relies on against a GA release, which needs a working
# compiler in the loop, not a blind version bump.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B clean package -DskipTests

# ---- Extract layered jar ----
# Spring Boot's layered jar support (enabled in pom.xml) splits the fat jar
# into dependencies / snapshot-dependencies / spring-boot-loader / application
# layers. Copying them as separate Docker layers below means a code-only
# change only invalidates the (small) "application" layer's cache, not the
# (large) "dependencies" layer's.
FROM eclipse-temurin:21-jre AS layers
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN addgroup --system quoteguard && adduser --system --ingroup quoteguard quoteguard

COPY --from=layers /app/app/dependencies/ ./
COPY --from=layers /app/app/spring-boot-loader/ ./
COPY --from=layers /app/app/snapshot-dependencies/ ./
COPY --from=layers /app/app/application/ ./

# Matches InvoiceController/PDFGenerator's relative path
# "generated/invoices/..." - see docker-compose.yml for the named volume
# mounted here so generated PDFs survive container restarts. This does NOT
# fix the underlying architectural gap (local-disk storage doesn't survive
# horizontal scaling to more than one backend instance) - it only makes the
# single-instance Docker Compose deployment durable across restarts.
RUN mkdir -p /app/generated/invoices && chown -R quoteguard:quoteguard /app

USER quoteguard
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
