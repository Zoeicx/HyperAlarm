Esta política explica cómo HyperAlarm (en adelante, «el módulo») trata su información. La conclusión, primero: el módulo no recopila ni carga ningún dato personal; sus registros y ajustes permanecen en este dispositivo. Los detalles se exponen a continuación.

## 0. Versión y fecha de entrada en vigor

- 0.1. Versión de la política: 1.0
- 0.2. Fecha de entrada en vigor: 15 de noviembre de 2026
- 0.3. Esta política se distribuye con cada compilación.

## 1. Información almacenada en este dispositivo

- 1.1. Registros de scripts: marca temporal, código de salida, salida estándar y error estándar de cada ejecución de script. Se guardan en la base de datos del módulo y en su directorio de almacenamiento externo.
- 1.2. Registros de la aplicación: el registro de ejecución del propio módulo, utilizado para diagnosticar problemas.
- 1.3. Registros de fallos: resúmenes breves de excepciones utilizados por el modo seguro, guardados en el directorio privado del módulo.
- 1.4. Ajustes: idioma, nivel de registro, filtros de lista y otros similares, guardados en el archivo de configuración del módulo.
- 1.5. Contraseña de bloqueo de pantalla: solo se escribe en un archivo si usted la introduce voluntariamente en los ajustes; el archivo tiene permisos 600 y solo root y este módulo pueden leerlo; se utiliza para desbloquear la pantalla antes de ejecutar una tarea silenciosa.
- 1.6. Nada de lo anterior sale de su dispositivo, salvo que usted mismo lo exporte, lo comparta o lo publique en una incidencia.

## 2. Información que no recopilamos

- 2.1. No recopilamos identificadores del dispositivo (IMEI, Android ID, número de serie, etc.).
- 2.2. No recopilamos datos de ubicación.
- 2.3. No recopilamos contactos, mensajes ni registros de llamadas.
- 2.4. No recopilamos el contenido de sus scripts.
- 2.5. El módulo no incluye SDK de analítica, de informes de fallos ni de publicidad.

## 3. Los dos accesos a la red

- 3.1. La página «Acerca de» solicita interfaces públicas de GitHub para leer el avatar del autor y la lista de colaboradores. Las solicitudes se envían únicamente a api.github.com y avatars.githubusercontent.com, incluyen el User-Agent estándar exigido por GitHub y no contienen ningún identificador del dispositivo ni del usuario. Por ello, GitHub verá su dirección IP.
- 3.2. El asistente de configuración inicial y la página de ajustes descargan de GitHub el texto del acuerdo de usuario y de esta política para que los documentos puedan actualizarse con independencia de las actualizaciones de versión. Estas solicitudes también incluyen solo cabeceras estándar. Si la descarga falla, se mostrará la copia incluida en el paquete de instalación o un aviso de error.
- 3.3. Aparte de lo anterior, el módulo no realiza ninguna solicitud de red.

## 4. Comunicación a terceros

- 4.1. No compartimos información con terceros porque no recopilamos ninguna.
- 4.2. Las solicitudes a GitHub mencionadas son una interacción directa suya con GitHub y están sujetas a la política de privacidad de GitHub.
- 4.3. Cuando usted exporta y comparte registros, el destinatario lo elige usted; esto no constituye una comunicación por nuestra parte.

## 5. Conservación y eliminación

- 5.1. Los registros se recortan automáticamente hasta un límite fijo, eliminando primero los más antiguos.
- 5.2. Puede borrar los registros desde la aplicación o eliminar directamente los datos del módulo.
- 5.3. Desinstalar el módulo elimina todos los datos de sus directorios privados.
- 5.4. Las copias de los registros en el almacenamiento externo debe eliminarlas usted manualmente antes de desinstalar el módulo.

## 6. Sus derechos

- 6.1. Puede consultar, exportar y borrar en cualquier momento toda la información guardada por el módulo.
- 6.2. Puede rechazar cualquier permiso; la función correspondiente dejará de estar disponible.
- 6.3. Puede borrar la contraseña de bloqueo de pantalla en los ajustes; el contenido del archivo se vaciará.

## 7. Menores de edad

- 7.1. El módulo está destinado a usuarios con ciertos conocimientos del sistema. Los menores de edad deben utilizarlo bajo la supervisión de un tutor.
- 7.2. Dado que implica root y ejecución de scripts, no recomendamos que los menores lo utilicen de forma independiente.

## 8. Cambios en esta política

- 8.1. Esta política puede revisarse con las actualizaciones de versión; el texto revisado se distribuye con la nueva compilación y se publica en el repositorio.
- 8.2. Si una revisión introduce nuevas formas de tratar la información, lo indicaremos expresamente en las notas de la versión.

## 9. Contacto

- 9.1. Para cuestiones de privacidad, abra una incidencia en el repositorio del proyecto.
- 9.2. El código fuente del módulo es público y auditable; puede verificar por su cuenta las afirmaciones anteriores.
