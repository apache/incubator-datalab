# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

sc <- sparkR.session(MASTER)

full_path <- function(file_path) {
    working_storage <- "WORKING_STORAGE"
    output_directory <- "rstudio"
    protocol_name <- "PROTOCOL_NAME"
    sprintf("%s://%s/%s/%s", protocol_name, working_storage, output_directory, file_path)
}

carriers <- read.df(full_path("carriers"), "parquet")
createOrReplaceTempView(carriers, "carriers")
printSchema(carriers)

airports <- read.df(full_path("airports"), "parquet")
createOrReplaceTempView(airports, "airports")
printSchema(airports)

flights <- read.df(full_path("flights"), "parquet")
createOrReplaceTempView(flights, "flights")
printSchema(flights)

library(ggplot2)
library(reshape2)

delay_sql <- sql("
SELECT SUBSTR(c.description, 0, 15) as Carrier, WorkDayDelay, WeekendDelay
FROM
       (SELECT CEIL( AVG(f.ArrDelay + f.DepDelay) ) as WorkDayDelay, f.UniqueCarrier
        FROM flights f
        WHERE f.DayOfWeek < 6
        GROUP BY f.UniqueCarrier
        ORDER BY WorkDayDelay desc
        LIMIT 10) t
    JOIN
       (SELECT CEIL( AVG(f.ArrDelay + f.DepDelay) ) as WeekendDelay, f.UniqueCarrier
        FROM flights f
        WHERE f.DayOfWeek > 5
        GROUP BY f.UniqueCarrier) t1
      ON t.UniqueCarrier = t1.UniqueCarrier
    JOIN carriers c
      ON t.UniqueCarrier = c.code
ORDER BY WeekendDelay DESC, WorkDayDelay DESC
")

delay <- collect(delay_sql)
delay_melt <- melt(delay[c('Carrier', 'WorkDayDelay', 'WeekendDelay')])

color_range_days <- c("#2966FF", "#61F2FF")

ggplot(data=delay_melt, aes(x=Carrier, y=value, fill=variable)) +
    geom_bar(stat="identity", width=.7, position="dodge") +
    stat_summary(fun.y=mean, geom = "line", mapping = aes(group = 1), color="red") +
    stat_summary(fun.y=mean, geom = "point", mapping = aes(group = 1), color="red") +
    theme(legend.position="right", axis.text.x=element_text(angle=90)) +
    labs(x="Carrier", y="Minutes", fill="Day Type") +
    coord_fixed(ratio = .2) +
    scale_fill_manual(values=color_range_days) +
    scale_y_continuous(breaks=seq(0, 30, 5))

top_flights_sql <- sql("
SELECT t.cnt as FlightsAmt, carriers.description as Carrier
FROM (
    SELECT count(*) as cnt, flights.UniqueCarrier as carrier_code
    FROM flights
    GROUP BY flights.UniqueCarrier LIMIT 6) t
LEFT JOIN carriers
  ON t.carrier_code = carriers.code
")

top_flights <- collect(top_flights_sql)

ggplot(transform(transform(top_flights, value=FlightsAmt/sum(FlightsAmt)), labPos=cumsum(FlightsAmt)-FlightsAmt/2),
       aes(x="", y = FlightsAmt, fill = Carrier)) +
    geom_bar(width = 1, stat = "identity") +
    coord_polar("y", start=0) +
    scale_fill_brewer(palette="Dark2") +
    theme_bw() +
    theme(axis.text.x=element_blank() ,panel.grid.major=element_blank(),panel.grid.minor = element_blank(),panel.border = element_blank()) +
    geom_text(size=4, aes(y=labPos, label=scales::percent(value))) +
    geom_text(size=3, aes(x=1.8, y=labPos, label=top_flights$Carrier)) +
    theme(legend.position="none")

distance_sql = sql("
SELECT SUBSTR(c.description, 0, 15) as Carrier, COUNT(Distance) AS Distance
FROM flights f
JOIN carriers c
  ON f.UniqueCarrier = c.code
GROUP BY c.description
ORDER BY distance DESC
LIMIT 10
")

distance <- collect(distance_sql)

distance$Carrier <- factor(distance$Carrier, levels = distance$Carrier[order(-distance$Distance)])

color_range <-  c("#2966FF", "#2E73FF","#3380FF", "#388CFF", "#3D99FF", "#42A6FF", "#47B2FF", "#4CBFFF", "#52CCFF",
                  "#57D9FF", "#5CE6FF", "#61F2FF", "#66FFFF")

ggplot(data=distance, aes(x=Carrier, y=Distance, fill=Carrier)) +
    geom_bar(stat="identity", width=.7, position="dodge") +
    theme(axis.text.x=element_text(angle=90)) +
    scale_fill_manual(values=color_range) +
    theme(legend.position="none")

