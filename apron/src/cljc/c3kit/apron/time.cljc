(ns c3kit.apron.time
	#?(:clj (:import
						[java.util Date Calendar GregorianCalendar TimeZone]
						[java.text SimpleDateFormat]))
	#?(:cljs (:require
						 [cljs-time.format :as timef]
						 [cljs-time.coerce :as timec]
						 [cljs-time.core :as time])))

(defn milliseconds
	"Our atomic unit"
	[n] n)

(defn seconds
	"Converts seconds to milliseconds"
	[n] (Math/round (double (* n 1000))))

(defn minutes
	"Converts minutes to milliseconds"
	[n] (Math/round (double (* n 60000))))

(defn hours
	"Converts hours to milliseconds"
	[n] (Math/round (double (* n 3600000))))

(defn days
	"Converts days to milliseconds"
	[n] (Math/round (double (* n 86400000))))

(defn months
	"Converts a number into a format that the Calendar object understands to be an amount of months"
	[n] [:months (if (float? n) (Math/round n) n)])

(defn years
	"Converts a number into a format that the Calendar object understands to be an amount of years"
	[n] [:years (if (float? n) (Math/round n) n)])

(defn now
	"Returns a java.util.Date or js/Date object that represents the current date and time in UTC"
	[]
	#?(:clj (Date.) :cljs (js/Date.)))

(defn utc-offset
	"The offset (milliseconds) between the local timezone and UTC. (AZ -> -7hrs)"
	([] (utc-offset (now)))
	([date]
	 #?(:clj  (.getOffset (TimeZone/getDefault) (.getTime date))
			:cljs (* -1 (minutes (.getTimezoneOffset date))))))

(defn from-epoch
	"Create Date relative to epoch, adjusted for timezone offset
	(from-epoch 0)"
	[^long millis-since-epoch]
	#?(:clj (Date. millis-since-epoch) :cljs (js/Date. millis-since-epoch)))

(def epoch (from-epoch 0))

(defn instant? [thing] (instance? #?(:clj Date :cljs js/Date) thing))
(defn millis-since-epoch [date] (.getTime date))

(defn millis-between
	"Milliseconds that separate the two times.  Negative if b is after a."
	[a b]
	(- (millis-since-epoch a) (millis-since-epoch b)))

(defn ->utc
	"Returns a new date representing time in UTC timezone, assuming given date is in local timezone."
	[^Date date]
	(from-epoch (- (millis-since-epoch date) (utc-offset date))))

(defn ->local
	"Returns a new date representing time in the timezone, assuming given date is in UTC timezone."
	[^Date date]
	(from-epoch (+ (millis-since-epoch date) (utc-offset date))))

(defn local
	"Create a Date assuming parameters are local timezone.
	e.g. in AZ: (local 2020 1 1 0 0 0) -> 2020-01-01T07:00:00.000-00:00"
	([year month day] (local year month day 0 0 0))
	([year month day hour minute] (local year month day hour minute 0))
	([year month day hour minute second]
	 #?(:clj  (.getTime (GregorianCalendar. year (dec month) day hour minute second))
			:cljs (js/Date. year (dec month) day hour minute second))))

(defn utc
	"Create a Date assuming parameters are UTC timezone.
	e.g. (utc 2020 1 1 0 0 0) -> 2020-01-01T00:00:00.000-00:00"
	([year month day] (utc year month day 0 0 0))
	([year month day hour minute] (utc year month day hour minute 0))
	([year month day hour minute second] (->local (local year month day hour minute second))))

(defn before?
	"Expects two Dates as arguments. The function returns true if the
	first date comes before the second date and returns false otherwise."
	[^Date first ^Date second]
	#?(:clj  (.before first second)
		 :cljs (< (.getTime first) (.getTime second))))


(defn after?
	"Expects two Date as arguments. The function returns true if the
	first date comes after the second date and returns false otherwise."
	[^Date first ^Date second]
	#?(:clj  (.after first second)
		 :cljs (> (.getTime first) (.getTime second))))

(defn between?
	"Expects the three Dates as arguments. The first date is the date
	being evaluated; the second date is the start date; the last date is the
	end date. The function returns true if the first date is between the start
	and end dates."
	[^Date date ^Date start ^Date end]
	(and
		(after? date start)
		(before? date end)))

#?(:clj (defn to-calendar
					"Converts a Date object into a GregorianCalendar object"
					[datetime]
					(doto (GregorianCalendar.)
						(.setTime datetime))))



(defn- mod-time-by-units
	"Modifies the value of a Date object. Expects the first argument to be
	a Date, the second argument to be a vector representing the amount of time to be changed,
	and the last argument to be either a + or - (indicating which direction to modify time)."
	[time [unit n] direction]
	#?(:clj
					 (let [calendar (GregorianCalendar.)
								 n (direction n)
								 unit (case unit
												:months Calendar/MONTH
												:years Calendar/YEAR
												(throw (ex-info (str "invalid duration unit: " unit) {:unit unit})))]
						 (.setTime calendar time)
						 (.add calendar unit (int n))
						 (.getTime calendar))
		 :cljs (let [goog-dt (timec/from-long time)
								 dir-fn (if (= + direction) time/plus time/minus)
								 period (time/period unit (int n))
								 new-goog (dir-fn goog-dt period)]
						 (timec/to-date new-goog))))

