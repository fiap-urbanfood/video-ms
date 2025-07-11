FROM eclipse-temurin:17-jre-alpine

COPY devops/timezone/Sao_Paulo /etc/localtime
RUN echo "America/Sao_Paulo" > /etc/timezone

WORKDIR /app

COPY target/video-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

#Parametros Java
# ENTRYPOINT ["java", "-jar", "app.jar"]
CMD java ${JAVA_OPTS} -jar app.jar
