FROM openjdk:18-slim-buster

WORKDIR /

COPY YoutubeDownloader.jar YoutubeDownloader.jar

EXPOSE 8080

CMD java -XX:MaxRAMPercentage=75 -jar YoutubeDownloader.jar