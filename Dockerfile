# Multi-stage build for Spring Boot app using Maven + Temurin 23
FROM maven:3.9.9-eclipse-temurin-23 AS build
WORKDIR /app

# Copy pom first to cache dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source and build
COPY src ./src
# Optional debug: uncomment to print Java/Maven versions inside the build container
# RUN java -version && javac -version && mvn -v
RUN mvn -B clean package -DskipTests

# Runtime stage (Temurin 23 JRE)
FROM eclipse-temurin:23-jre
WORKDIR /app

# Create non-root user (portable for Debian-based image)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
