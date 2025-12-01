# Multi-stage build for Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies (this layer will be cached if pom.xml doesn't change)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR file from build stage
COPY --from=build /app/target/invoicegeneratorapi-*.jar app.jar

# Expose the application port
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

