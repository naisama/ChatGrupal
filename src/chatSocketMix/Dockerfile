FROM eclipse-temurin:11

ENV JAVA_HOME /opt/jdk

ENV PATH ${PATH}:${JAVA_HOME}/bin

RUN mkdir /opt/app

COPY Server.java /opt/app

CMD ["java", "/opt/app/Server.java"]
