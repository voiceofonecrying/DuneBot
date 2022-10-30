FROM eclipse-temurin:18-jdk-focal as build

# Installing Maven
ENV MAVEN_VERSION=3.8.6
RUN wget https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
RUN tar zxvf apache-maven-${MAVEN_VERSION}-bin.tar.gz
RUN ln -s /apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

COPY ./ /app

WORKDIR /app

# Build the application
RUN mvn package

# Setup smaller Image to run the application
FROM eclipse-temurin:18-jre-alpine as runtime

COPY --from=build /app/target/DuneBot-*-SNAPSHOT.jar /

COPY scripts/env-to-dotenv.sh /

CMD /env-to-dotenv.sh && java -jar /DuneBot-*-SNAPSHOT.jar