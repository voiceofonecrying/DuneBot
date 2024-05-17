FROM maven:3.9.6-amazoncorretto-21 as build

COPY ./ /app

WORKDIR /app

# Build the application
RUN mvn package -Dmaven.test.skip=true

# Setup smaller Image to run the application
FROM maven:3.9.6-amazoncorretto-21 as runtime

COPY --from=build /app/target/DuneBot-*-SNAPSHOT.jar /

STOPSIGNAL SIGUSR1

CMD java -jar /DuneBot-*-SNAPSHOT.jar
