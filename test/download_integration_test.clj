(ns download_integration_test
  (:require [clojure.test :refer :all]
            [youtube_downloader.download :refer :all]))

(def lofi-cozy-winter-sections
  [{:start 0, :name "Team Astro - Over The Moon", :end 173}
   {:start 173, :name "Hoogway - After You", :end 312}
   {:start 312, :name "Purrple Cat - Moonlit Walk", :end 522}
   {:start 522, :name "Enluv x E I S U - Fjallstoppur", :end 648}
   {:start 648, :name "squeeda - Vulnerable", :end 824}
   {:start 824, :name "Towerz x farewell - Sparkler", :end 982}
   {:start 982, :name "Jhove - Night Lamp", :end 1112}
   {:start 1112, :name "cxlt. - Overthinking ", :end 1264}
   {:start 1264, :name "Elior - Soaring", :end 1424}
   {:start 1424, :name "xander. - Rain Come Again", :end 1607}
   {:start 1607, :name "G Mills x aimless - Drifting", :end 1735}
   {:start 1735, :name "WYS - San Fransisco", :end 1876}
   {:start 1876, :name "Lofty x Pointy Features x Quist - Loves Dissonance", :end 2036}
   {:start 2036, :name "Monma x cocabona - Tetra", :end 2227}
   {:start 2227, :name "aimless x soho - Every Second", :end 2392}
   {:start 2392, :name "Glimlip - Ebs and Flows", :end 2491}
   {:start 2491, :name "TABAL x eaup - Days Will Pass", :end 2611}
   {:start 2611, :name "Ambulo - Serene", :end 2739}
   {:start 2739, :name "Sleepermane x Sling Dilly - Inside Out", :end 2863}
   {:start 2863, :name "Otaam x squeeda - Dreaming of Snow", :end 2979}
   {:start 2979, :name "eaup x Elior - Floating", :end 3124}
   {:start 3124, :name "Bert x Nerok - Campfire", :end 3256}
   {:start 3256, :name "azula x iamalex x Dillan Witherow - Hammock", :end 3386}
   {:start 3386, :name "Anbuu x Blue Wednesday - Sixth Station", :end 3548}
   {:start 3548, :name "tysu x Spencer Hunt - Heated Blanket", :end 3692}
   {:start 3692, :name "Kainbeats x S N U G - Formless", :end 3842}
   {:start 3842, :name "Chiccote’s Beats x Pueblo Vista - Counting Stars", :end 3967}
   {:start 3967, :name "Towerz x Hoogway - Moonfall", :end 4118}
   {:start 4118, :name "fourwalls - Waves", :end 4241}
   {:start 4241, :name "Celestial Alignment - A Roomful of Memories and Longing", :end 4371}
   {:start 4371, :name "Mondo Loops - Always Drifting", :end 4530}
   {:start 4530, :name "Laffey - As The Sun Sets", :end 4680}])

(def trappin-in-paradise-50-sections
  [{:start 0, :name "GREEN PICCOLO - Time Capsule", :end 197}
   {:start 197, :name "Goupil. - JUST BEGUN", :end 324}
   {:start 324, :name "superior. - WE HAD A DEAL", :end 499}
   {:start 499, :name "KVN$ - let’s play 💔", :end 650}
   {:start 650, :name "aleks - babywipe", :end 761}
   {:start 761, :name "Goupil. - MOON", :end 895}
   {:start 895, :name "KVN$ - keep on wishin'", :end 1028}
   {:start 1028, :name "FONSO - shawty", :end 1135}
   {:start 1135, :name "$NAKE - TOO EASY (w GIMBIT)", :end 1281}
   {:start 1281, :name "ViNi $AN - PEEK A BOO", :end 1454}
   {:start 1454, :name "Goupil. - BUST IT UP", :end 1617}
   {:start 1617, :name "Sappy - NINE TO YO DOME", :end 1748}
   {:start 1748, :name "DJ YUNG VAMP - IN NEW YORK MY NEGGA DONT MILLI ROCK", :end 1933}
   {:start 1933, :name "[ bsd.u ] - tyler", :end 2011}
   {:start 2011, :name "(no_show) - WITH A VENGEANCE", :end 2163}
   {:start 2163, :name "N X X Y $ - WIT MY GUN w Fungi", :end 2385}
   {:start 2385, :name "Sappy - Trust None (slowed)", :end 2604}
   {:start 2604, :name "t1e.6k - 2 LATE w NGK999", :end 2752}
   {:start 2752, :name "TOMMY II X DRAE DA SKI MASK - DIRTY TRAMP", :end 3032}
   {:start 3032, :name "GIMBIT - FREAK NO MORE w DVLTEM", :end 3134}
   {:start 3134, :name "GREEN PICCOLO - Wait", :end 3354}
   {:start 3354, :name "Chvnge Up x half blunt prince - He Doesn't Feel Love (slowed)", :end 3442}
   {:start 3442, :name "CARTIER ZOMBIE - CLOUD OF SMOKE W ROLAND JONES", :end 3528}
   {:start 3528, :name "BERRYMANE - EYE FOR AN EYE", :end 3666}
   {:start 3666, :name "xxx & ski mask - the slump god (knarf.✝ flip)", :end 3737}
   {:start 3737, :name "GREEN PICCOLO - Please Don't", :end 3993}
   {:start 3993, :name "cokeboy - AIN'T GONNA STOP ME w erick", :end 4145}
   {:start 4145, :name "KVN$ - Gankin' Deez Fools (slowed)", :end 4266}
   {:start 4266, :name "N9 - drive by", :end 4425}
   {:start 4425, :name "KOPHEE KUT - I can't control my Kut", :end 4547}
   {:start 4547, :name "coldcobain - I'M ON A BEAN", :end 4650}
   {:start 4650, :name "yungmaple - HIT EM WITH THE BLADE NOT THE STRAP", :end 4785}
   {:start 4785, :name "K e u z. ✞ - PASSED OUT", :end 4933}
   {:start 4933, :name "GREEN PICCOLO - You Already Snow", :end 5114}
   {:start 5114, :name "leftfield - ALL THE WAY GONE", :end 5201}
   {:start 5201, :name "PHXNKY FLEXX - Thats My H0e", :end 5285}
   {:start 5285, :name "raigeki - KlLL U ON CAMERA", :end 5427}
   {:start 5427, :name "vahybz - hydro", :end 5580}
   {:start 5580, :name "PXSSXSSXD - GOING DOWN", :end 5751}
   {:start 5751, :name "N X X Y $ - THEY WILL KNOW ME w KVN$", :end 5915}
   {:start 5915, :name "DESTRO - $CREW IT w CHALEUR", :end 6091}
   {:start 6091, :name "DVLTEM - SAUCE IT UP", :end 6250}
   {:start 6250, :name "LIA$ - WEED IN THE PURSE", :end 6372}
   {:start 6372, :name "ViNi $AN - B''CH HOLD ON", :end 6535}
   {:start 6535, :name "TUNDRAMANE - BLADE", :end 6639}
   {:start 6639, :name "Xavier Wulf - Kurokumo (Eric Dingus Remix)", :end 6870}
   {:start 6870, :name "SPYDER550 - GODDD MODE", :end 7082}
   {:start 7082, :name "ViNi $AN - FOR REAL", :end 7227}])

(deftest get-sections-from-youtube-tests
  (is (= lofi-cozy-winter-sections (get-sections "_tV5LEBDs7w")))
  (is (= trappin-in-paradise-50-sections (get-sections "HjxZYiTpU3k"))))

