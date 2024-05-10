# Load applet onto card

## Build Globalplatformpro:
### Download JDK
https://adoptium.net/temurin/releases/?os=linux&package=jdk&version=8&arch=x64

### Move to folder
extract and move to /usr/lib/jvm/

### Set JAVA_HOME and PATH
export JAVA_HOME=/usr/lib/jvm/jdk8u402-b06 </br>
export PATH=$JAVA_HOME/bin:$PATH

### Download Globalplatformpro and change version
git clone from repo </br>
git checkout 15bdd14

### Build project Globalplatformpro
mvn package && ant

### Make sure you are in /card dir and java version is 8

### Upload applet to card
mvn clean install




# Use Globalplatformpro to list applets on card
### Find all connected things and their AIDs
java -jar gp.jar --list

# Run Terminals
### Make sure you are in /terminals

### Build project
mvn package

### Run one of three terminals
java -cp target/terminals-1.0-SNAPSHOT.jar nl.ru.spp.group5.InitTerminal </br>
java -cp target/terminals-1.0-SNAPSHOT.jar nl.ru.spp.group5.AccessGateTerminal </br>
java -cp target/terminals-1.0-SNAPSHOT.jar nl.ru.spp.group5.VendingMachineTerminal



