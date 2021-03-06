(ns twitter-example.core
 (:require [clojure.set]
           [clojure.string :as s]
           [clj-http.client :as client]
           [overtone.at-at :as overtone]
           [twitter.api.restful :as twitter]
           [twitter.oauth :as twitter-oauth]
           [environ.core :refer [env]]))

; We are generating tweets based on templates, similar to the game Apples to
; Apples: https://en.wikipedia.org/wiki/Apples_to_Apples
; We start with two lists of strings: One list contains string with blank
; spaces, the other list is used to fill in these spaces.


; We define the "templates" - these strings are the skeleton of the tweet
; We will later replace every occurence of ___ with a string that we chose
; randomly from the list "blanks".
(def templates ["I hear he eats ___"
                "The police arrested ___ in connection with the robbery."
                "I don't know if ___ will visit us next Sunday."
                "She sent a card to ___."
                "Doing that sort of thing makes you look like ___."])

; Next we define the "blanks"
(def blanks ["a Haunted House"
             "Rich Hickey"
             "a purple dinosaur"
             "Junk Mail"
             "Pirates"])

; generate-sentence returns a random sentence, built by choosing one template
; string at random and filling in the blank space (___) with a randomly chosen
; string from the blanks list.
(defn generate-sentence []
  (let [template (rand-nth templates)
        blank (rand-nth blanks)]
      (s/replace template "___" blank)))

; We retrieve the twitter credentials from the profiles.clj file here.
; In profiles.clj we defined the "env(ironment)" which we use here
; to get the secret passwords we need.
; Make sure you add your credentials in profiles.clj and not here!
(def twitter-credentials (twitter-oauth/make-oauth-creds $TWITTER_API_KEY 
                                                         $TWITTER_API_SECRET
                                                         $TWITTER_ACCESS_TOKEN
                                                         $TWITTER_ACCESS_TOKEN_SECRET))

; Tweets are limited to 140 characters. We might randomly generate a sentence
; with more than 140 characters, which would be rejected by Twitter.
; So we check if our generated sentence is longer than 140 characters, and
; don't tweet if so.
(defn tweet [text]
  (when (and (not-empty text) (<= (count text) 140))
   (try (twitter/statuses-update :oauth-creds twitter-credentials
                                 :params {:status text})
        (catch Exception e (println "Something went wrong: " (.getMessage e))))))

; Generate a sentence and tweet it.
(defn tweet-sentence []
  (tweet (generate-sentence)))


(def my-pool (overtone/mk-pool))

(defn -main [& args]
  ;; every 2 hours
  (println "Started up")
  (println (tweet-sentence))
  (overtone/every (* 1000 60 60 2) #(println (tweet-sentence)) my-pool))
