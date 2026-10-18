# SONORA 2.0 — reproductor de música Android

Aplicación nativa Android en Java con interfaz oscura futurista y acentos cian/violeta.

## Funciones incluidas
- Importación de archivos de audio locales mediante el selector seguro de Android.
- Biblioteca con búsqueda por nombre de archivo.
- Reproducción, pausa, anterior, siguiente y barra de control compacta.
- Servicio de reproducción en primer plano con notificación y controles multimedia para la pantalla bloqueada.
- Favoritos persistentes.
- Lista guardada básica para agrupar pistas.
- Ecualizador del sistema mediante `android.media.audiofx.Equalizer` cuando el dispositivo lo admite.
- Reproducción aleatoria y repetición.

## Compilar en GitHub Actions
1. Sube el contenido de esta carpeta a la raíz de tu repositorio GitHub (incluida `.github`).
2. Abre Actions → Build SONORA APK → Run workflow.
3. Descarga el artefacto `SONORA-debug-APK` de la ejecución terminada.

## Compilar localmente
Requiere JDK 17, Android SDK API 35 y Gradle 8.7. Ejecuta `gradle assembleDebug` desde la raíz del proyecto.

## Notas
- No incluye radio online, tal como se pidió.
- El ecualizador depende de las capacidades del dispositivo y puede no estar disponible en todos los modelos.
- La gestión de metadatos y carátulas embebidas puede variar según los permisos y el formato del archivo.
- Este proyecto necesita compilarse en un entorno Android SDK para producir el APK; no se afirma que el APK haya sido compilado o probado en este entorno.
