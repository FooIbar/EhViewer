package com.hippo.ehviewer.ui.i18n

object EsTranslations : Translations by EnTranslations {
    override val homepage = "Página de inicio"

    override val subscription = "Suscripción"

    override val whatsHot = "Destacado"

    override val favourite = "Favoritos"

    override val history = "Historial"

    override val downloads = "Descargas"

    override val settings = "Configuración"

    override val username = "Usuario"

    override val password = "Contraseña"

    override val signIn = "Iniciar sesión"

    override val register = "Registrarse"

    override val signInViaWebview = "Iniciar sesión vía web"

    override val signInFirst = "Por favor, inicie sesión primero"

    override val textIsEmpty = "El texto está vacío"

    override val waring = "Advertencia"

    override val invalidDownloadLocation = "Parece que la ubicación de descarga no está disponible. Establece la ubicación en Configuración."

    override val errorTimeout = "Sin tiempo"

    override val errorUnknownHost = "Host desconocido"

    override val errorRedirection = "Demasiadas redirecciones"

    override val errorSocket = "Error de Red"

    override val errorUnknown = "Raro"

    override val errorCantFindActivity = "No se encuentra la aplicación"

    override val errorCannotParseTheUrl = "No se puede analizar el URL"

    override val errorDecodingFailed = "Decodificación fallida"

    override val errorReadingFailed = "Lectura Fallida"

    override val errorOutOfRange = "Fuera de rango"

    override val errorParseError = "Análisis erróneo"

    override val error509 = "509"

    override val errorInvalidUrl = "Url invalida"

    override val errorGetPtokenError = "Error al obtener token"

    override val errorCantSaveImage = "Imposible guardar la imagen"

    override val errorInvalidNumber = "Número invalido"

    override val appWaring = "El contenido de esta aplicación es de Internet. Algunas de ellas pueden causarle daño psicológico. Ahora sabes los riesgos anteriormente mencionado y te gustaría asumirlos"

    override val errorUsernameCannotEmpty = "El usuario no puede estar vacío"

    override val guestMode = "Saltar inicio de sesión"

    override val signInFailed = "Inicio de sesión fallido"

    override val getIt = "Entendido"

    override val galleryListSearchBarHintExhentai = "Buscar ExHentai"

    override val galleryListSearchBarHintEHentai = "Buscar E-Hentai"

    override val galleryListEmptyHit = "El mundo es grande y el panda está sentado solo"

    override val keywordSearch = "Búsqueda por palabras clave"

    override val imageSearch = "Búsqueda de imagen"

    override val searchImage = "Búsqueda de imágenes"

    override val searchSh = "Mostrar galerías borradas"

    override val searchSto = "Mostrar solo galerías con torrents"

    override val searchSr = "Valoración mínima"

    override val selectImage = "Seleccionar imagen"

    override val selectImageFirst = "Seleccione la imagen primero"

    override val searchSpTo = "hasta"

    override val searchSf = "Deshabilitar filtros para:"

    override val searchSfl = "Lenguaje"

    override val searchSfu = "Uploader"

    override val searchSft = "Etiqueta"

    override val quickSearch = "Búsqueda rápida"

    override val quickSearchTip = "Pulsar \"+\" para añadir a la búsqueda rápida"

    override val addQuickSearchDialogTitle = "Añadir búsqueda rápida"

    override val nameIsEmpty = "Nombre vacío"

    override val addQuickSearchTip = "El estado de la lista de galerías se guardará como búsqueda rápida. Aplique la búsqueda primero si desea guardar el estado del panel de búsqueda."

    override val readme = "LÉEME"

    override val imageSearchNotQuickSearch = "No se puede añadir la búsqueda de imágenes como búsqueda rápida"

    override val duplicateQuickSearch = { p0: String ->
        "Búsqueda rápida duplicada. El nombre es \"%s\"."
            .format(p0)
    }

    override val duplicateName = "Nombre duplicado existente"

    override val goToHint = { p0: Int, p1: Int ->
        "Página %d, total %d páginas"
            .format(p0, p1)
    }

    override val star2 = "2 estrellas"

    override val star3 = "3 estrellas"

    override val star4 = "4 estrellas"

    override val star5 = "5 estrellas"

    override val download = "Descargar"

    override val read = "Leer"

    override val favoredTimes = { p0: Int ->
        "\u2665 %d"
            .format(p0)
    }

