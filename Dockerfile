FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --gid 10001 vitna && useradd --uid 10001 --gid vitna vitna
COPY --from=build /build/target/vitna-0.0.1-SNAPSHOT.jar /app/vitna.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh && chmod +x /app/docker-entrypoint.sh
ENV SPRING_PROFILES_ACTIVE=prod
USER vitna
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
