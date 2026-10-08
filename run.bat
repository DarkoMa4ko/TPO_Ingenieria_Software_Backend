@echo off
REM Compila y ejecuta el ajedrez SIN Maven (alcanza con un JDK 17 o superior).
REM   run.bat                     partida nueva
REM   run.bat --fen "<posicion>"  partida desde una posicion FEN
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b chess-core\src\main\*.java chess-cli\src\main\*.java > out\fuentes.tmp
setlocal enabledelayedexpansion
set "ARGS="
(for /f "usebackq delims=" %%f in ("out\fuentes.tmp") do (
  set "p=%%f"
  echo "!p:\=/!"
)) > out\fuentes.txt
javac --release 17 -encoding UTF-8 -d out @out\fuentes.txt || exit /b 1
java -Dfile.encoding=UTF-8 -cp out chess.cli.ConsoleApp %*
