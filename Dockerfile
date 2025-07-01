FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY package-command/pom.xml package-command/
COPY package-query/pom.xml package-query/

RUN mvn dependency:go-offline -B

COPY package-command/src package-command/src
COPY package-query/src package-query/src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/package-command/target/package-command-1.0.0.jar package-command/target/
COPY --from=build /app/package-query/target/package-query-1.0.0.jar package-query/target/

EXPOSE 8080

CMD ["java", "-jar", "package-command/target/package-command-1.0.0.jar"] 