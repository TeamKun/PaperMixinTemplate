@echo off

set VMOptions=-Xmx4G
set VMOptions=%VMOptions% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
set VMOptions=%VMOptions% -javaagent:mixin-0.8.2.jar

set ProgramArgs=nogui --serverJar cache/patched_1.16.5.jar

java %VMOptions% -jar server.jar %ProgramArgs%

pause
