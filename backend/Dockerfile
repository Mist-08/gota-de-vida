# Etapa 1: compilar con el Gradle Wrapper del proyecto.
# installDist deja la app en build/install/gota-de-vida-backend (bin/ + lib/).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN chmod +x gradlew && ./gradlew installDist --no-daemon

# Etapa 2: imagen ligera solo con el JRE y la app.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/build/install/gota-de-vida-backend/ ./
ENV PORT=8080
EXPOSE 8080
CMD ["./bin/gota-de-vida-backend"]
