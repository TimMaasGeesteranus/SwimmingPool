#!/bin/bash
#Borys :p
# Convert the JavaCard application to a CAP file
# -classdir: specifies the directory containing the compiled classes
# -out: specifies the output format (CAP)
# -v: enables verbose mode
# -applet: specifies the applet AID and class name
# -exportpath: specifies the export path for API files
./sdk/bin/converter -classdir ./target/classes -out CAP -v -applet 0x11:0x22:0x33:0x44:0x55:0x66 Card -exportpath ./sdk/api_export_files nl.ru.spp.group5 0x11:0x22:0x33:0x44:0x55 1.0

# Install the generated CAP file onto the JavaCard
java -jar ./GlobalPlatformPro/tool/target/gp.jar --install ./target/classes/nl/ru/spp/group5/javacard/group5.cap
