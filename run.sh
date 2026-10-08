#!/bin/sh
# Compila y ejecuta el ajedrez SIN Maven (alcanza con un JDK 17 o superior).
#   ./run.sh                    partida nueva
#   ./run.sh --fen "<posición>" partida desde una posición FEN
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac --release 17 -encoding UTF-8 -d out $(find chess-core/src/main chess-cli/src/main -name '*.java')
exec java -Dfile.encoding=UTF-8 -cp out chess.cli.ConsoleApp "$@"
