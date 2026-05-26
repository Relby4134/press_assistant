FROM node:20 AS addin-build
WORKDIR /addin
COPY pres-assistant-addin/package*.json ./
RUN npm ci
COPY pres-assistant-addin/ .
RUN npm run build

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src src
COPY --from=addin-build /addin/dist src/main/resources/static/addin/
RUN ./gradlew bootJar --no-daemon -x npmInstall -x buildAddin -x copyAddinToResources

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
