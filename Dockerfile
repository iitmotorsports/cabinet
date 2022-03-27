FROM gradle:jdk11-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle shadowJar --no-daemon

FROM openjdk:17-alpine

EXPOSE 80

RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/cabinet-all.jar /app/cabinet-all.jar

ENTRYPOINT ["java","/app/cabinet-all.jar"]