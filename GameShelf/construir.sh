#!/bin/sh
# Compila GameShelf y arma GameShelf.jar (incluye el controlador de MariaDB). Requiere un JDK 17 o superior.
cd "$(dirname "$0")"
rm -rf build && mkdir build
(cd build && jar xf ../lib/mariadb-java-client.jar && rm -f META-INF/MANIFEST.MF META-INF/*.SF META-INF/*.RSA META-INF/*.DSA) || exit 1
javac --release 17 -cp lib/mariadb-java-client.jar -encoding UTF-8 -d build $(find src -name '*.java') || exit 1
jar cfe GameShelf.jar gameshelf.Main -C build . || exit 1
rm -rf build
echo "Listo: GameShelf.jar"
