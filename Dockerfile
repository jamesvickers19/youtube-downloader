FROM clojure:openjdk-18-tools-deps-slim-buster

WORKDIR /

RUN ["apt", "-y", "update"]
RUN ["apt", "-y", "install", "ffmpeg"]

COPY server/YoutubeDownloader.jar YoutubeDownloader.jar

EXPOSE 8080

CMD java -Xmx512m -jar YoutubeDownloader.jar