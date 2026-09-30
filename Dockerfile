# Étape 1 : Build du projet avec Maven et JDK 21
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Dépendances d'abord : cette couche reste en cache tant que pom.xml ne change pas
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compilation du projet Spring Boot (les tests tournent dans la CI, pas ici)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Étape 2 : Image d'exécution légère (JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S felana && adduser -S felana -G felana
COPY --from=build /app/target/*.jar app.jar
USER felana

# Tas plafonné : sans ça la JVM se dimensionne sur la RAM du serveur entier.
# Prévoir une limite mémoire de conteneur d'environ 768 Mo ; surchargeable au déploiement.
ENV JAVA_TOOL_OPTIONS="-Xmx384m"
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
