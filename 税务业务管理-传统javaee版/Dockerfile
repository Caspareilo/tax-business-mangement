FROM maven:3.9.9-eclipse-temurin-11 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:11-jre-alpine

ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai \
    CATALINA_HOME=/opt/tomcat \
    CATALINA_BASE=/opt/tomcat \
    PATH="/opt/tomcat/bin:${PATH}" \
    CATALINA_OPTS="-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

COPY third_party/apache-tomcat-9.0.123.zip /tmp/tomcat.zip
RUN mkdir -p /opt/tomcat \
    && cd /opt/tomcat \
    && busybox unzip -q /tmp/tomcat.zip \
    && mv apache-tomcat-9.0.123/* . \
    && rmdir apache-tomcat-9.0.123 \
    && rm /tmp/tomcat.zip \
    && chmod +x /opt/tomcat/bin/*.sh \
    && rm -rf webapps/*

# The traditional edition deliberately runs Tomcat 9 with javax.servlet.
COPY --from=build /workspace/target/tax-business-system.war \
     /opt/tomcat/webapps/ROOT.war

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=24 --start-period=30s \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/auth/login.action || exit 1

CMD ["catalina.sh", "run"]