    override val ratingText = { p0: String, p1: Int ->
        "%s (%.2f, %d)"
            .format(p0, p1)
    }

    override val torrentCount = { p0: Int ->
        "Torrent (%d)"
            .format(p0)
    }

    override val share = "Compartir"

    override val rate = "Valorar"

    override val similarGallery = "Similar"

    override val searchCover = "Buscar portada"

    override val noTags = "Sin etiquetas"

    override val noComments = "Sin comentarios"

    override val noMoreComments = "No hay más comentarios"

    override val moreComment = "Más comentarios"

    override val refresh = "Actualizar"

    override val openInOtherApp = "Abrir en otra aplicación"

    override val rateSuccessfully = "Valoración completada"

    override val rateFailed = "Valoración fallida"

    override val noTorrents = "Sin torrents"

    override val torrents = "Torrents"

    override val notFavorited = "No en favoritos"

    override val addFavoritesDialogTitle = "Añadir a favoritos - Elige categoría"

    override val removeFromFavoriteSuccess = "Eliminado de favoritos"

    override val addToFavoriteFailure = "No se ha podido agregar a favoritos"

    override val removeFromFavoriteFailure = "Error al borrar de favoritos"

    override val rating10 = "OBRA MAESTRA"

    override val rating9 = "ASOMBROSO"

    override val rating8 = "GRANDIOSO"

    override val rating7 = "BUENO"

    override val rating6 = "DECENTE"

    override val rating5 = "MEDIOCRE"

    override val rating4 = "MALO"

    override val rating3 = "HORRIBLE"

    override val rating2 = "DOLOROSO"

    override val rating1 = "INSORPOTABLE"

    override val rating0 = "DESASTRE"

    override val galleryInfo = "Información de la galería"

    override val copiedToClipboard = "Copiado al portapapeles"

    override val keyGid = "Guía"

    override val keyToken = "Clave"

    override val keyUrl = "URL"

    override val keyTitle = "Título"

    override val keyTitleJpn = "Título en japonés"

    override val keyThumb = "Miniatura"

    override val keyCategory = "Categoría"

    override val keyUploader = "Uploader"

    override val keyPosted = "Publicado"

    override val keyParent = "Padre"

    override val keyVisible = "Visible"

    override val keyLanguage = "Idioma"

    override val keyPages = "Páginas"

    override val keySize = "Tamaño"

    override val keyFavoriteCount = "Número de favoritos"

    override val keyFavorited = "Favorito"

    override val keyRatingCount = "Número de valoraciones"

    override val keyRating = "Valoración"

    override val keyTorrents = "Torrents"

    override val keyTorrentUrl = "Url de torrent"

    override val galleryComments = "Comentarios de la galería"

    override val commentSuccessfully = "Comentario publicado"

    override val copyCommentText = "Copiar texto de comentario"

    override val voteUp = "Votar positivo"

    override val cancelVoteUp = "Cancelar voto"

    override val voteDown = "Votar negativo"

    override val voteUpSuccessfully = "Voto positivo completado"

    override val cancelVoteUpSuccessfully = "Voto cancelado"

    override val voteDownSuccessfully = "Voto negativo completado"

    override val cancelVoteDownSuccessfully = "Voto cancelado"

    override val voteFailed = "Voto fallido"

    override val checkVoteStatus = "Verificar estado del voto"

    override val goTo = "Ir a"

    override val sceneDownloadTitle = { p0: String ->
        "Descargas - %s"
            .format(p0)
    }

    override val noDownloadInfo = "Las galerías descargadas se mostrarán acá"

    override val downloadStateWait = "Esperando"

    override val downloadStateDownloading = "Descargando"

    override val downloadStateDownloaded = "Descargado"

    override val downloadStateFailed2 = { p0: Int ->
        "%d incompleto"
            .format(p0)
    }

    override val downloadStateFinish = "Completado"

    override val stat509AlertTitle = "Alerta 509"

    override val stat509AlertText = "Se alcanzó el límite de imágenes. Detenga la descarga y espere un poco"

    override val statDownloadDoneTitle = "Descarga completada"

    override val statDownloadDoneTextSucceeded = { p0: Int ->
        "%d completada"
            .format(p0)
    }

    override val statDownloadDoneTextFailed = { p0: Int ->
        "%d fallido"
            .format(p0)
    }

    override val statDownloadDoneTextMix = { p0: Int, p1: Int ->
        "%d completado, %d fallido"
            .format(p0, p1)
    }

