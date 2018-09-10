#!/bin/sh

export TZ='America/New_York'

cd /apps/snapshots

pkill -f SeasonalSnapshots
unzip -oq seasonalsnapshots-0.0.1-SNAPSHOT.jar -x "*/snapshots/*"
java -Dspring.profiles.active=prod -cp ./:./lib/*:./:./BOOT-INF/classes:./BOOT-INF/lib/* com.chubbybuttons.seasonalsnapshots.SeasonalSnapshotsApplication
