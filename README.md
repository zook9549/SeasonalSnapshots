# SeasonalSnapshots
Takes a snapshot from Nest IQ Cameras at sunrise, solar noon, and sunset each day to cycle through the progression
## General Overview
Seasonal Snapshots is a Java built application based on Spring IO and a collection of open source frameworks.  Modules of note are:
* Spring Security and Jasypt
  * This provides the basic protection of the site (using the basic of single account authentication for the website) and encryption of keys accessed through application.properties.
 * Spring Web
  * This provides REST based access to information used in the captured snapshots
## Integrations
There are two main integrations used to provide the data used in Seasonal Snapshots:
* Nest API
  * Nest IQ Outdoor cameras were used in the development, but it should work for any Nest Camera with the granted permissions.  The API use is fairly straightforward once you work through the OAute token generation
* Sunrise/Sunset API
  * It turned out be a very simple but useful API. For a given latitude/longitude, an API is exposed that gives you the times for a specified day that will be sunrise, solar noon, and sunset.
## Setup
The biggest pain in the ass will be your Nest camera install!  Once that is done, you can get to work here.
### Nest API Setup
* Go to https://console.developers.nest.com/ and setup a new OAuth client.
  * No need to put in a redirect URL. While that app is setup for the callback, it's unnecessary. Your end goal is to get the token which doesn't expire until you general a new token or a bagillion years (whatever comes first).  Since this app wasn't built for general reusability across Nest clients (v2 perhaps?), you don't have to be too concerned with the robustness of this step.
  * From the generated OAuth client setup, go to the URL provided.  Login in with you Nest account (the one tied to your cameras).  You will general a PIN.  Save this.
  * Using your favorite web service client (I used Postman), generate a call to https://api.home.nest.com/oauth2/access_token. It will need to be a post and your Body parameters (not headers) will need to include grant_type, code (the PIN), client_id, and client_secret.  The Nest docs are decent on these steps.  The response will give you your access_token.  This is your key to the kingdom.  Save this and you will use Jasypt to encrypt in a later step.
  * Go ahead and run atest with your token at https://developer-api.nest.com.  Pass in your access_token in the header (according to Nest API docs).  You should get a response back with your camera information in JSON format.
  * Test out the field returned from snapshot_url.  You should get an up-to-the-moment pic from your camera.  Interesting enough, this link is preauthorized so anybody could creep up on you.  I'm sure it expires quickly (?) but I didn't test it out.
### Sunrise Sunset API
  * Nothing fancy here.  No authentication needed.  You will just need to snag your latitude/longitude.  I used Google Maps for this.  You don't need to be uber-precise.  Just your general timezone.
  * https://api.sunrise-sunset.org/json?lat={latitude}&lng=-{longitude}
### Application.Properties
* While this seems straightforward, there are a few steps here that are necessary to protect your tokens and configure this for your own use
  * Encryption
    * Download Jasypt for local encruption. The bat files are helpful so you don't need to finagle with the jar files
    * Think of an encryption key and use this according to Jasypt help docs
    * Encrypt your token, placing it in ENC(encrypted token)
    * Encrypt your login password, wrapping it in ENC(...) as well
  * You need to make your encryption password available during runtime.  For local, pass it in as a command-line variable: -Djasypt.encryptor.password=
  * You'll also need to do this for your Maven build for package.
  * For prod, create application-prod.properties and add this property with your key.  Place it in the root directory of your application, some place you will use run.sh (note - I run dev on Windows and prod on Linux)
  * Add your Sunrise Sunset URL that includes your lat and lng
## Running
If you followed the setup steps, running is just firing up the Spring Boot main class, SeasonalSnapshotsApplication.  This will run an embedded Tomcat instance on port 8001 (modifiable within application.properties).
* You don't have to wait for sunrise to get going.  You can hit http://localhost:8001/test?phase=SUNRISE to do an immediate capture across all cameras.
* All snapshots are stored locally and accessible through the web server.  Each is stored under a lifecycle phase (sunrise, solar_noon, sunset).  I tried setting these as attributes on the image/snapshot, but this turned out to be super finicky across Windows and Linux.
* All view logic is on index.html.  It was hacked together and not clean, but it gets the job done right now.
