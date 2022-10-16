FROM node:18 as frontend-build

WORKDIR /usr/src/app
COPY ./frontend/ .
RUN npm install
RUN npm run build

FROM sbtscala/scala-sbt:openjdk-18.0.2.1_1.7.2_3.2.0
WORKDIR /opt/app
COPY ./project ./project
COPY ./src ./src
COPY build.sbt ./
COPY .scalafmt.conf ./

RUN sbt compile

COPY --from=frontend-build /usr/src/app/build ./frontend/build
CMD sbt run