    override val statDownloadDoneLineSucceeded = { p0: String ->
        "Completado: %s"
            .format(p0)
    }

    override val statDownloadDoneLineFailed = { p0: String ->
        "Fallido: %s"
            .format(p0)
    }

    override val downloadRemoveDialogTitle = "Borrar galería descargada"

    override val downloadRemoveDialogMessage = { p0: String ->
        "¿Borrar %s de la lista de descargas?"
            .format(p0)
    }

    override val downloadRemoveDialogMessage2 = { p0: Int ->
        "¿Borrar %d galerías de la lista de descargas?"
            .format(p0)
    }

    override val downloadRemoveDialogCheckText = "Borrar imágenes"

    override val downloadResetReadingProgress = "Reiniciar progreso de lectura"

    override val resetReadingProgressMessage = "¿Deseas reiniciar el progreso de lectura en todas las galerías?"

    override val statDownloadActionStopAll = "Detener todo"

    override val defaultDownloadLabelName = "Predeterminado"

    override val downloadMoveDialogTitle = "Mover"

    override val downloadLabels = "Descargar etiquetas"

    override val downloadStartAll = "Iniciar todo"

    override val downloadStopAll = "Detener todo"

    override val downloadServiceLabel = "Servicio de descarga EhViewer"

    override val downloadSpeedText = { p0: String ->
        "%s"
            .format(p0)
    }

    override val downloadSpeedText2 = { p0: String, p1: String ->
        "%s, %s restantes"
            .format(p0, p1)
    }

    override val rememberDownloadLabel = "Recordar etiqueta de descarga"

    override val defaultDownloadLabel = "Etiqueta predeterminada de descarga"

    override val addedToDownloadList = "Añadido a la lista de descargas"

    override val add = "Añadir"

    override val newLabelTitle = "Nueva etiqueta"

    override val labelTextIsEmpty = "Nombre de la etiqueta vacía"

    override val labelTextIsInvalid = "\"Defecto\" es una etiqueta invalida"

    override val labelTextExist = "Etiqueta existente"

    override val renameLabelTitle = "Renombrar etiqueta"

    override val noHistory = "Las galerías visualizadas se muestran aquí"

    override val clearAllHistory = "¿Limpiar historial?"

    override val shareImage = "Compartir imagen"

    override val imageSaved = { p0: String ->
        "Imagen guardada en %s"
            .format(p0)
    }

    override val settingsEh = "EH"

    override val settingsEhSignOut = "Cerrar sesión"

    override val settingsUConfig = "Configuración de EHentai"

    override val settingsUConfigSummary = "Configurar en la página de EHentai"

    override val settingsMyTags = "Mis etiquetas"

    override val settingsMyTagsSummary = "Ver página de etiquetas en EHentai"

    override val settingsEhLaunchPage = "Página al iniciar aplicación"

    override val settingsEhGallerySite = "Seleccionar galería"

    override val settingsEhListMode = "Modo lista"

    override val settingsEhListModeDetail = "Con detalles"

    override val settingsEhListModeThumb = "Solo portada"

    override val settingsEhDetailSize = "Tamaño del detalle"

    override val settingsEhDetailSizeLong = "Largo"

    override val settingsEhDetailSizeShort = "Corto"

    override val settingsEhShowJpnTitle = "Mostrar títulos en japonés"

    override val settingsEhShowJpnTitleSummary = "Es necesario habilitar los títulos en japonés en la propia página de EHentai"

    override val settingsEhShowGalleryPages = "Mostrar número de páginas de la galería"

    override val settingsEhShowGalleryPagesSummary = "Se verá cuántas páginas tiene una galería"

    override val settingsEhFilter = "Filtrado de galerías"

    override val settingsEhFilterSummary = "Filtrado de galerías por título, uploader y etiquetas. Para más detalles presiona el ícono (i) en la parte superior del menú"

    override val settingsDownload = "Descargas"

    override val settingsDownloadDownloadLocation = "Ruta de descargas"

    override val settingsDownloadCantGetDownloadLocation = "No se puede obtener ruta de descargas"

    override val settingsDownloadMediaScan = "Permitir escaneo de medios"

    override val settingsDownloadMediaScanSummaryOn = "Las galerías de imágenes mostrarán los doujinshis/mangas descargados. ¡Cuidado con prestar tu móvil o tablet!"

    override val settingsDownloadMediaScanSummaryOff = "Las de galería de imágenes no mostrarán los doujinshis/mangas descargados"

