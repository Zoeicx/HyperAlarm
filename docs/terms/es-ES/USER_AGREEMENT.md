HyperAlarm es un módulo no comercial de mejora del sistema que se ejecuta sobre Xposed y está destinado a Mi Clock. Convierte las alarmas de Mi Clock en activadores de scripts root: cuando suena una alarma, se puede ejecutar un script proporcionado por usted en lugar del sonido, o junto con él. Lea y comprenda este Acuerdo de usuario de HyperAlarm antes de continuar usando el módulo.

## 0. Versión y fecha de entrada en vigor

- 0.1. Versión del acuerdo: 1.0
- 0.2. Fecha de entrada en vigor: 15 de noviembre de 2026
- 0.3. Este acuerdo se distribuye con cada compilación; la versión más reciente está en el repositorio del proyecto.

## 1. Disposiciones generales

- 1.1. HyperAlarm es desarrollado y mantenido por el Proyecto HyperAlarm (en adelante, «nosotros»).
- 1.2. El módulo requiere un dispositivo con permisos root y el framework Xposed / LSPosed instalado; si falta cualquiera de las dos condiciones, no funcionará.
- 1.3. El único ámbito de este módulo es Mi Clock (com.android.deskclock). No inyectamos hooks en otras aplicaciones.
- 1.4. Al usar este módulo, usted reconoce que hace que las alarmas ejecuten un script proporcionado por usted a la hora configurada, en lugar de limitarse a sonar.
- 1.5. Puede dejar de usar el módulo en cualquier momento desinstalándolo o desactivándolo en el gestor de Xposed.

## 2. Alcance de la licencia

- 2.1. Le concedemos una licencia personal, no transferible, no sublicenciable y no exclusiva para instalar y usar el módulo.
- 2.2. El código fuente se publica bajo la GNU Affero General Public License v3.0 (AGPL-3.0). Puede copiarlo, modificarlo y redistribuirlo conforme a los términos de esa licencia.
- 2.3. La preinstalación comercial, la venta conjunta o la distribución con un dispositivo requieren nuestra autorización previa por escrito.
- 2.4. Las partes heredadas de HyperCeiler siguen sujetas a su licencia original; consulte THIRD_PARTY_NOTICES.md en el repositorio.
- 2.5. El nombre y el icono de HyperAlarm no están incluidos en la licencia del código fuente.

## 3. Capacidades y su responsabilidad

- 3.1. Los scripts se ejecutan como root en el espacio de nombres de montaje principal del sistema, por lo que pueden ver los directorios de datos de otras aplicaciones.
- 3.2. Usted proporciona, instala y mantiene los scripts. No somos responsables del comportamiento de los scripts ni de ninguna consecuencia derivada de su ejecución.
- 3.3. Una tarea silenciosa suprime la interfaz de la alarma; si el script falta, es rechazado o la alarma no se puede reprogramar, el módulo mantiene el sonido normal de la alarma.
- 3.4. Este módulo no es un medio de recordatorio fiable. No lo utilice en situaciones en las que una alarma que no suene pueda causar daños personales o pérdidas materiales importantes, como el ámbito médico, la seguridad contra incendios o los servicios de guardia.
- 3.5. Modificar el comportamiento del sistema conlleva riesgos, incluidos, entre otros, que el dispositivo no arranque, la pérdida de datos o la invalidación de la garantía.

## 4. Tratamiento de la información

- 4.1. Los registros de scripts, los registros de la aplicación, los registros de fallos, los ajustes y la contraseña de bloqueo de pantalla permanecen únicamente en su dispositivo. No los cargamos, recopilamos ni vendemos.
- 4.2. El módulo accede a la red en exactamente dos lugares: la página «Acerca de» consulta interfaces públicas de GitHub para obtener el avatar del autor y la lista de colaboradores, y el texto del acuerdo se descarga de GitHub. Ambos accesos solo revelan su dirección IP a GitHub y no contienen ningún identificador del dispositivo.
- 4.3. Los detalles figuran en la Política de privacidad de HyperAlarm publicada junto con este acuerdo.

## 5. Permisos que solicitamos

- 5.1. Root: ejecutar scripts, leer y escribir el directorio de scripts, sincronizar registros y modificar ajustes globales. Es necesario para las funciones principales.
- 5.2. Lista de aplicaciones instaladas: para mostrar aplicaciones en el selector de ámbito.
- 5.3. Notificaciones: para mostrar la ejecución de scripts en curso y sus resultados, así como los motivos de los fallos.
- 5.4. Red: solo en los dos casos descritos en el punto 4.2.
- 5.5. Puede rechazar cualquiera de ellos; la función correspondiente dejará de estar disponible, pero las demás no se verán afectadas.

## 6. Normas de uso

- 6.1. No debe eliminar la información sobre derechos de autor y licencias del módulo ni de sus copias.
- 6.2. No debe utilizar el módulo para perjudicar la seguridad de las redes, vulnerar la privacidad de terceros o incurrir en cualquier otro acto contrario a la ley.
- 6.3. No debe instalar ni usar el módulo en un dispositivo ajeno sin el consentimiento de su propietario.
- 6.4. No debe utilizar el módulo para eludir pagos, vulnerar la protección de programas informáticos o evadir controles de acceso.

## 7. Exención y limitación de responsabilidad

- 7.1. El módulo se proporciona «tal cual», sin garantías de ningún tipo, expresas o implícitas, incluidas, entre otras, las de comerciabilidad, idoneidad para un fin concreto y no infracción.
- 7.2. Solo hemos verificado su funcionamiento en Mi Clock 18.28.0. Tras una actualización del reloj o del sistema, algunas funciones pueden dejar de funcionar o cambiar de comportamiento.
- 7.3. Los problemas derivados de sus acciones, del contenido de sus scripts, de su entorno root, del framework Xposed o de una actualización del sistema son responsabilidad suya.
- 7.4. En la máxima medida permitida por la ley, no somos responsables de los daños indirectos, incidentales, especiales o punitivos derivados del uso o de la imposibilidad de uso de este módulo.
- 7.5. Si alguna cláusula se declara inválida, las demás seguirán vigentes.

## 8. Cambios y terminación

- 8.1. Podemos revisar este acuerdo con las actualizaciones de versión; el texto revisado se distribuye con la nueva compilación y se publica en el repositorio.
- 8.2. Si sigue usando el módulo después de que una revisión entre en vigor, se entenderá que la acepta; si no está de acuerdo, deje de usarlo y desinstálelo.
- 8.3. Puede poner fin a este acuerdo en cualquier momento desinstalando el módulo.

## 9. Legislación aplicable y resolución de conflictos

- 9.1. Este acuerdo se rige por las leyes de la República Popular China.
- 9.2. Las controversias derivadas de este acuerdo se resolverán primero mediante negociación de buena fe; si no se llega a un acuerdo, se someterán al tribunal popular competente de nuestra sede principal.

## 10. Contacto

- 10.1. Para cualquier cuestión relacionada con este acuerdo, abra una incidencia en el repositorio del proyecto.
- 10.2. Si considera que alguna cláusula no coincide con el comportamiento real, le rogamos que nos lo comunique para corregirla, en lugar de considerarla vinculante.
