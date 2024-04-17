# Build Globalplatformpro:
### Download JDK
https://adoptium.net/temurin/releases/?os=linux&package=jdk&version=8&arch=x64

### Move to folder
extract and move to /usr/lib/jvm/

### Set JAVA_HOME and PATH
export JAVA_HOME=/usr/lib/jvm/jdk8u402-b06
export PATH=$JAVA_HOME/bin:$PATH

### Download Globalplatformpro and change version
git clone from repo
git checkout 15bdd14

### Build project Globalplatformpro
mvn package && ant


# Load applet onto card
### Make sure you are in /card dir and java version is 8

### Upload applet to card
mvn clean install


# Use Globalplatformpro to list applets on card
### Find all connected things and their AIDs
java -jar gp.jar --list