    override val settingsDownloadConcurrency = "Multi-hilos al descargar"

    override val settingsDownloadConcurrencySummary = { p0: String ->
        "Máximo de %s imágenes"
            .format(p0)
    }

    override val settingsDownloadPreloadImage = "Precargar imagen"

    override val settingsDownloadPreloadImageSummary = { p0: String ->
        "Precargar las siguientes %s imágenes"
            .format(p0)
    }

    override val settingsDownloadDownloadOriginImage = "Descargar imagen original"

    override val settingsDownloadDownloadOriginImageSummary = "¡Cuidado! Podrías recibir error 509"

    override val settingsDownloadRestoreDownloadItems = "Restaurar los elementos de descarga"

    override val settingsDownloadRestoreDownloadItemsSummary = "Restaurar todos los elementos de descarga en la ruta de descarga"

    override val settingsDownloadRestoreNotFound = "No se han encontrado elementos para restaurar"

    override val settingsDownloadRestoreFailed = "Restauración fallida"

    override val settingsDownloadRestoreSuccessfully = { p0: Int ->
        "Restauración de %d elementos completados"
            .format(p0)
    }

    override val settingsDownloadCleanRedundancy = "Borrar imágenes sobrantes"

    override val settingsDownloadCleanRedundancySummary = "Elimina las imágenes almacenadas en la ruta de descarga que no se encuentran en la lista de descarga"

    override val settingsDownloadCleanRedundancyNoRedundancy = "Sin imágenes para borrar"

    override val settingsDownloadCleanRedundancyDone = { p0: Int ->
        "Borrado de imágenes completada, un total de %d elementos borrados"
            .format(p0)
    }

    override val settingsAdvanced = "Avanzado"

    override val settingsAdvancedSaveParseErrorBody = "Guardar contenido en HTML al analizar el error"

    override val settingsAdvancedSaveParseErrorBodySummary = "El contenido HTML puede ser sensible a la privacidad"

    override val settingsAdvancedDumpLogcat = "Guardado de logcat"

    override val settingsAdvancedDumpLogcatSummary = "Guardar logcat en almacenamiento externo"

    override val settingsAdvancedDumpLogcatFailed = "Guardado de logcat fallido"

    override val settingsAdvancedDumpLogcatTo = { p0: String ->
        "Logcat guardado en %s"
            .format(p0)
    }

    override val settingsAdvancedReadCacheSize = "Tamaño del caché (archivos temporales)"

    override val settingsAdvancedAppLanguageTitle = "Idioma de la aplicación"

    override val settingsAdvancedExportData = "Exportar datos"

    override val settingsAdvancedExportDataSummary = "Guardar datos en almacenamiento externo, tales como la lista de descarga, lista de búsqueda rápida, entre otros"

    override val settingsAdvancedExportDataTo = { p0: String ->
        "Datos exportados en %s"
            .format(p0)
    }

    override val settingsAdvancedExportDataFailed = "Exportación de datos fallido"

    override val settingsAdvancedImportData = "Importar datos"

    override val settingsAdvancedImportDataSummary = "Cargar datos que han sido guardado antes"

    override val settingsAdvancedImportDataSuccessfully = "Importación de datos completo"

    override val settingsAbout = "Acerca de"

    override val settingsAboutAuthor = "Autor"

    override val settingsAboutSource = "Código fuente"

    override val settingsAboutVersion = "Versión de compilación"

    override val settingsAboutCheckForUpdates = "Verificar actualizaciones"

    override val cantReadTheFile = "No se puede leer el archivo"

    override val appLanguageSystem = "Lenguaje del sistema (Defecto)"

    override val pleaseWait = "Espere"

    override val cloudFavorites = "Favoritos en la nube"

    override val localFavorites = "Favoritos locales"

    override val searchBarHint = { p0: String ->
        "Buscar %s"
            .format(p0)
    }

    override val favoritesTitle = { p0: String ->
        "%s"
            .format(p0)
    }

    override val favoritesTitle2 = { p0: String, p1: String ->
        "%s - %s"
            .format(p0, p1)
    }

    override val deleteFavoritesDialogTitle = "Eliminar de favoritos"

    override val deleteFavoritesDialogMessage = { p0: Int ->
        "¿Eliminar %d galerías de favoritos?"
            .format(p0)
    }

    override val moveFavoritesDialogTitle = "Mover favoritos"

