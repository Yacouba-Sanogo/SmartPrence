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

# Variables surchargeables au run.
#
# PORT (et non SERVER_PORT) : une variable d'environnement SERVER_PORT
# l'emporte sur la configuration Spring et figerait le port à 8080, alors
# que les plateformes type Render imposent le leur via PORT.
ENV PORT=8080

# Sur un conteneur de 512 Mo (offre gratuite Render), la JVM ne s'alloue par
# défaut que 25 % de la mémoire — environ 128 Mo de tas, insuffisant pour
# Spring Boot + Hibernate. Le ramasse-miettes série coûte moins cher en
# mémoire et en CPU sur une machine mono-cœur.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

EXPOSE 8080

# Forme « shell » indispensable : JAVA_OPTS doit être développé au démarrage.
# « exec » fait de la JVM le processus n°1 du conteneur : sans lui, le SIGTERM
# envoyé lors d'un redéploiement s'arrêterait au shell et Spring ne fermerait
# jamais proprement ses connexions à la base.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