(defn- mod-time
	"Modifies the value of a Date. Expects the first argument to be
	a Date object, the second argument to be an amount of milliseconds, and
	the last argument to be either a + or - (indicating which direction to
	modify time)."
	[time bit direction]
	(cond
		(number? bit) #?(:clj  (Date. (direction (.getTime time) (long bit)))
										 :cljs (js/Date. (direction (.getTime time) (long bit))))
		(vector? bit) (mod-time-by-units time bit direction)))

(defn before
	"Rewinds the time on a Date object. Expects a Date object as the first
	argument and a number of milliseconds to rewind time by."
	[time & bits]
	(reduce #(mod-time %1 %2 -) time bits))

(defn after
	"Fast-forwards the time on a Date object. Expects a Date object as the first
	argument and a number of milliseconds to fast-forward time by."
	[time & bits]
	(reduce #(mod-time %1 %2 +) time bits))

(def earlier? before?)
(def later? after?)
(def earlier before)
(def later after)

(defn ago
	"Returns a Date some time (n) before now."
	[n]
	(before (now) n))

(defn from-now
	"Returns a Date some time (n) after now."
	[n]
	(after (now) n))

(defn formatter [format]
	#?(:clj  (let [sdf (SimpleDateFormat. format)]
						 (.setTimeZone sdf (TimeZone/getTimeZone "UTC"))
						 sdf)
		 :cljs (timef/formatter format)))

(def date-formats
	{
	 :http       (formatter "EEE, dd MMM yyyy HH:mm:ss ZZZ")
	 :rfc1123    (formatter "EEE, dd MMM yyyy HH:mm:ss ZZZ")
	 :rfc822     (formatter "EEE, dd MMM yyyy HH:mm:ss Z")
	 :ref3339    (formatter "yyyy-MM-dd'T'hh:mm:ss-00:00")
	 :long-no-tz (formatter "EEE, dd MMM yyyy HH:mm:ss")
	 :iso8601    (formatter "yyyy-MM-dd HH:mm:ssZ")
	 :dense      (formatter "yyyyMMddHHmmss")
	 :ymd        (formatter "yyyyMMdd")
	 :webform    (formatter "yyyy-MM-dd")
	 :friendly   (formatter "EEE - MMM d, yyyy")
	 :short      (formatter "MMM d, yyyy")
	 })

(defn- ->formatter [format]
	(cond
		(keyword? format) (format date-formats)
		(string? format) (formatter format)
		(instance? #?(:clj SimpleDateFormat :cljs timef/Formatter) format) format
		:else (throw (ex-info (str "Unhandled date format: " format) {:format format}))))

(defn parse
	"Parses text into a Java Date object. Expects a keyword, string, or SimpleDateFormat
	object as the first object and a string representing the date as the second argument.
	The date is assumed to be in UTC."
	[format value]
	(let [formatter (->formatter format)]
		#?(:clj  (.parse formatter value)
			 :cljs (let [goog-dt (timef/parse formatter value)]
							 (timec/to-date goog-dt)))))

(defn unparse
	"Returns a string that is populated with a formatted date and time. Expects the
	first argument to be the requested format and the second argument to be the date
	to be formatted.
	The following are options for the first argument:
	1. Keyword - :http, :rfc1123, :iso8601, :dense
	2. String - must be a valid argument to the SimpleDateFormat Java Object
	3. SimpleDateFormat - Java Object"
	[format value]
	(if value
		(let [formatter (->formatter format)]
			#?(:clj  (.format formatter value)
				 :cljs (let [goog-dt (timec/from-date value)]
								 (timef/unparse formatter goog-dt))))))

(defn year
	"Returns the Date's year (local timezone)."
	[^Date datetime]
	#?(:clj  (.get (to-calendar datetime) Calendar/YEAR)
		 :cljs (.getFullYear datetime)))

(defn month
	"Returns the Date's month (local timezone)."
	[^Date datetime]
	(inc #?(:clj  (.get (to-calendar datetime) Calendar/MONTH)
					:cljs (.getMonth datetime))))

(defn day
	"Returns the Date's day (local timezone)."
	[^Date datetime]
	#?(:clj  (.get (to-calendar datetime) Calendar/DAY_OF_MONTH)
		 :cljs (.getDate datetime)))

(defn hour
	"Returns the Date's hour (24-hour clock) (local timezone)."
	[^Date datetime]
	#?(:clj  (.get (to-calendar datetime) Calendar/HOUR_OF_DAY)
		 :cljs (.getHours datetime)))

(defn minute
	"Returns the Date's minute."
	[^Date datetime]
	#?(:clj  (.get (to-calendar datetime) Calendar/MINUTE)
		 :cljs (.getMinutes datetime)))

(defn sec
	"Returns the Date's second."
	[^Date datetime]
	#?(:clj  (.get (to-calendar datetime) Calendar/SECOND)
		 :cljs (.getSeconds datetime)))

(defn bounds [start end]
	(list start end))

(defn bounds? [thing]
	(and (seq? thing)
			 (= 2 (count thing))
			 (instant? (first thing))
			 (instant? (first (rest thing)))))

(defn start-of [bounds] (first bounds))
(defn end-of [bounds] (first (rest bounds)))

(defn during? [bounds instant]
	(and (after? instant (start-of bounds))
			 (before? instant (end-of bounds))))

