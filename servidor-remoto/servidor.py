
import argparse
import json
import sqlite3
import time

from contextlib import closing
from http.server import BaseHTTPRequestHandler
from http.server import ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from uuid import UUID


# ============================================
# 1. CONFIGURACION
# ============================================

DIRECTORIO = Path(__file__).resolve().parent

DATABASE = DIRECTORIO / "recordatorios_remotos.db"

CATEGORIAS = {
    "Trabajo",
    "Personal",
    "Hogar",
    "Estudios"
}

PRIORIDADES = {
    "Alta",
    "Media",
    "Baja"
}

ESTADOS = {
    "Pendiente",
    "En progreso",
    "Completado"
}


# ============================================
# 2. BASE DE DATOS
# ============================================

def conectar():

    conexion = sqlite3.connect(
        DATABASE,
        timeout=10
    )

    conexion.row_factory = sqlite3.Row

    return conexion


def inicializar_database():

    with closing(conectar()) as conexion:

        conexion.execute("PRAGMA journal_mode=WAL")

        conexion.execute("""
            CREATE TABLE IF NOT EXISTS recordatorios (

                uuid TEXT PRIMARY KEY,

                titulo TEXT NOT NULL,

                descripcion TEXT,

                categoria TEXT NOT NULL,

                prioridad TEXT NOT NULL,

                estado TEXT NOT NULL,

                ubicacion TEXT,

                fechaAviso TEXT,

                fechaCreacion INTEGER NOT NULL,

                fechaModificacion INTEGER NOT NULL,

                revision INTEGER NOT NULL,

                eliminado INTEGER NOT NULL DEFAULT 0
            )
        """)

        conexion.commit()


def convertir_registro(fila):

    registro = dict(fila)

    registro["eliminado"] = bool(
        registro["eliminado"]
    )

    return registro


# ============================================
# 3. VALIDACIONES
# ============================================

def validar_uuid(valor):

    try:

        return str(UUID(valor))

    except (ValueError, TypeError, AttributeError):

        raise ValueError("UUID invalido")


def validar_revision(valor):

    if type(valor) is not int or valor < 1:

        raise ValueError("Revision invalida")

    return valor


def validar_fecha(valor):

    if type(valor) is not int or valor < 0:

        raise ValueError("Fecha invalida")

    return valor


def texto_opcional(valor):

    if valor is not None and not isinstance(valor, str):

        raise ValueError("Campo de texto invalido")

    return valor


def validar_recordatorio(uuid, datos):

    if not isinstance(datos, dict):

        raise ValueError("Se esperaba un objeto JSON")

    uuid_recibido = validar_uuid(
        datos.get("uuid")
    )

    if uuid_recibido != uuid:

        raise ValueError("El UUID no coincide")

    titulo = datos.get("titulo")

    if not isinstance(titulo, str):

        raise ValueError("Titulo invalido")

    titulo = titulo.strip()

    if not titulo or len(titulo) > 100:

        raise ValueError(
            "El titulo debe contener entre 1 y 100 caracteres"
        )

    categoria = datos.get("categoria")
    prioridad = datos.get("prioridad")
    estado = datos.get("estado")

    if categoria not in CATEGORIAS:

        raise ValueError("Categoria invalida")

    if prioridad not in PRIORIDADES:

        raise ValueError("Prioridad invalida")

    if estado not in ESTADOS:

        raise ValueError("Estado invalido")

    eliminado = datos.get("eliminado", False)

    if type(eliminado) is not bool:

        raise ValueError("Campo eliminado invalido")

    return {

        "uuid": uuid,

        "titulo": titulo,

        "descripcion": texto_opcional(
            datos.get("descripcion")
        ),

        "categoria": categoria,

        "prioridad": prioridad,

        "estado": estado,

        "ubicacion": texto_opcional(
            datos.get("ubicacion")
        ),

        "fechaAviso": texto_opcional(
            datos.get("fechaAviso")
        ),

        "fechaCreacion": validar_fecha(
            datos.get("fechaCreacion")
        ),

        "fechaModificacion": validar_fecha(
            datos.get("fechaModificacion")
        ),

        "revision": validar_revision(
            datos.get("revision")
        ),

        "eliminado": eliminado
    }


# ============================================
# 4. SERVIDOR HTTP
# ============================================

