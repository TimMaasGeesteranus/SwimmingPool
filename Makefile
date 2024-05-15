# Path to the Java Card Development Kit
JC_HOME=util/java_card_kit-2_2_1

# Version of JCardSim to use;
JCARDSIM=jcardsim-3.0.4-SNAPSHOT

# Beware that only JCardSim-3.0.4-SNAPSHOT.jar includes the classes
# AIDUtil and CardTerminalSimulator, so some of the code samples on
# https://jcardsim.org/docs do not work with older versions
#    JCARDSIM=jcardsim-2.2.1-all
#    JCARDSIM=jcardsim-2.2.2-all

# Classpath for JavaCard code, i.e., the smartcard applet; this includes
# way more than is probably needed
JC_CLASSPATH=${JC_HOME}/lib/apdutool.jar:${JC_HOME}/lib/apduio.jar:${JC_HOME}/lib/converter.jar:${JC_HOME}/lib/jcwde.jar:${JC_HOME}/lib/scriptgen.jar:${JC_HOME}/lib/offcardverifier.jar:${JC_HOME}/lib/api.jar:${JC_HOME}/lib/installer.jar:${JC_HOME}/lib/capdump.jar:${JC_HOME}/samples/classes:${CLASSPATH}

all: applet terminal quicktest swimmingpool

# Compilation rules for CalcApplet
applet: CalcApplet/bin/CalcApplet.class 

CalcApplet/bin/CalcApplet.class: CalcApplet/src/applet/CalcApplet.java
	javac -d CalcApplet/bin -cp ${JC_CLASSPATH}:CalcTerminal/src CalcApplet/src/applet/CalcApplet.java 

# Compilation rules for CalcTerminal
terminal: CalcTerminal/bin/terminal/CalcTerminal.class

CalcTerminal/bin/terminal/CalcTerminal.class: CalcTerminal/src/terminal/CalcTerminal.java
	javac -d CalcTerminal/bin -cp ${JC_HOME}:util/jcardsim/${JCARDSIM}.jar:CalcApplet/bin:CalcTerminal/bin CalcTerminal/src/terminal/CalcTerminal.java  

# Running quicktest in CalcTerminal
quicktest: CalcTerminal/bin/terminal/QuickTest.class

CalcTerminal/bin/terminal/QuickTest.class: CalcTerminal/src/terminal/QuickTest.java
	javac -d CalcTerminal/bin -cp ${JC_HOME}:util/jcardsim/${JCARDSIM}.jar:CalcApplet/bin CalcTerminal/src/terminal/QuickTest.java

runquicktest: 
	# Sends some sample APDUs to the CalcApplet
	java -cp util/jcardsim/${JCARDSIM}.jar:CalcTerminal/bin:CalcApplet/bin terminal.QuickTest

# Running terminal GUI
runterminal: 
	# Runs the GUI terminal
	java -cp util/jcardsim/${JCARDSIM}.jar:CalcTerminal/bin:CalcApplet/bin terminal.CalcTerminal

# SwimmingPool project specific variables
SP_SRC_DIR=SwimmingPool/card/src/main/java
SP_BIN_DIR=SwimmingPool/bin
SP_PACKAGE=nl.ru.spp.groupname
SP_MAIN_CLASS=Card

# Compile the SwimmingPool project
swimmingpool: ${SP_BIN_DIR}/${SP_PACKAGE}/${SP_MAIN_CLASS}.class

${SP_BIN_DIR}/${SP_PACKAGE}/${SP_MAIN_CLASS}.class: ${SP_SRC_DIR}/${SP_PACKAGE}/${SP_MAIN_CLASS}.java
	@echo "Compiling ${SP_MAIN_CLASS}.java"
	javac -d ${SP_BIN_DIR} -cp "${JC_CLASSPATH}" $<

# Run the compiled Java program from the SwimmingPool project
runswimmingpool:
	@echo "Running ${SP_MAIN_CLASS} from classpath: ${SP_BIN_DIR}"
	java -cp "${SP_BIN_DIR}" ${SP_PACKAGE}.${SP_MAIN_CLASS}

# Clean up compiled files for all projects
clean:
	rm -rf CalcApplet/bin/*  
	rm -rf CalcTerminal/bin/*
	rm -rf ${SP_BIN_DIR}/*

.PHONY: all applet terminal quicktest runquicktest runterminal swimmingpool runswimmingpool clean


# javac -d SwimmingPool/bin -cp "util/java_card_kit-2_2_1/lib/apdutool.jar:util/java_card_kit-2_2_1/lib/apduio.jar:util/java_card_kit-2_2_1/lib/converter.jar:util/java_card_kit-2_2_1/lib/jcwde.jar:util/java_card_kit-2_2_1/lib/scriptgen.jar:util/java_card_kit-2_2_1/lib/offcardverifier.jar:util/java_card_kit-2_2_1/lib/api.jar:util/java_card_kit-2_2_1/lib/installer.jar:util/java_card_kit-2_2_1/lib/capdump.jar" SwimmingPool/card/src/main/java/nl/ru/spp/groupname/Card.java
