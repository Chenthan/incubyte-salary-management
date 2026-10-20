# syntax=docker/dockerfile:1.7
FROM node:24.19.0-bookworm-slim@sha256:a9f5f7c91a432850b2a8a7797adf5eadb6c733ceed61167806cee7ea7fbc29df AS frontend
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm --mount=type=secret,id=proxy_ca \
    set -eu; if [ -f /run/secrets/proxy_ca ]; then export NODE_EXTRA_CA_CERTS=/run/secrets/proxy_ca; fi; npm ci --no-fund
COPY frontend/ ./
RUN npm run build

FROM frontend AS frontend-check
CMD ["sh", "-c", "npm run format:check && npm test && npx tsc -p tsconfig.e2e.json"]

FROM maven:3.9.16-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS backend-source
WORKDIR /work
COPY backend/ ./
COPY scripts/maven-network.sh /scripts/maven-network.sh

FROM backend-source AS backend-tests
CMD ["sh", "/scripts/maven-network.sh", "spotless:check", "verify", "-Pintegration"]

FROM backend-source AS backend-build
COPY --from=frontend /web/dist/frontend/browser/ ./src/main/resources/static/
RUN --mount=type=cache,target=/root/.m2 --mount=type=secret,id=proxy_ca \
    sh /scripts/maven-network.sh spotless:check package

FROM eclipse-temurin:21-jre@sha256:227118764cccd3c3294617061aaa93a08309c63a395eefa83e6b35032992ea99 AS runtime
WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /work/target/salary-management-1.0.0.jar /app/application.jar
COPY --chmod=755 scripts/healthcheck.sh /app/healthcheck.sh
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=6 CMD ["/app/healthcheck.sh"]
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
