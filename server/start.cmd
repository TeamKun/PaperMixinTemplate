@echo off

set VMOptions=-Xmx4G
set VMOptions=%VMOptions% -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005

set ProgramArgs=nogui --serverJar cache/patched_1.16.5.jar

java %VMOptions% -jar server.jar %ProgramArgs%

pause
