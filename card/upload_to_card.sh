#!/bin/bash

./sdk/bin/converter -classdir ./target/classes -out CAP -v -applet 0x11:0x22:0x33:0x44:0x55:0x66 Card -exportpath ./sdk/api_export_files nl.ru.spp.groupname 0x11:0x22:0x33:0x44:0x55 1.0

java -jar ./GlobalPlatformPro/tool/target/gp.jar --install ./target/classes/nl/ru/spp/groupname/javacard/groupname.cap