    override val defaultFavoritesCollection = "Colección de favoritos por defecto"

    override val letMeSelect = "Déjame elegir"

    override val collections = "Colección"

    override val fromTheFuture = "Desde el futuro"

    override val justNow = "Ahora"

    override val yesterday = "Ayer"

    override val someDaysAgo = { p0: Int ->
        "Hace %d días atrás"
            .format(p0)
    }

    override val archive = "Archivo"

    override val noArchives = "Sin archivos"

    override val downloadArchiveStarted = "Descarga de archivos iniciada"

    override val downloadArchiveFailure = "Error al descargar archivo"

    override val downloadArchiveFailureNoHath = "Necesita un cliente H@H para descargar archivo"

    override val addToFavoriteSuccess = "Añadido a favoritos"

    override val addToFavourites = "Agregar a favoritos"

    override val cancelVoteDown = "Cancelar voto negativo"

    override val clearAll = "Limpiar todo"

    override val commentFailed = "Comentario fallido"

    override val delete = "Eliminar"

    override val downloadStateFailed = "Descarga fallida"

    override val downloadStateNone = "Descarga en pausa"

    override val errorPasswordCannotEmpty = "La contraseña no puede estar vacía"

    override val errorSomethingWrongHappened = "Ocurrió algo malo"

    override val settingsPrivacy = "Privacidad"

    override val filter = "Bloqueo de galerías"

    override val filterTitle = "Título"

    override val filterUploader = "Uploader"

    override val filterTag = "Etiqueta"

    override val filterTagNamespace = "Etiqueta namespace"

    override val deleteFilter = { p0: String ->
        "¿Borrar \"%s\"?"
            .format(p0)
    }

    override val addFilter = "Bloquear etiqueta"

    override val filterTheTag = { p0: String ->
        "¿Deseas bloquear la etiqueta \"%s\"?"
            .format(p0)
    }

    override val showDefinition = "Ver definición de la etiqueta"

    override val filterAdded = "Etiqueta bloqueada"

    override val filterText = "Escribe la etiqueta"

    override val filterTip = "Este sistema de bloqueo filtrará las galerías de EHentai que contengan la etiqueta especificada acá.\n\nBloqueo de título: excluye las galerías cuyo título contenga la palabra.\n\nBloqueo de uploader: Excluye las galerías que publicadas por el uploader.\n\nBloqueo de etiqueta: excluye galerías que contengan la etiqueta, tomará más tiempo en obtener la lista de galerías.\n\nBloqueo de etiqueta namespace: excluye galerías que contengan la etiqueta namespace, tomará más tiempo en obtener la lista de galerías. Nota: Con namespace quiere decir etiquetas como \"male\", \"female\", \"misc\", entre otros."

    override val settingsPrivacySecure = "Impedir capturas de pantalla"

    override val settingsPrivacySecureSummary = "Si habilitas esta opción no podrás hacer captura de pantalla y el sistema no mostrará una previsualización en el multitareas.\n\nDeberás reiniciar la aplicación para activar esta función."

    override val uConfig = "Ajustes EHentai"

    override val applyTip = "Toca la marca de verificación para guardar los cambios"

    override val darkThemeFollowSystem = "Sigue el sistema"

    override val pageCount = { quantity: Int ->
        when (quantity) {
            1 -> "%d páginas"
            else -> "%d páginas"
        }.format(quantity)
    }

    override val someMinutesAgo = { quantity: Int ->
        when (quantity) {
            1 -> "Hace un minuto"
            else -> "Hace %d minutos"
        }.format(quantity)
    }

    override val someHoursAgo = { quantity: Int ->
        when (quantity) {
            1 -> "Hace una hora"
            else -> "Hace %d horas"
        }.format(quantity)
    }

    override val second = { quantity: Int ->
        when (quantity) {
            1 -> "segundo"
            else -> "segundos"
        }.format(quantity)
    }

    override val minute = { quantity: Int ->
        when (quantity) {
            1 -> "minuto"
            else -> "minutos"
        }.format(quantity)
    }

    override val hour = { quantity: Int ->
        when (quantity) {
            1 -> "hora"
            else -> "horas"
        }.format(quantity)
    }

    override val day = { quantity: Int ->
        when (quantity) {
            1 -> "día"
            else -> "días"
        }.format(quantity)
    }

    override val year = { quantity: Int ->
        when (quantity) {
            1 -> "año"
            else -> "años"
        }.format(quantity)
    }
}