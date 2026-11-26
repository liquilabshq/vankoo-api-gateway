# language: es
@NFR @NFR01 @seguridad @servicio_gateway
Característica: Acceso seguro a las APIs protegidas
  Como inversionista o empresario MYPE
  Quiero que solo yo pueda operar con mis datos y mi dinero
  Para que nadie acceda a mi cuenta ni invierta en mi nombre

  # Atributo de calidad 2.1.1: Seguridad. Medida: 0 accesos no autorizados.
  # El gateway valida el JWT emitido por IAM; cada servicio comprueba que X-User-Id sea el dueño.

  Esquema del escenario: Rechazar en el gateway una petición sin credenciales válidas
    Dado que un cliente llama a "<ruta protegida>"
    Y la petición tiene <credencial>
    Cuando el API Gateway recibe la petición
    Entonces responde con el código 401 y el problema "unauthenticated"
    Y la petición no llega a ningún microservicio

    Ejemplos:
      | ruta protegida                          | credencial                      |
      | /investment/api/v1/auctions/marketplace | ningún encabezado Authorization |
      | /invoicing/api/v1/invoices              | un token con la firma alterada  |
      | /finance/api/v1/accounts                | un token expirado               |

  Escenario: Reenviar una petición autenticada con la identidad del usuario
    Dado que el inversionista inició sesión en IAM y obtuvo un JWT válido
    Cuando consulta el marketplace a través del API Gateway
    Entonces el gateway reenvía la petición con estos encabezados:
      | encabezado   |
      | X-User-Id    |
      | X-User-Email |
      | X-User-Roles |
    Y la respuesta tiene el código 200

  Escenario: Impedir que un usuario invierta en nombre de otro
    Dado que el inversionista "inv-01" tiene un JWT válido
    Cuando intenta comprar una participación indicando el inversionista "inv-02"
    Entonces Investment responde con el código 403
    Y no se debita ninguna billetera

  Escenario: Mantener las rutas internas fuera del alcance público
    Cuando un cliente externo llama a "/investment/api/v1/internal/auctions/123/evaluation"
    Entonces el API Gateway responde con el código 404 porque la ruta no es pública
    Y la evaluación de riesgo de la subasta no cambia
