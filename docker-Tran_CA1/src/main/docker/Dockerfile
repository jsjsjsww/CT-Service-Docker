FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD docker-tranca1-1.0.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]