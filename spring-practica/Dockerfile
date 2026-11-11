# Imagen para Render: compila Spring con Gradle y lo corre con Java 17.
# --- Etapa 1: compilar ---
FROM eclipse-temurin:17-jdk AS compilar
WORKDIR /app
COPY . .
# gradlew pudo llegar con finales de linea de Windows: se corrigen y se le da permiso
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

# --- Etapa 2: correr ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilar /app/build/libs/*SNAPSHOT.jar app.jar
# Que Java no use mas memoria de la que da el plan gratis de Render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 10000
CMD ["java", "-jar", "app.jar"]
