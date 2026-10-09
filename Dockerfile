# Etapa 1: Construcción con JDK 21
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar configuración de Gradle para aprovechar caché de capas Docker
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# Permisos de ejecución para el wrapper de Gradle
RUN chmod +x ./gradlew

# Copiar código fuente
COPY src src

# Compilar artefacto JAR ejecutable (omitiendo tests para agilizar el build en Render)
RUN ./gradlew clean bootJar -x test --no-daemon

# Etapa 2: Imagen ligera de ejecución con JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiar el JAR generado desde la etapa de construcción
COPY --from=build /app/build/libs/*.jar app.jar

# Puerto por defecto
ENV PORT=8081
EXPOSE 8081

# Optimización de memoria JVM para el límite de 512MB de Render
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
