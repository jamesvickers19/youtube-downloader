(ns youtube-downloader.download-youtube
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import (java.net URL URLConnection HttpURLConnection URLDecoder)
           (java.io OutputStreamWriter)
           (java.nio.charset StandardCharsets)))


(defn parse-player-config
  [body]
  (let [playerConfigStr (last (re-find #"ytInitialPlayerResponse\s?=\s?(\{.*\})" body))]
    (json/read-json playerConfigStr)))

(defn parse-url
  [{:keys [signatureCipher]}]
  (let [sc (last (re-find #"url=(.*)" signatureCipher))]
    (.replace sc "\\u0026", "&"))) ; TODO don't know if this replace is needed

(defn adaptive-formats
  [player-config]
  (get-in player-config [:streamingData :adaptiveFormats]))

(defn adaptive-formats-with-mime-type
  [mime-prefix player-config]
  (filter #(s/starts-with? (:mimeType %) mime-prefix) (adaptive-formats player-config)))

(defn highest-quality-audio
  [player-config]
  (apply max-key :bitrate (adaptive-formats-with-mime-type "audio/mp4" player-config)))


; this can work (but may expire), my audio-url doesn't
; https://r2---sn-qxoedn7k.googlevideo.com/videoplayback?expire=1617782210&ei=YhFtYNfNNM6a2_gPwPKH2Ac&ip=73.26.209.80&id=o-AKbGM5_oxXOABZJbRQTYf2SDwwuCYiUOS1yBosBC5qd4&itag=18&source=youtube&requiressl=yes&mh=VD&mm=31%2C26&mn=sn-qxoedn7k%2Csn-q4flrnle&ms=au%2Conr&mv=m&mvi=2&pl=16&initcwndbps=1845000&vprv=1&mime=video%2Fmp4&ns=Ovb31hEuQXWSG34rU9u-dj4F&gir=yes&clen=790527&ratebypass=yes&dur=18.993&lmt=1524502661914999&mt=1617759657&fvip=2&fexp=24001373%2C24007246&c=WEB&n=webmio27xn7D1tJG&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cns%2Cgir%2Cclen%2Cratebypass%2Cdur%2Clmt&sig=AOq0QJ8wRQIhAJ8XZSwMZglCVNTE0pD9Ag1c7ctATjzLvqNgDIg8SH2qAiBDyFfpoql-4LZkMhzPM4ZzNwZAolAWJ5W1Q-P0MsME7Q%3D%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRgIhAIFXVhvRf2FCffNXEY9oKvlsFS9PoOsmX3-sQU0CB8YlAiEAhUSvO_WND5p6wchbFkpE0sEb4-xyhaVL0XL-PBJuTE4%3D

; need to look at DefaultParser.parseFormat, does something to URL
; write a test case that does the example transformation

; example:
(def ^String s "s=_Iow2DytF4DFLxWHpDIIVcwbkjmJkeo3Ce8rz08kB3m9AE%3DAAJ3hn_-tih2p34tE3eEP4bAFBzLrX_CDNATCofkJYTOAhIgRw8JQ0qOANOAi&sp=sig&url=https://r1---sn-qxoedn7z.googlevideo.com/videoplayback%3Fexpire%3D1617862561%26ei%3DQUtuYMXRMoi-igThp7fADg%26ip%3D73.26.209.80%26id%3Do-AKsdDkANQmLUf6k6t6U8sEoAqQ71Dhcf87bYmuBm_lbg%26itag%3D18%26source%3Dyoutube%26requiressl%3Dyes%26mh%3DVz%26mm%3D31%252C26%26mn%3Dsn-qxoedn7z%252Csn-q4fl6n7e%26ms%3Dau%252Conr%26mv%3Dm%26mvi%3D1%26pl%3D16%26initcwndbps%3D2177500%26vprv%3D1%26mime%3Dvideo%252Fmp4%26ns%3DP6hGsQ6LkCFQjsdGCtSdg_EF%26gir%3Dyes%26clen%3D666864181%26ratebypass%3Dyes%26dur%3D7226.583%26lmt%3D1579262686108827%26mt%3D1617840316%26fvip%3D1%26fexp%3D24001373%252C24007246%26beids%3D23886205%26c%3DWEB%26txp%3D5431432%26n%3D-wSqb9ZnXAW68gIP%26sparams%3Dexpire%252Cei%252Cip%252Cid%252Citag%252Csource%252Crequiressl%252Cvprv%252Cmime%252Cns%252Cgir%252Cclen%252Cratebypass%252Cdur%252Clmt%26lsparams%3Dmh%252Cmm%252Cmn%252Cms%252Cmv%252Cmvi%252Cpl%252Cinitcwndbps%26lsig%3DAG3C_xAwRgIhANEftTdaXUnGcZhUSiTLhUTW0MslKJNnt6SRSzYpor0YAiEAj3Y5yPgIDyIgQzxl5kMJ29vwk_KiozrZqPvr1w6lq7g%253D")
; signatureCipher: s=_Iow2DytF4DFLxWHpDIIVcwbkjmJkeo3Ce8rz08kB3m9AE%3DAAJ3hn_-tih2p34tE3eEP4bAFBzLrX_CDNATCofkJYTOAhIgRw8JQ0qOANOAi&sp=sig&url=https://r1---sn-qxoedn7z.googlevideo.com/videoplayback%3Fexpire%3D1617862561%26ei%3DQUtuYMXRMoi-igThp7fADg%26ip%3D73.26.209.80%26id%3Do-AKsdDkANQmLUf6k6t6U8sEoAqQ71Dhcf87bYmuBm_lbg%26itag%3D18%26source%3Dyoutube%26requiressl%3Dyes%26mh%3DVz%26mm%3D31%252C26%26mn%3Dsn-qxoedn7z%252Csn-q4fl6n7e%26ms%3Dau%252Conr%26mv%3Dm%26mvi%3D1%26pl%3D16%26initcwndbps%3D2177500%26vprv%3D1%26mime%3Dvideo%252Fmp4%26ns%3DP6hGsQ6LkCFQjsdGCtSdg_EF%26gir%3Dyes%26clen%3D666864181%26ratebypass%3Dyes%26dur%3D7226.583%26lmt%3D1579262686108827%26mt%3D1617840316%26fvip%3D1%26fexp%3D24001373%252C24007246%26beids%3D23886205%26c%3DWEB%26txp%3D5431432%26n%3D-wSqb9ZnXAW68gIP%26sparams%3Dexpire%252Cei%252Cip%252Cid%252Citag%252Csource%252Crequiressl%252Cvprv%252Cmime%252Cns%252Cgir%252Cclen%252Cratebypass%252Cdur%252Clmt%26lsparams%3Dmh%252Cmm%252Cmn%252Cms%252Cmv%252Cmvi%252Cpl%252Cinitcwndbps%26lsig%3DAG3C_xAwRgIhANEftTdaXUnGcZhUSiTLhUTW0MslKJNnt6SRSzYpor0YAiEAj3Y5yPgIDyIgQzxl5kMJ29vwk_KiozrZqPvr1w6lq7g%253D

(-> s
    (URLDecoder/decode "UTF-8"))

; => url:  https://r1---sn-qxoedn7z.googlevideo.com/videoplayback?expire=1617862561&ei=QUtuYMXRMoi-igThp7fADg&ip=73.26.209.80&id=o-AKsdDkANQmLUf6k6t6U8sEoAqQ71Dhcf87bYmuBm_lbg&itag=18&source=youtube&requiressl=yes&mh=Vz&mm=31%2C26&mn=sn-qxoedn7z%2Csn-q4fl6n7e&ms=au%2Conr&mv=m&mvi=1&pl=16&initcwndbps=2177500&vprv=1&mime=video%2Fmp4&ns=P6hGsQ6LkCFQjsdGCtSdg_EF&gir=yes&clen=666864181&ratebypass=yes&dur=7226.583&lmt=1579262686108827&mt=1617840316&fvip=1&fexp=24001373%2C24007246&beids=23886205&c=WEB&txp=5431432&n=-wSqb9ZnXAW68gIP&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cvprv%2Cmime%2Cns%2Cgir%2Cclen%2Cratebypass%2Cdur%2Clmt&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRgIhANEftTdaXUnGcZhUSiTLhUTW0MslKJNnt6SRSzYpor0YAiEAj3Y5yPgIDyIgQzxl5kMJ29vwk_KiozrZqPvr1w6lq7g%3D&sig=AOq0QJ8wRgIhAOTYJkfoCTNiDC_XrLzBFAb4PEe3Et43p2h_t-_nh3JAAiEA9m3Bk80zr8eC3oekJmjkbwcVIIDpHWxLFD4FtyD2woI=
;
; use URLDecoder.decode on url and 's' from signature ?

(let [url ^URL (URL. audio-url)
      conn ^URLConnection (.openConnection url)]
  (.getContentLength conn))
    ;(spit "C:\\Users\\james\\Downloads\\foo.mp4" (.getInputStream conn))))

; try this instead of openConnection thing
;(client/get audio-url {:as :byte-array})

(comment
  (def audio-url
    (let [r (client/get "https://www.youtube.com/watch?v=HjxZYiTpU3k")
          body (:body r)
          config (parse-player-config body)
          audio (highest-quality-audio config)]
      (parse-url audio))))
