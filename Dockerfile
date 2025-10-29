FROM maven:3-eclipse-temurin-25 as build

COPY ./ /app

WORKDIR /app

# Build the application
RUN mvn package -Dmaven.test.skip=true

# Setup smaller Image to run the application
FROM eclipse-temurin:25-jdk as runtime

COPY --from=build /app/target/DuneBot-*-SNAPSHOT.jar /

STOPSIGNAL SIGUSR1

CMD java -jar /DuneBot-*-SNAPSHOT.jar
