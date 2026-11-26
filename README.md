# GameShelf – Administrador de biblioteca de videojuegos

Aplicación de escritorio (Java + Swing) para organizar una colección de videojuegos compartida entre varios usuarios:
cada uno tiene su propio estado, notas y calificaciones, y hay un ranking con el promedio de todos.

## ⚠️ La base de datos NO está en este repositorio

La base de datos (usuarios, juegos, calificaciones e imágenes) **no se incluye en el repositorio porque está hosteada en un
servidor**. La aplicación se conecta a ella por red; por eso, al clonar el proyecto hay que configurar la conexión (ver más abajo).
Así los datos y las imágenes no quedan expuestos en el código ni dependen de la copia local de cada persona.

## Usuario administrador

| Dato | Valor |
|---|---|
| **Usuario** | `ABC` |
| **Contraseña** | `ABC_123` |
| **Rol** | Administrador |

El administrador es el único que puede **modificar los datos de un juego** y **dar de baja juegos**. Los usuarios comunes pueden
agregar juegos, y llevar su propio estado, horas, notas y calificaciones (privadas).

## Requisitos
- Java 17 o superior.

## Cómo ejecutarla
1. Descargá o cloná el repositorio.
2. Ejecutá `GameShelf.jar` (`java -jar GameShelf.jar`) o el script `ejecutar.bat` (Windows) / `./ejecutar.sh` (Linux/macOS).
   Si falta el `.jar`, se genera con `construir.bat` / `./construir.sh` (requiere un JDK 17+).

## Hostear tu propia base
Para publicar una base en una PC o servidor: `servidor.bat` / `./servidor.sh` (o `java -jar GameShelf.jar --servidor`) en la carpeta que
contiene `gameshelf-db.mv.db`. Muestra las direcciones para conectarse; hay que habilitar el puerto en el firewall.
Para importar datos de la versión anterior (JSON + carpeta `imagenes`): `java -jar GameShelf.jar --importar "<carpeta>"`.

## Tecnologías
Java 17, Swing, base de datos H2 (https://h2database.com, licencia MPL 2.0 / EPL 1.0), JDBC.
