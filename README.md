# Write4 NFC
App Android nativa (Java), sin dependencias externas ni permiso de Internet. Interfaz en español.

## Funciones
- Leer registros NDEF: web, vCard y texto; mostrar capacidad y si permite escritura.
- Grabar contactos vCard 3.0 o enlaces HTTP/HTTPS. Validación, tamaño en bytes, confirmación de reemplazo y verificación de lectura posterior.
- Formatear etiquetas compatibles vacías. En ese caso, retirar y volver a acercar para verificar.
- Borrador privado local; sin sincronización. No solicita acceso a contactos.
- Cancelación de escritura al salir de la app. No bloquea permanentemente etiquetas.

## Compilar
Instalar Android SDK 36 y JDK 17 o superior. Configurar `local.properties` con `sdk.dir`.
```sh
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Uso
Abre la app, activa NFC y completa Contacto o Sitio web. Vista previa muestra el contenido y tamaño. Pulsa Grabar, confirma y acerca una etiqueta compatible. La grabación reemplaza todo su contenido NDEF. Para leer basta acercar la etiqueta con la app abierta. Los enlaces solo se abren después de confirmarlos.

Usa NTAG213/215/216 u otra etiqueta NDEF compatible. Los contactos grandes pueden necesitar NTAG215/216; la app comprueba la capacidad antes de escribir. Las tarjetas bancarias, de transporte o acceso no son tarjetas de contacto NDEF y no están soportadas. El teléfono requiere NFC físico para leer/grabar; el emulador solo permite validar la interfaz. La compatibilidad al abrir vCards depende del teléfono lector.

## Verificación manual con hardware
1. Sin NFC: abrir la app y comprobar aviso; con NFC apagado: ir a Ajustes y activarlo.
2. Grabar URL HTTPS en etiqueta regrabable, leer y abrir con confirmación.
3. Grabar contacto con acentos, empresa, teléfono y correo; leer desde otro teléfono.
4. Probar etiqueta de solo lectura y capacidad insuficiente: debe rechazar escritura.
5. Alejar etiqueta durante escritura: informar error; leer antes de reintentar.
6. Preparar grabación, salir y regresar: debe volver a lectura.
7. Formatear etiqueta vacía compatible, retirar y leer para verificar.

API de referencia: https://developer.android.com/reference/android/nfc/tech/Ndef