class ServidorRecordatorios(BaseHTTPRequestHandler):

    def responder(self, codigo, datos):

        contenido = json.dumps(
            datos,
            ensure_ascii=False
        ).encode("utf-8")

        self.send_response(codigo)

        self.send_header(
            "Content-Type",
            "application/json; charset=utf-8"
        )

        self.send_header(
            "Content-Length",
            str(len(contenido))
        )

        self.end_headers()

        try:

            self.wfile.write(contenido)

        except (BrokenPipeError, ConnectionResetError):

            pass


    def error(self, codigo, mensaje):

        self.responder(
            codigo,
            {"error": mensaje}
        )


    def leer_json(self):

        longitud = int(
            self.headers.get("Content-Length", "0")
        )

        if longitud <= 0 or longitud > 1048576:

            raise ValueError("Tamano de solicitud invalido")

        contenido = self.rfile.read(longitud)

        return json.loads(
            contenido.decode("utf-8")
        )


    def obtener_ruta(self):

        return urlparse(self.path)


    def obtener_uuid_ruta(self):

        ruta = self.obtener_ruta().path

        partes = ruta.strip("/").split("/")

        if len(partes) != 2:

            return None

        if partes[0] != "recordatorios":

            return None

        return validar_uuid(partes[1])


    # ========================================
    # 5. SIMULACION DE ERRORES
    # ========================================

    def simular_error(self):

        modo = self.headers.get(
            "X-Test-Mode",
            ""
        )

        if modo == "server_error":

            self.error(
                500,
                "Error interno simulado"
            )

            return True

        if modo == "bad_request":

            self.error(
                400,
                "Solicitud invalida simulada"
            )

            return True

        if modo == "timeout":

            time.sleep(15)

        return False


    # ========================================
    # 6. GET
    # ========================================

    def do_GET(self):

        try:

            if self.simular_error():

                return

            ruta = self.obtener_ruta().path

            if ruta == "/health":

                self.responder(
                    200,
                    {
                        "status": "UP",
                        "service": "MisRecordatorios",
                        "version": "1.0"
                    }
                )

                return

            if ruta == "/recordatorios":

                with closing(conectar()) as conexion:

                    filas = conexion.execute("""
                        SELECT *
                        FROM recordatorios
                        ORDER BY fechaCreacion, uuid
                    """).fetchall()

                registros = [
                    convertir_registro(fila)
                    for fila in filas
                ]

                self.responder(200, registros)

                return

            uuid = self.obtener_uuid_ruta()

            if uuid is None:

                self.error(404, "Ruta no encontrada")

                return

            with closing(conectar()) as conexion:

                fila = conexion.execute("""
                    SELECT *
                    FROM recordatorios
                    WHERE uuid = ?
                """, (uuid,)).fetchone()

            if fila is None:

                self.error(
                    404,
                    "Recordatorio no encontrado"
                )

                return

            self.responder(
                200,
                convertir_registro(fila)
            )

        except (ValueError, TypeError) as error:

            self.error(400, str(error))

        except Exception as error:

            print("ERROR GET:", error)

            self.error(500, "Error interno del servidor")


    # ========================================
    # 7. PUT: CREAR O ACTUALIZAR
    # ========================================

    def do_PUT(self):

        try:

            if self.simular_error():

                return

            uuid = self.obtener_uuid_ruta()

            if uuid is None:

                self.error(404, "Ruta no encontrada")

                return

            datos = validar_recordatorio(
                uuid,
                self.leer_json()
            )

            with closing(conectar()) as conexion:

                # Serializa las escrituras concurrentes.
                conexion.execute("BEGIN IMMEDIATE")

                existente = conexion.execute("""
                    SELECT *
                    FROM recordatorios
                    WHERE uuid = ?
                """, (uuid,)).fetchone()

                if existente is not None:

                    revision_actual = existente["revision"]

                    if datos["revision"] < revision_actual:

                        conexion.rollback()

                        self.error(
                            409,
                            "Revision anterior a la almacenada"
                        )

                        return

                    if datos["revision"] == revision_actual:

                        registro = convertir_registro(
                            existente
                        )

                        conexion.rollback()

                        if registro == datos:

                            # Reintento identico:
                            # no se genera un duplicado.
                            self.responder(200, registro)

                        else:

                            self.error(
                                409,
                                "Misma revision con datos diferentes"
                            )

                        return

                    if existente["eliminado"] and not datos["eliminado"]:

                        conexion.rollback()

                        self.error(
                            409,
                            "El recordatorio fue eliminado"
                        )

                        return

                    if datos["fechaCreacion"] != existente["fechaCreacion"]:

                        conexion.rollback()

                        self.error(
                            409,
                            "La fecha de creacion no puede cambiar"
                        )

                        return

                conexion.execute("""
                    INSERT INTO recordatorios (

                        uuid,
                        titulo,
                        descripcion,
                        categoria,
                        prioridad,
                        estado,
                        ubicacion,
                        fechaAviso,
                        fechaCreacion,
                        fechaModificacion,
                        revision,
                        eliminado

                    ) VALUES (

                        :uuid,
                        :titulo,
                        :descripcion,
                        :categoria,
                        :prioridad,
                        :estado,
                        :ubicacion,
                        :fechaAviso,
                        :fechaCreacion,
                        :fechaModificacion,
                        :revision,
                        :eliminado

                    )

                    ON CONFLICT(uuid) DO UPDATE SET

                        titulo = excluded.titulo,
                        descripcion = excluded.descripcion,
                        categoria = excluded.categoria,
                        prioridad = excluded.prioridad,
                        estado = excluded.estado,
                        ubicacion = excluded.ubicacion,
                        fechaAviso = excluded.fechaAviso,
                        fechaModificacion = excluded.fechaModificacion,
                        revision = excluded.revision,
                        eliminado = excluded.eliminado

                """, datos)

                conexion.commit()

                fila = conexion.execute("""
                    SELECT *
                    FROM recordatorios
                    WHERE uuid = ?
                """, (uuid,)).fetchone()

            codigo = 201 if existente is None else 200

            self.responder(
                codigo,
                convertir_registro(fila)
            )

        except (ValueError, TypeError) as error:

            self.error(400, str(error))

        except Exception as error:

            print("ERROR PUT:", error)

            self.error(500, "Error interno del servidor")


    # ========================================
    # 8. DELETE: ELIMINACION LOGICA
    # ========================================

    def do_DELETE(self):

        try:

            if self.simular_error():

                return

            uuid = self.obtener_uuid_ruta()

            if uuid is None:

                self.error(404, "Ruta no encontrada")

                return

            parametros = parse_qs(
                self.obtener_ruta().query
            )

            revision = validar_revision(
                int(parametros.get("revision", ["0"])[0])
            )

            with closing(conectar()) as conexion:

                conexion.execute("BEGIN IMMEDIATE")

                fila = conexion.execute("""
                    SELECT *
                    FROM recordatorios
                    WHERE uuid = ?
                """, (uuid,)).fetchone()

                if fila is None:

                    conexion.rollback()

                    self.error(
                        404,
                        "Recordatorio no encontrado"
                    )

                    return

                if revision < fila["revision"]:

                    conexion.rollback()

                    self.error(409, "Revision desactualizada")

                    return

                if revision == fila["revision"]:

                    conexion.rollback()

                    if fila["eliminado"]:

                        self.responder(
                            200,
                            convertir_registro(fila)
                        )

                    else:

                        self.error(409, "Conflicto de revision")

                    return

                conexion.execute("""
                    UPDATE recordatorios

                    SET eliminado = 1,

                        revision = ?,

                        fechaModificacion = ?

                    WHERE uuid = ?

                """, (
                    revision,
                    int(time.time() * 1000),
                    uuid
                ))

                conexion.commit()

                fila = conexion.execute("""
                    SELECT *
                    FROM recordatorios
                    WHERE uuid = ?
                """, (uuid,)).fetchone()

            self.responder(
                200,
                convertir_registro(fila)
            )

        except (ValueError, TypeError) as error:

            self.error(400, str(error))

        except Exception as error:

            print("ERROR DELETE:", error)

            self.error(500, "Error interno del servidor")


# ============================================
# 9. INICIAR SERVIDOR
# ============================================

def main():

    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--host",
        default="127.0.0.1"
    )

    parser.add_argument(
        "--port",
        type=int,
        default=8080
    )

    argumentos = parser.parse_args()

    inicializar_database()

    servidor = ThreadingHTTPServer(
        (
            argumentos.host,
            argumentos.port
        ),
        ServidorRecordatorios
    )

    print("=" * 50)

    print("SERVIDOR MISRECORDATORIOS")

    print(
        f"URL: http://{argumentos.host}:{argumentos.port}"
    )

    print(f"Database: {DATABASE}")

    print("Presiona Ctrl+C para detener")

    print("=" * 50)

    try:

        servidor.serve_forever()

    except KeyboardInterrupt:

        print("\nServidor detenido")

    finally:

        servidor.server_close()


if __name__ == "__main__":

    main()
 