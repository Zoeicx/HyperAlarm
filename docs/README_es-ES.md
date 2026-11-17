<div align="center">
<img src="/imgs/icon.webp" width="160" height="160" style="display: block; margin: 0 auto;" alt="icon" />

# HyperAlarm

[简体中文](../README.md) | [English](README_en-US.md) | [繁體中文](README_zh-TW.md) | [繁體中文（香港）](README_zh-HK.md) | [梗体中文](README_zh-ME.md) | [日本語](README_ja-JP.md) | [Русский](README_ru-RU.md) | [Українська](README_uk-UA.md) | [Português (Brasil)](README_pt-BR.md) | [Deutsch](README_de-DE.md) | **Español** | [Italiano](README_it-IT.md) | [Polski](README_pl-PL.md) | [Türkçe](README_tr-TR.md) | [Tiếng Việt](README_vi-VN.md) | [العربية](README_ar-SA.md) | [Bahasa Indonesia](README_in-ID.md)

</div>

---

HyperAlarm es un módulo Xposed cuyo **único ámbito es el Reloj de Xiaomi**. Convierte las alarmas del reloj en disparadores de scripts de shell.

## App anfitriona

Solo están verificadas las versiones de abajo. Con cualquier otra, pruébalo tú, envía un APK para adaptarlo o actualiza/vuelve a estas:

- Reloj de Xiaomi `com.android.deskclock`
- Versión `18.28.0`, código de versión `1306002800`

## Instalación

1. Ten root (KernelSU / Magisk) y LSPosed (Xposed API 101 o superior).
2. Instala el módulo y activa HyperAlarm en LSPosed.
3. Marca como ámbito solo `com.android.deskclock`. No añadas otros paquetes.
4. Fuerza la detención del Reloj y deja que su proceso arranque de nuevo, o el módulo no hará nada.

## Uso

- Nueva tarea: el `+` a la derecha de la barra de acciones superior.
- La etiqueta de la alarma es el nombre del script y debe acabar en `.sh`:
  - `alert.sh`: se ejecuta en silencio a la hora, sin sonido ni pantalla de alarma
  - `@alert.sh`: ejecuta el script y mantiene la pantalla de alarma y el tono
- La `@` se oculta en la lista; con el interruptor «Tono» pasas de un modo a otro.
- «Ejecutar ahora» permite probar un script sin esperar a la hora.
- Las tareas por turnos se disparan según el turno calculado desde la fecha de inicio y el ciclo.

## Reglas de los scripts

Los scripts viven en `/data/adb/alarm_shell/scripts/` y el nombre de la etiqueta debe coincidir con el del archivo.

```text
/data/adb/alarm_shell/scripts/      # directorio de scripts
/data/adb/alarm_shell/password      # contraseña de la pantalla de bloqueo
/data/adb/alarm_shell/unlock_phone  # script de desbloqueo por defecto
```

- Los saltos de línea deben ser LF. Un archivo con CRLF de Windows simplemente falla.
- Los scripts se ejecutan como root dentro del espacio de nombres principal del sistema, así que ven los directorios de datos de otras apps.
- Cada ejecución deja un registro: se ve en «Registros de scripts» dentro de la app y además se copia a
  `/sdcard/Android/data/com.hyperalarm/files/script_logs/<nombre del script>/`.
- Antes de una tarea silenciosa, el módulo desbloquea la pantalla con tu contraseña si la has definido.

## Ajustes

- **El reloj solo muestra alarmas**: en la lista del Reloj quedan las alarmas reales y se ocultan las tareas de script.
- **Ocultar tareas con sonido**: oculta también las tareas con tono activado.
- Nivel de registro / Registros de la app / Registros de scripts: sube el nivel antes de investigar un fallo.
- **Contraseña de la pantalla de bloqueo**: sirve para desbloquear antes de las tareas silenciosas; vacío significa no cambiarla.
- Tutorial de reinicio, copiar el directorio de scripts, copia de seguridad y restauración de la configuración.

## Limitaciones conocidas

- Verificado solo en Android 16 / HyperOS 3 / Reloj 18.28.0; otras combinaciones corren por tu cuenta.
- El modo por turnos depende de las tripas del Reloj y hay que volver a verificarlo tras actualizarlo.
- Sin root el módulo no funciona; la página de ajustes lo indica.
- Los registros de scripts se copian al almacenamiento externo y los puede leer root y adb shell. No escribas secretos en la salida de un script.

## Preguntas frecuentes

- **He creado una tarea y el reloj no reacciona**: comprueba que el módulo está activo en LSPosed, que el ámbito es solo el Reloj y que reiniciaste el proceso del reloj.
- **La tarea no aparece en la lista del reloj**: es «El reloj solo muestra alarmas» haciendo su trabajo; desactívalo.
- **Llegó la hora y no se ejecutó nada**: mira los registros de scripts de la app; un script ausente o una etiqueta mal escrita se avisan ahí.

## Licencia

- Código propio de este repositorio: **AGPL-3.0** (ver `../LICENSE`)
- Componentes de terceros y sus licencias: [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md)

## Agradecimientos

- [HyperCeiler](https://github.com/ReChronoRain/HyperCeiler): el punto de partida del proyecto; buena parte de la interfaz MIUIX y de los ajustes viene de ahí
- [MIUIX](https://github.com/miuix-kotlin-multiplatform/miuix): componentes de interfaz al estilo Xiaomi
- [libsu](https://github.com/topjohnwu/libsu), [libxposed](https://github.com/libxposed/api): APIs de root y Xposed
- Todas las personas que abrieron issues y enviaron registros
