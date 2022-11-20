FROM node:18 as frontend-build

WORKDIR /usr/src/app
COPY ./frontend/package.json ./frontend/package-lock.json  ./
RUN npm install
COPY ./frontend/ ./
RUN npm run build

FROM sbtscala/scala-sbt:openjdk-18.0.2.1_1.7.2_3.2.0 as backend-build
WORKDIR /opt/app
COPY ./project ./project
COPY ./src ./src
COPY build.sbt ./
COPY .scalafmt.conf ./
RUN sbt stage

FROM eclipse-temurin:19.0.1_10-jre-ubi9-minimal

WORKDIR /opt/app
COPY --from=backend-build /opt/app/target/universal/stage ./
COPY --from=frontend-build /usr/src/app/build ./frontend/build

EXPOSE 8080
CMD ./bin/orangutan-game
