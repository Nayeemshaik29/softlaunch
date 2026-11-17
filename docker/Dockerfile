# One Dockerfile for all 11 services. Build context = the service folder, after ./mvnw package
# (the jar is built on the host, so images don't download the internet a second time).

# --- stage 1: split the fat jar into layers (dependencies change rarely, our code often) ---
FROM eclipse-temurin:25-jre AS layers
WORKDIR /build
COPY target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# --- stage 2: the image we ship ---
FROM eclipse-temurin:25-jre
# never run as root: if the app is ever exploited, the attacker isn't root in the container
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
# least-changing layers first: a code change only rebuilds the small "application" layer (~1 MB)
COPY --from=layers /build/extracted/dependencies/ ./
COPY --from=layers /build/extracted/spring-boot-loader/ ./
COPY --from=layers /build/extracted/snapshot-dependencies/ ./
COPY --from=layers /build/extracted/application/ ./
USER app
# Container-aware memory: heap = 60% of the container limit, the rest is for metaspace, threads, buffers.
# SerialGC: least memory overhead for small heaps. Exit on OOM so Docker restarts a clean process.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "-jar", "app.jar"]
