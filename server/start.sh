#!/bin/bash

VMOptions=-Xmx4G
VMOptions+=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
VMOptions+=" -javaagent:$(echo mixin-*.jar)"

ProgramArgs='nogui --serverJar cache/patched_1.16.5.jar'

java $VMOptions -jar server.jar $ProgramArgs 
