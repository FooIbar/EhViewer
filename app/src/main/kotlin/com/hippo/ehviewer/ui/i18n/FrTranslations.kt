package com.hippo.ehviewer.ui.i18n

object FrTranslations : Translations by EnTranslations {
    override val homepage = "Page d'acceuil"
    override val subscription = "Tags suivis"
    override val whatsHot = "Plus vus"
    override val favourite = "Favoris"
    override val history = "Historique"
    override val downloads = "Téléchargement"
    override val settings = "Paramétres"
    override val username = "Nom d'utilisateur"
    override val password = "Mot de passe"
    override val signIn = "Se connecter"
    override val register = "Créer un compte"
    override val signInViaWebview = "Se connecter via WebView"
    override val signInFirst = "Connectez-vous pour continuer"
    override val textIsEmpty = "Entrez le texte."
    override val waring = "Attention"
    override val invalidDownloadLocation = "La location de téléchargement actuelle n'est pas disponible. Choissez-en une à nouveau dans les Paramétres."
    override val errorTimeout = "Time-out"
    override val errorUnknownHost = "Hôte inconnu"
    override val errorRedirection = "Trop de redirections"
    override val errorSocket = "Erreur de network"
    override val errorUnknown = "Erreur inconnue"
    override val errorCantFindActivity = "Impossible de trouver l'application"
    override val errorCannotParseTheUrl = "Impossible de parser le lien URL"
    override val errorDecodingFailed = "Échec de décodage"
    override val errorReadingFailed = "Échec de lecture"
    override val errorOutOfRange = "Hors de portée"
    override val errorParseError = "Échec d'analyse syntaxique"
    override val error509 = "509"
    override val errorInvalidUrl = "URL invalide"
    override val errorGetPtokenError = "Échec d'obtention de pToken"
    override val errorCantSaveImage = "Impossible d'enregistrer l'image"
    override val errorInvalidNumber = "Nombre invalide"
    override val appWaring = "Le contenu de l'application provient de l'Internet, dont quelque chose peut vous causer le dommage physique ou mentale. Vous l'avez bien compris et voulez continuer quand même."
    override val errorUsernameCannotEmpty = "Nom d'ultilisateur ne doit pas être une chaîne vide"
    override val errorPasswordCannotEmpty = "Mot de passe ne doit pas être une chaîne vide"
    override val guestMode = "Continuer sans se connecter"
    override val signInFailed = "Impossible de se connecter"
    override val getIt = "Compris"
    override val galleryListSearchBarHintExhentai = "Chercher sur ExHentai"
    override val galleryListSearchBarHintEHentai = "Chercher sur E-Hentai"
    override val galleryListSearchBarOpenGallery = "Ouvrir la galerie spécifiée"
    override val galleryListEmptyHit = "Le monde est grand et le panda s'assied tout seul."
    override val galleryListEmptyHitSubscription = "Vous pouvez suivre des tags à Paramètres -> EH -> Mes tags."
    override val keywordSearch = "Recherche du mot-clé"
    override val imageSearch = "Recherche d'une image"
    override val searchImage = "Recherche d'une image"
    override val searchSh = "Montrer les galeries supprimées"
    override val searchSto = "Seulement montrer galeries avec des fiches torrent"
    override val searchSr = "Minimum note"
    override val searchSpTo = "à"
    override val searchSf = "Désactiver le filtre par défaut pour les items suivants :"
    override val searchSfl = "Langue"
    override val searchSfu = "Uploader"
    override val searchSft = "Tag"
    override val selectImage = "Sélectionner une image"
    override val selectImageFirst = "Sélectionnez une image"
    override val addToFavourites = "Ajouter à favori"
    override val removeFromFavourites = "Retirer de favori"
    override val quickSearch = "Recherche rapide"
    override val quickSearchTip = "Appuyez sur \"+\" pour ajouter une Recherche rapide"
    override val addQuickSearchDialogTitle = "Ajouter la Recherche rapide"
    override val nameIsEmpty = "Entrez un nom"
    override val delete = "Supprimer"
    override val addQuickSearchTip = "Le resultat de la liste de galerie sera sauvegardé comme une Recherche rapide. Cherchez d'abord pour sauvegarder l'état du panneau recherche."
    override val readme = "README"
    override val imageSearchNotQuickSearch = "Impossible d'ajouter recherche d'une image comme une Recherche rapide."
    override val duplicateQuickSearch = { a: String -> "Une Recherche rapide avec le nom \"$a\" déjà existe." }
    override val duplicateName = "Ce nom déjà existe"
    override val goToHint = { a: Int, b: Int -> "Page $a, avec un totale de $a pages" }
    override val star2 = "2 étoiles"
    override val star3 = "3 étoiles"
    override val star4 = "4 étoiles"
    override val star5 = "5 étoiles"
    override val download = "Télécharger"
    override val read = "Lire"
    override val favoredTimes = { a: Int -> "\u2665 $a" }
    override val ratingText = { a: String, b: Float, c: Int -> "%s (%.2f, %d)".format(a, b, c) }
    override val torrentCount = { a: Int -> "Torrent ($a)" }
    override val share = "Partager"
    override val rate = "Noter"
    override val similarGallery = "Similaire(s)"
    override val searchCover = "Chercher le couverture"
    override val noTags = "Aucun tag"
    override val noComments = "Aucun commentaire"
    override val noMoreComments = "Pas d'autres commentaires"
    override val moreComment = "Voir plus de commentaires"
    override val refresh = "Rafraîchir"
    override val openInOtherApp = "Ouvrir avec une autre appli"
    override val rateSuccessfully = "Noté avec succès"
    override val rateFailed = "Échec de notation"
    override val noTorrents = "Aucun fiche torrent"
    override val torrents = "Fiches torrent"
    override val notFavorited = "Non ajoutée à favoris"
    override val addFavoritesDialogTitle = "Ajouter à favoris"
    override val addToFavoriteSuccess = "Ajouté à favoris"
    override val removeFromFavoriteSuccess = "Retirer de favoris"
    override val addToFavoriteFailure = "Impossible de l'ajouter à favoris"
    override val removeFromFavoriteFailure = "Impossible de la retirer de favoris"
    override val filterTheUploader = { a: String -> "Bloquer l'uploader \"$a\" ?" }
    override val filterTheTag = { a: String -> "Bloquer le tag \"$a\" ?" }
    override val filterAdded = "Bloqué(e)"
    override val rating10 = "CHEF-D'ŒUVRE"
    override val rating9 = "IRRÉSISTABLE"
    override val rating8 = "TRÈS BIEN"
    override val rating7 = "AGRÉABLE"
    override val rating6 = "PAS MAL"
    override val rating5 = "COMME SI COMME ÇA"
    override val rating4 = "MAL"
    override val rating3 = "TRÈS MAL"
    override val rating2 = "PEINE"
    override val rating1 = "INSUPPORTABLE"
    override val rating0 = "FUYEZ !"
    override val galleryInfo = "Infos de la galerie"
    override val copiedToClipboard = "Copié à presse-papiers"
    override val keyGid = "Gid"
    override val keyToken = "Token"
    override val keyUrl = "URL"
    override val keyTitle = "Titre"
    override val keyTitleJpn = "Titre en japonais"
    override val keyThumb = "Aperçu"
    override val keyCategory = "Catégorie"
    override val keyUploader = "Uploader"
    override val keyPosted = "Mise en ligne"
    override val keyParent = "Parent"
    override val keyVisible = "Visibile ?"
    override val keyLanguage = "Langue"
    override val keyPages = "Pages"
    override val keySize = "Taille"
    override val keyFavoriteCount = "Nombre d'additions à favoris"
    override val keyFavorited = "Ajouté à favoris"
    override val keyRatingCount = "Nombre de notations"
    override val keyRating = "Notation"
    override val keyTorrents = "Nombre de torrents"
    override val keyTorrentUrl = "URL du fiche torrent"
    override val galleryComments = "Commentaires de la galerie"
    override val commentSuccessfully = "Commentaire posté"
    override val commentFailed = "Impossible de commenter"
    override val copyCommentText = "Copier le texte du commentaire"
    override val voteUp = "Donner \"J'aime\""
    override val cancelVoteUp = "Annuler \"J'aime\""
    override val voteDown = "Donner \"Je hais\""
    override val cancelVoteDown = "Donner \"Je hais\""
    override val voteUpSuccessfully = "\"J'aime\" donné avec succès"
    override val cancelVoteUpSuccessfully = "\"J'aime\" retiré avec succès"
    override val voteDownSuccessfully = "\"Je hais\" donné avec succès"
    override val cancelVoteDownSuccessfully = "\"Je hais\" retiré avec succès"
    override val voteFailed = "Échec du vote"
    override val checkVoteStatus = "Voir les détails des votes"
    override val goTo = "Aller à"
    override val sceneDownloadTitle = { a: String -> "Téléchargement - $a" }
    override val noDownloadInfo = "Vos téléchargements s'afficheront ici"
    override val downloadStateNone = "En veille"
    override val downloadStateWait = "En attente"
    override val downloadStateDownloading = "Téléchargement en cours"
    override val downloadStateDownloaded = "Téléchargement completé"
    override val downloadStateFailed = "Échec"
    override val downloadStateFailed2 = { a: Int -> "$a non complet" }
    override val downloadStateFinish = "Complet"
    override val stat509AlertTitle = "Erreur 509"
    override val stat509AlertText = "Votre limite d'obtention d'images est atteinte. Arrêtez le téléchargement et réessayez plus tard."
    override val statDownloadDoneTitle = "Téléchargement completé"
    override val statDownloadDoneTextSucceeded = { a: Int -> "$a avec succés" }
    override val statDownloadDoneTextFailed = { a: Int -> "$a échoué(s)" }
    override val statDownloadDoneTextMix = { a: Int, b: Int -> "$a avec succés, $a échoué(s)" }
    override val statDownloadDoneLineSucceeded = { a: String -> "Complété(s) : $a" }
    override val statDownloadDoneLineFailed = { a: String -> "Échoué(s) : $a" }
    override val downloadRemoveDialogTitle = "Retire téléchargement"
    override val downloadRemoveDialogMessage = { a: String -> "Retirer $a de la liste de téléchargement ?" }
    override val downloadRemoveDialogMessage2 = { a: Int -> "Retirer $a articles de la liste de téléchargement ?" }
    override val downloadRemoveDialogCheckText = "Supprimer fiches d'image"
    override val statDownloadActionStopAll = "Arrêter tout"
    override val defaultDownloadLabelName = "Défaut"
    override val downloadMoveDialogTitle = "Déplacer"
    override val downloadLabels = "Labels de téléchargement"
    override val downloadStartAll = "Commencer tout"
    override val downloadStopAll = "Arrêter tout"
    override val downloadResetReadingProgress = "Réinitialiser les progrès de lecture"
    override val resetReadingProgressMessage = "Réinitialiser les progrès de lecture de toutes galéries téléchargées ?"
    override val downloadServiceLabel = "Service du téléchargement EhViewer"
    override val downloadSpeedText = { a: String -> a }
    override val downloadSpeedText2 = { a: String, b: String -> "$a, encore $b" }
    override val rememberDownloadLabel = "Enregistrer le label de téléchargement"
    override val defaultDownloadLabel = "Label de téléchargement par défaut"
    override val addedToDownloadList = "Ajouté à la liste de téléchargement"
    override val add = "Ajouter"
    override val newLabelTitle = "Nouveau label"
    override val labelTextIsEmpty = "Entrez le texte du label"
    override val labelTextIsInvalid = "\"Default\" n'est pas un label valide"
    override val labelTextExist = "Ce label déjà existe"
    override val renameLabelTitle = "Renommer le label"
    override val noHistory = "Votre historique de les lectures de galerie s'affichera ici"
    override val clearAll = "Tout effacer"
    override val clearAllHistory = "Effacer toute l'historique ?"
    override val filter = "Bloqueur des galeries"
    override val filterTitle = "Titre"
    override val filterUploader = "Uploader"
    override val filterTag = "Tag"
    override val filterTagNamespace = "Espace de nom de tag"
    override val deleteFilter = { a: String -> "Supprimer le bloqueur bloqueur \"$a\" ?" }
    override val showDefinition = "Montrer les expliques"
    override val addFilter = "Ajouter un bloqueur"
    override val filterText = "Texte du bloqueur"
    override val filterTip = "Les galeries non désirées qui satisfont les critères des bloqueurs ne sont pas affichées.\n\nBloqueur du titre : exclure les galeries dont le titre contient le mot.\n\nBloqueur d'uploader : exclure les galerie mises en ligne par cet uploader.\n\nBloqueur de tag : exclure les galeries qui ont ce tag. Il vas prendre plus de temps pour charger la liste.\n\nBloqueur d'espace de nom de tag : exclure les galeries qui ont cette espace de nom de tag. Il vas prendre plus de temps pour charger la liste."
    override val uConfig = "Paramètres EHentai"
    override val applyTip = "Appuyez sur la coche pour sauvegarder les changements"
    override val shareImage = "Partager l'image"
    override val imageSaved = { a: String -> "Image sauvegardée à $a" }
    override val settingsEh = "EH"
    override val settingsEhSignOut = "Se déconnecter"
    override val settingsUConfig = "Paramètres EHentai"
    override val settingsUConfigSummary = "Paramètres sur le site Web EHentai"
    override val settingsMyTags = "Tags suivis"
    override val settingsMyTagsSummary = "Gérer les tags suivis sur le site Web EHentai"
    override val settingsEhGallerySite = "Site de galerie"
    override val settingsEhLaunchPage = "Écran par défaut après le lancement"
    override val settingsEhListMode = "Mode de liste"
    override val settingsEhListModeDetail = "Avec détails"
    override val settingsEhListModeThumb = "Miniatures uniquement"
    override val settingsEhDetailSize = "Taille des détails"
    override val settingsEhDetailSizeLong = "Long"
    override val settingsEhDetailSizeShort = "Court"
    override val settingsEhShowJpnTitle = "Montrer les titres japonais"
    override val settingsEhFilter = "Bloqueur des galeries"
    override val settingsEhFilterSummary = "Filtrer les galerie par les titres, les uploaders ou les tags"
    override val myTags = "Tags suivis"
    override val settingsDownload = "Téléchargement"
    override val settingsDownloadDownloadLocation = "Destination de téléchargement"
    override val settingsDownloadCantGetDownloadLocation = "Impossible d'obtenir la destination de téléchargement"
    override val settingsDownloadMediaScan = "Permettre le scannage multimédia"
    override val settingsDownloadMediaScanSummaryOn = "Ne montrez jamais vos applications galerie à personne"
    override val settingsDownloadMediaScanSummaryOff = "La plupart d'applications galerie ignorent les images dans le chemin de téléchargement"
    override val settingsDownloadConcurrency = "Téléchargement multi-thread"
    override val settingsDownloadConcurrencySummary = { a: String -> "Jusqu'à $a image(s)" }
    override val settingsDownloadPreloadImage = "Charger les images d'avance"
    override val settingsDownloadPreloadImageSummary = { a: String -> "Charger les prochaine $a images" }
    override val settingsDownloadDownloadOriginImage = "Télécharger l'images originales"
    override val settingsDownloadDownloadOriginImageSummary = "Vous allez obtenir l'erreur 509 plus facilement"
    override val settingsDownloadRestoreDownloadItems = "Rétablir les galeries téléchargées"
    override val settingsDownloadRestoreDownloadItemsSummary = "Rétablir tous objets déjà téléchargés dans la destination de téléchargement"
    override val settingsDownloadRestoreNotFound = "Aucun objet trouvé à rétablir"
    override val settingsDownloadRestoreFailed = "Échec de rétablissement"
    override val settingsDownloadRestoreSuccessfully = { a: Int -> "$a objets rétablis avec succès" }
    override val settingsDownloadCleanRedundancy = "Effacer fiches inutiles de téléchargement"
    override val settingsDownloadCleanRedundancySummary = "Effacer les fiches des galeries qui ne sont pas dans la liste de téléchargement mais se trouvent dans la destination de téléchargement"
    override val settingsDownloadCleanRedundancyNoRedundancy = "Rien à effacer"
    override val settingsDownloadCleanRedundancyDone = { a: Int -> "Nettoyage complété avec succès, $a objets effacés" }
    override val settingsAdvanced = "Avancé"
    override val settingsAdvancedSaveParseErrorBody = "Enrég. les HTMLs avec erreurs"
    override val settingsAdvancedSaveParseErrorBodySummary = "Le fiche HTML pouvait contenir les informations personnelles"
    override val settingsAdvancedSaveCrashLog = "Enrégistrer le rapport crash"
    override val settingsAdvancedSaveCrashLogSummary = "Les rapports crash aident le développeur à correcter les bugs"
    override val settingsAdvancedDumpLogcat = "Sauvegarder le journal logcat"
    override val settingsAdvancedDumpLogcatSummary = "Sauvegarder le journal logcat au stockage externe"
    override val settingsAdvancedDumpLogcatFailed = "Echec de sauvegarde"
    override val settingsAdvancedDumpLogcatTo = { a: String -> "Fiche sauvegardé à $a" }
    override val clipboardGalleryUrlSnackMessage = "Un lien d'un galerie est détecté dans les presse-papiers."
    override val clipboardGalleryUrlSnackAction = "Aller"
    override val settingsAdvancedReadCacheSize = "Taille du cache pour la lecture"
    override val settingsAdvancedExportData = "Exporter les données"
    override val settingsAdvancedExportDataSummary = "Sauvegarder les données au stockage externe, y compris la liste de téléchargement, la liste de recherche rapide"
    override val settingsAdvancedExportDataTo = { a: String -> "Données exportées à $a" }
    override val settingsAdvancedExportDataFailed = "Échec d'exportation de données"
    override val settingsAdvancedImportData = "Importer les données"
    override val settingsAdvancedImportDataSummary = "Charger les données exportées"
    override val settingsAdvancedImportDataSuccessfully = "Importation complète avec succès"
    override val settingsAbout = "À propos de l'application"
    override val settingsAboutDeclarationSummary = "EhViewer ne s'affilie à E-Hentai de aucune manière."
    override val settingsAboutAuthor = "Développeur"
    override val settingsAboutLatestRelease = "Dernière version"
    override val settingsAboutSource = "Source"
    override val settingsAboutVersion = "Numéro de version"
    override val settingsAboutCheckForUpdates = "Chercher mise à jour"
    override val cantReadTheFile = "Impossible de lire le fiche"
    override val pleaseWait = "Merci de patienter"
    override val cloudFavorites = "Favoris de cloud"
    override val localFavorites = "Favoris locales"
    override val searchBarHint = { a: String -> "Chercher dans $a" }
    override val favoritesTitle = { a: String -> a }
    override val favoritesTitle2 = { a: String, b: String -> "$a - $a" }
    override val deleteFavoritesDialogTitle = "Effacer de favoris"
    override val deleteFavoritesDialogMessage = { a: Int -> "Effacer $a objets de favoris ?" }
    override val moveFavoritesDialogTitle = "Déplacer favoris"
    override val defaultFavoritesCollection = "Collection favoris par défaut"
    override val letMeSelect = "Sélectionner manuellement"
    override val collections = "Collections"
    override val errorSomethingWrongHappened = "Une erreur s'est produite"
    override val fromTheFuture = "Du futur"
    override val justNow = "En ce moment"
    override val yesterday = "Hier"
    override val someDaysAgo = { a: Int -> "Il y a $a jours" }
    override val archive = "Fiches archives"
    override val noArchives = "Aucun fiche archives"
    override val downloadArchiveStarted = "Téléchargement du fiche archives a commencé"
    override val downloadArchiveFailure = "Téléchargement du fiche archives a échoué"
    override val downloadArchiveFailureNoHath = "Client H@H est nécessaire pour le téléchargement de fiches archives"
    override val settingsPrivacy = "Confidentialité"
    override val settingsPrivacySecure = "Empêcher les captures d'ecran"
    override val settingsPrivacySecureSummary = "Quand cette option est activée, vous ne pouvez prendre aucune capture d'écran de l'application, et le systéme n'affiche pas le contenu d'écran dans le sélecteur de tâches.\n\nRedémarrage de l'application est nécessaire pour appliquer le changement"
    override val settingsAdvancedAppLanguageTitle = "Langue de l'app (Language)"
    override val appLanguageSystem = "Langue du système (défaut)"
    override val settingsEhIdentityCookiesSigned = "Tout le monde peut utiliser les cookies de ce compte pour s'y connecter.<br><b>GARDEZ-LES BIEN !</b>"
    override val settingsEhIdentityCookiesGuest = "Vous ne vous êtes pas connecté."
    override val settingsEhShowGalleryPages = "Montrer les nombres des pages des galeries"
    override val settingsEhShowGalleryPagesSummary = "Montrer le nombre des pages des galerie dans la liste des galeries"
    override val darkTheme = "Thème sombre"
    override val darkThemeFollowSystem = "Identique au système"
    override val darkThemeOff = "Toujours actif"
    override val darkThemeOn = "Toujours inactif"
    override val pageCount = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity page"
            else -> "$quantity pages"
        }
    }
    override val someMinutesAgo = { quantity: Int ->
        when (quantity) {
            1 -> "Il y a $quantity minute"
            else -> "Il y a $quantity minutes"
        }
    }
    override val someHoursAgo = { quantity: Int ->
        when (quantity) {
            1 -> "Il y a $quantity heure"
            else -> "Il y a $quantity heures"
        }
    }
    override val second = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity sec"
            else -> "secs"
        }
    }
    override val minute = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity min"
            else -> "mins"
        }
    }
    override val hour = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity heure"
            else -> "heures"
        }
    }
    override val day = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity jour"
            else -> "jours"
        }
    }
    override val year = { quantity: Int ->
        when (quantity) {
            1 -> "$quantity an"
            else -> "ans"
        }
    }
}
