FROM maven:3.9.2-amazoncorretto-20 as build

# Installing Maven
#ENV MAVEN_VERSION=3.9.1
#RUN wget https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
#RUN tar zxvf apache-maven-${MAVEN_VERSION}-bin.tar.gz
#RUN ln -s /apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

COPY ./ /app

WORKDIR /app

# Build the application
RUN mvn package -Dmaven.test.skip=true

# Setup smaller Image to run the application
FROM maven:3.9.2-amazoncorretto-20 as runtime

COPY --from=build /app/target/DuneBot-*-SNAPSHOT.jar /

COPY scripts/env-to-dotenv.sh /

CMD sh /env-to-dotenv.sh && java -jar /DuneBot-*-SNAPSHOT.jar
