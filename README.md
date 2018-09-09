# SeasonalSnapshots
Takes a snapshot from Nest IQ Cameras at sunrise, solar noon, and sunset each day to cycle through the progression
## General Overview
Seasonal Snapshots is a Java built application based on Spring IO and a collection of open source frameworks.  Modules of note are:
* Spring Security and Jasypt
 ** This provides the basic protection of the site (using the basic of single account authentication for the website) and encryption of keys accessed through application.properties.
 * Spring Web
 ** This provides REST based access to information used in the captured snapshots
## Integrations
There are two main integrations used to provide the data used in Seasonal Snapshots:
* Nest API
** Nest IQ Outdoor cameras were used in the development, but it should work for any Nest Camera with the granted permissions.  The API use is fairly straightforward once you work through the OAute token generation
* Sunrise/Sunset API
** It turned out be a very simple but useful API. For a given latitude/longitude, an API is exposed that gives you the times for a specified day that will be sunrise, solar noon, and sunset.
## Setup
