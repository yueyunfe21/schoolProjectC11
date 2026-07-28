@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "D:\mavenProject\DHXY-cr271"
set "DHXY_CLIENT_DEPS="
for /f "usebackq delims=" %%L in ("target\client-dependency-classpath.txt") do set "DHXY_CLIENT_DEPS=!DHXY_CLIENT_DEPS!%%L"
set "CLASSPATH=D:\mavenProject\DHXY-cr271\target\classes;%DHXY_CLIENT_DEPS%"

start "" /b "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe" ^
  com.bot.dhxy.AutoBot ^
  --bot.run.show-ui=false ^
  --bot.run.auto-start=true ^
  --bot.run.init-game-window=true ^
  --bot.run.tasks=xiuluo_v2
