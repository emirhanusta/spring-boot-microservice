# Reusable runtime image for every Readscape service.
#   (context = a dir holding app.jar and this file; make apps-build prepares services/<name>/build/docker)
# `make apps-build` runs bootJar and copies each boot jar to services/<name>/build/docker/app.jar first.
# The base image ships neither curl nor wget; compose healthchecks use bash's /dev/tcp instead.
FROM eclipse-temurin:25-jre-noble@sha256:f3dd0838e69b881cc382237d5901e74ae3705b1056ad964b2e0e523b174fef99

ARG JAR=app.jar
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --chown=app:app ${JAR} /app/app.jar
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
