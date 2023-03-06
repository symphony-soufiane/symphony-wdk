FROM amazoncorretto:17
COPY artifact/*.jar wdk.jar
ENTRYPOINT ["java", "-Dspring.config.additional-location=./${VOLUME:symphony}/",  "-jar", "wdk.jar", "--spring.profiles.active=${PROFILE:default}"]

