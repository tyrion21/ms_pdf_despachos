# Multi-stage build para optimizar el tamaño de la imagen final

# Etapa 1: Build del backend (Spring Boot)
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Build del frontend (Next.js)
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --only=production
COPY frontend/ .
RUN npm run build

# Etapa 3: Imagen final de producción
FROM eclipse-temurin:17-jre
WORKDIR /app

# Instalar herramientas necesarias
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root para seguridad
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copiar el JAR del backend
COPY --from=backend-builder /app/target/*.jar app.jar

# Copiar archivos estáticos del frontend
COPY --from=frontend-builder /app/frontend/.next /app/static/frontend
COPY --from=frontend-builder /app/frontend/public /app/static/public

# Crear directorios necesarios
RUN mkdir -p /app/generated-pdf /app/logs
RUN chown -R appuser:appuser /app

# Cambiar a usuario no-root
USER appuser

# Configurar variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SERVER_PORT=8090

# Exponer puerto
EXPOSE 8090

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8090/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]