#============================================================
# SmartPresence — Backend Dockerfile
# Image multi-stage : build Maven + runtime JDK léger
#============================================================

# ---- Étape 1 : Build de l'application avec Maven Wrapper ----
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Copie sélective pour optimiser le cache Docker (pom d'abord)
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Télécharge les dépendances (cacheable si pom inchangé)
RUN ./mvnw -B dependency:go-offline

# Copie le code source et compile
COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---- Étape 2 : Runtime léger ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copie uniquement le JAR exécutable
COPY --from=builder /workspace/target/*.jar app.jar

# Variables surchargeables au run
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
