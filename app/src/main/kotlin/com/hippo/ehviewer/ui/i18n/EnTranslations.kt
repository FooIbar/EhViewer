package com.hippo.ehviewer.ui.i18n

object EnTranslations : Translations {
    override val appName = "EhViewer"

    override val siteE = "e-hentai"

    override val siteEx = "exhentai"

    override val doujinshi = "DOUJINSHI"

    override val manga = "MANGA"

    override val artistCg = "ARTIST CG"

    override val gameCg = "GAME CG"

    override val western = "WESTERN"

    override val nonH = "NON-H"

    override val imageSet = "IMAGE SET"

    override val cosplay = "COSPLAY"

    override val asianPorn = "ASIAN PORN"

    override val misc = "MISC"

    override val homepage = "Homepage"

    override val subscription = "Subscription"

    override val whatsHot = "What\'s hot"

    override val favourite = "Favourite"

    override val history = "History"

    override val downloads = "Downloads"

    override val settings = "Settings"

    override val username = "Username"

    override val password = "Password"

    override val signIn = "Sign in"

    override val register = "Register"

    override val signInViaWebview = "Sign in via WebView"

    override val signInFirst = "Please sign in first"

    override val textIsEmpty = "Text is empty"

    override val waring = "Warning"

    override val invalidDownloadLocation = "It seems download location is not available. Please set it in Settings."

    override val clipboardGalleryUrlSnackMessage = "There is a gallery URL in the clipboard"

    override val clipboardGalleryUrlSnackAction = "View"

    override val errorTimeout = "Timeout"

    override val errorUnknownHost = "Unknown host"

    override val errorRedirection = "Too many redirections"

    override val errorSocket = "Network error"

    override val errorUnknown = "Weird"

    override val errorCantFindActivity = "Can\'t find the application"

    override val errorCannotParseTheUrl = "Can\'t parse the URL"

    override val errorDecodingFailed = "Decoding failed"

    override val errorReadingFailed = "Reading Failed"

    override val errorOutOfRange = "Out of range"

    override val errorParseError = "Parse error"

    override val error509 = "509"

    override val errorInvalidUrl = "Invalid URL"

    override val errorGetPtokenError = "Get pToken error"

    override val errorCantSaveImage = "Can\'t save image"

    override val errorInvalidNumber = "Invalid number"

    override val appWaring = "The content of this application is from the Internet. Some of it may do physical or mental harm to you. You have learnt the risks above and would like to undertake them. By continuing to use it, you agree to the above terms."

    override val appWaring2 = "By continuing to use it, you agree to the above terms."

    override val errorUsernameCannotEmpty = "Username cannot be empty"

    override val errorPasswordCannotEmpty = "Password cannot be empty"

    override val guestMode = "Guest mode"

    override val signInFailed = "Sign in failed"

    override val signInFailedTip = { p0: String ->
        "If this issue continues, try \"%s\"."
            .format(p0)
    }

    override val getIt = "Got it"

    override val galleryListSearchBarHintExhentai = "Search ExHentai"

    override val galleryListSearchBarHintEHentai = "Search E-Hentai"

    override val galleryListSearchBarOpenGallery = "Open the gallery"

    override val galleryListEmptyHit = "The World is Big and the panda sit alone"

    override val galleryListEmptyHitSubscription = "Subscribe to tags in Settings->EH->My tags"

    override val keywordSearch = "Keyword search"

    override val imageSearch = "Image search"

    override val searchImage = "Image Search"

    override val searchSh = "Expunged"

    override val searchSto = "Has Torrent"

    override val searchSr = "Minimum Rating"

    override val searchSpTo = "to"

    override val searchSpErr1 = "The page range maximum cannot be below 10"

    override val searchSpErr2 = "The page range is too narrow"

    override val searchSpSuffix = ""

    override val searchSf = "Disable default filters for:"

    override val searchSfl = "Language"

    override val searchSfu = "Uploader"

    override val searchSft = "Tags"

    override val selectImage = "Select image"

    override val selectImageFirst = "Please select image first"

    override val addToFavourites = "Add to favourites"

    override val removeFromFavourites = "Remove from favourites"

    override val deleteDownloads = "Delete downloads"

    override val quickSearch = "Quick search"

    override val quickSearchTip = "Tap \"+\" to add Quick Search"

    override val addQuickSearchDialogTitle = "Add Quick Search"

    override val translateTagForTagger = "Use tag translation"

    override val nameIsEmpty = "Name is empty"

    override val delete = "Delete"

    override val addQuickSearchTip = "The state of gallery list will be saved as quick search. Perform a search first to save the state of search panel."

    override val readme = "README"

    override val imageSearchNotQuickSearch = "Can\'t add image search as quick search"

    override val duplicateQuickSearch = { p0: String ->
        "A duplicate quick search exists. The name is \"%s\"."
            .format(p0)
    }

    override val duplicateName = "This name is already in use."

    override val saveProgress = "Save progress"

    override val deleteQuickSearch = { p0: String ->
        "Delete quick search \"%s\"?"
            .format(p0)
    }

    override val goToHint = { p0: Int, p1: Int ->
        "Page %d, total %d pages"
            .format(p0, p1)
    }

    override val any = "Any"

    override val star2 = "2 stars"

    override val star3 = "3 stars"

    override val star4 = "4 stars"

    override val star5 = "5 stars"

    override val download = "Download"

    override val read = "Read"

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

    override val share = "Share"

    override val rate = "Rate"

    override val similarGallery = "Similar"

    override val searchCover = "Search Cover"

    override val noTags = "No tags"

    override val noComments = "No comments"

    override val noMoreComments = "No more comments"

    override val moreComment = "More comments"

    override val refresh = "Refresh"

    override val viewOriginal = "View original image"

    override val openInOtherApp = "Open with other app"

    override val clearImageCache = "Clear image cache"

    override val clearImageCacheConfirm = "Clear all cached images for this gallery?"

    override val imageCacheCleared = "Image cache cleared"

    override val rateSuccessfully = "Rate successfully"

    override val rateFailed = "Rate failed"

    override val noTorrents = "No torrents"

    override val torrents = "Torrents"

    override val notFavorited = "Not favorited"

    override val addFavoritesDialogTitle = "Add to favorites"

    override val addToFavoriteSuccess = "Added to favorites"

    override val removeFromFavoriteSuccess = "Removed from favorites"

    override val addToFavoriteFailure = "Failed to add to favorites"

    override val removeFromFavoriteFailure = "Failed to remove from favorites"

    override val filterTheUploader = { p0: String ->
        "Block the uploader \"%s\"?"
            .format(p0)
    }

    override val filterTheTag = { p0: String ->
        "Block the tag \"%s\"?"
            .format(p0)
    }

    override val filterAdded = "Blocker added"

    override val newerVersionAvailable = "There are newer versions of this gallery available."

    override val newerVersionTitle = { p0: String, p1: String ->
        "%s, added %s"
            .format(p0, p1)
    }

    override val rating10 = "MASTERPIECE"

    override val rating9 = "AMAZING"

    override val rating8 = "GREAT"

    override val rating7 = "GOOD"

    override val rating6 = "OKAY"

    override val rating5 = "MEDIOCRE"

    override val rating4 = "BAD"

    override val rating3 = "AWFUL"

    override val rating2 = "PAINFUL"

    override val rating1 = "UNBEARABLE"

    override val rating0 = "DISASTER"

    override val ratingNone = "(´_ゝ`)"

    override val galleryInfo = "Gallery Info"

    override val copiedToClipboard = "Copied to clipboard"

    override val keyGid = "GID"

    override val keyToken = "Token"

    override val keyUrl = "URL"

    override val keyTitle = "Title"

    override val keyTitleJpn = "Jpn Title"

    override val keyThumb = "Thumb"

    override val keyCategory = "Category"

    override val keyUploader = "Uploader"

    override val keyPosted = "Posted"

    override val keyParent = "Parent"

    override val keyVisible = "Visible"

    override val keyLanguage = "Language"

    override val keyPages = "Pages"

    override val keySize = "Size"

    override val keyFavoriteCount = "Favorite count"

    override val keyFavorited = "Favorited"

    override val keyRatingCount = "Rating count"

    override val keyRating = "Rating"

    override val keyTorrents = "Torrents"

    override val keyTorrentUrl = "Torrent URL"

    override val galleryComments = "Gallery Comments"

    override val commentSuccessfully = "Comment post successfully"

    override val commentFailed = "Failed to post the comment"

    override val copyCommentText = "Copy comment text"

    override val blockCommenter = "Block the commenter"

    override val filterTheCommenter = { p0: String ->
        "Block the commenter \"%s\"?"
            .format(p0)
    }

    override val editComment = "Edit comment"

    override val editCommentSuccessfully = "The comment has been edited"

    override val editCommentFailed = "Failed to edit the comment"

    override val voteUp = "Vote up"

    override val cancelVoteUp = "Cancel up-vote"

    override val voteDown = "Vote down"

    override val cancelVoteDown = "Cancel down-vote"

    override val voteUpSuccessfully = "Voted up successfully"

    override val cancelVoteUpSuccessfully = "Cancel up-vote successfully"

    override val voteDownSuccessfully = "Voted down successfully"

    override val cancelVoteDownSuccessfully = "Down-vote cancelled successfully"

    override val voteFailed = "Vote failed"

    override val checkVoteStatus = "View vote details"

    override val clickMoreComments = "Click to load more comments"

    override val lastEdited = { p0: String ->
        "Last edited: %s"
            .format(p0)
    }

    override val formatBold = "Bold"

    override val formatItalic = "Italic"

    override val formatUnderline = "Underline"

    override val formatStrikethrough = "Strikethrough"

    override val formatUrl = "URL"

    override val formatPlain = "Plain text"

    override val goTo = "Go to"

    override val sceneDownloadTitle = { p0: String ->
        "Download - %s"
            .format(p0)
    }

    override val noDownloadInfo = "Download items will be shown here"

    override val downloadStateNone = "Idle"

    override val downloadStateWait = "Waiting"

    override val downloadStateDownloading = "Downloading"

    override val downloadStateDownloaded = "Downloaded"

    override val downloadStateFailed = "Failed"

    override val downloadStateFailed2 = { p0: Int ->
        "%d incomplete"
            .format(p0)
    }

    override val downloadStateFinish = "Done"

    override val stat509AlertTitle = "509 Alert"

    override val stat509AlertText = "Image limit has been reached. Please stop download and have a relax."

    override val statDownloadDoneTitle = "Download Finished"

    override val statDownloadDoneTextSucceeded = { p0: Int ->
        "%d succeeded"
            .format(p0)
    }

    override val statDownloadDoneTextFailed = { p0: Int ->
        "%d failed"
            .format(p0)
    }

    override val statDownloadDoneTextMix = { p0: Int, p1: Int ->
        "%d succeeded, %d failed"
            .format(p0, p1)
    }

    override val statDownloadDoneLineSucceeded = { p0: String ->
        "Succeeded: %s"
            .format(p0)
    }

    override val statDownloadDoneLineFailed = { p0: String ->
        "Failed: %s"
            .format(p0)
    }

    override val downloadRemoveDialogTitle = "Remove Download Item"

    override val downloadRemoveDialogMessage = { p0: String ->
        "Remove %s from download list ?"
            .format(p0)
    }

    override val downloadRemoveDialogMessage2 = { p0: Int ->
        "Remove %d items from download list ?"
            .format(p0)
    }

    override val downloadRemoveDialogCheckText = "Delete image files"

    override val statDownloadActionStopAll = "Stop all"

    override val defaultDownloadLabelName = "Default"

    override val downloadMoveDialogTitle = "Move"

    override val downloadLabels = "Download labels"

    override val downloadStartAll = "Start all"

    override val downloadStopAll = "Stop all"

    override val downloadResetReadingProgress = "Reset reading progress"

    override val resetReadingProgressMessage = "Reset the reading progress of all downloaded galleries?"

    override val downloadServiceLabel = "EhViewer Download Service"

    override val downloadSpeedText = { p0: String ->
        "%s"
            .format(p0)
    }

    override val downloadSpeedText2 = { p0: String, p1: String ->
        "%s, %s left"
            .format(p0, p1)
    }

    override val rememberDownloadLabel = "Remember download label"

    override val defaultDownloadLabel = "Default download label"

    override val addedToDownloadList = "Added to download list"

    override val selectGroupingMode = "Select Grouping Mode"

    override val selectGroupingModeCustom = "Custom"

    override val selectGroupingModeArtist = "Artist"

    override val unknownArtists = "Unknown"

    override val add = "Add"

    override val newLabelTitle = "New label"

    override val labelTextIsEmpty = "Label text is empty"

    override val labelTextIsInvalid = "\"Default\" is an invalid label"

    override val labelTextExist = "Label exists"

    override val renameLabelTitle = "Rename label"

    override val deleteLabel = { p0: String ->
        "Delete label \"%s\"?"
            .format(p0)
    }

    override val noHistory = "Viewed galleries will be shown here"

    override val clearAll = "Clear all"

    override val clearAllHistory = "Clear all history?"

    override val filter = "Blockers"

    override val filterTitle = "Title"

    override val filterUploader = "Uploader"

    override val filterTag = "Tag"

    override val filterTagNamespace = "Tag namespace"

    override val filterCommenter = "Commenter"

    override val filterComment = "Comment Regex"

    override val deleteFilter = { p0: String ->
        "Delete blocker \"%s\"?"
            .format(p0)
    }

    override val addFilter = "Add blocker"

    override val showDefinition = "Show definition"

    override val filterText = "Blocker text"

    override val filterTip = "This blocking system filters the result of EHentai website blocking system.\n\nTitle Blocker: exclude the gallery whose title contains the word.\n\nUploader Blocker: exclude the gallery which was uploaded by the uploader.\n\nTag Blocker: exclude the gallery which contain the tag, it takes more time to get gallery list.\n\nTag Namespace Blocker: exclude the gallery which contain the tag namespace, it takes more time to get gallery list.\n\nCommenter Blocker: exclude the comments posted by the commenter.\n\nComment Blocker: exclude the comments matching the regex."

    override val uConfig = "EHentai settings"

    override val applyTip = "Tap the check mark to save the settings"

    override val myTags = "My tags"

    override val shareImage = "Share image"

    override val imageSaved = { p0: String ->
        "Image saved to %s"
            .format(p0)
    }

    override val settingsEh = "EH"

    override val settingsEhSignOut = "Sign out"

    override val settingsEhIdentityCookiesSigned = "Identity cookies can be used to sign in to this account.<br><b>KEEP IT SAFE</b>"

    override val settingsEhIdentityCookiesGuest = "Guest mode"

    override val settingsEhClearIgneous = "Clear igneous"

    override val settingsUConfig = "EHentai settings"

    override val settingsUConfigSummary = "Settings on EHentai website"

    override val settingsMyTags = "My tags"

    override val settingsMyTagsSummary = "My tags on EHentai website"

    override val settingsEhGallerySite = "Gallery site"

    override val settingsEhLaunchPage = "Launch page"

    override val settingsEhListMode = "List mode"

    override val settingsEhListModeDetail = "Detail"

    override val settingsEhListModeThumb = "Thumb"

    override val settingsEhDetailSize = "Detail width"

    override val settingsEhDetailSizeLong = "Long"

    override val settingsEhDetailSizeShort = "Short"

    override val settingsEhThumbColumns = "Thumb columns"

    override val settingsEhForceEhThumb = "Use e-hentai thumbnail server"

    override val settingsEhForceEhThumbSummary = "Try disabling this if you have trouble loading thumbnails"

    override val settingsEhShowJpnTitle = "Show Japanese title"

    override val settingsEhShowJpnTitleSummary = "Require enabling Japanese Title in Settings on EHentai website"

    override val settingsEhShowGalleryPages = "Show gallery pages"

    override val settingsEhShowGalleryPagesSummary = "Display the number of pages in the gallery list"

    override val settingsEhShowVoteStatus = "Show tag vote status"

    override val settingsEhShowGalleryComments = "Show gallery comments"

    override val settingsEhShowGalleryCommentsSummary = "Show comments on the gallery details page"

    override val settingsEhShowGalleryCommentThreshold = "Comment score threshold"

    override val settingsEhShowGalleryCommentThresholdSummary = "Hide comments at or below this score (-101 disables)"

    override val settingsEhShowTagTranslations = "Show tag translations"

    override val settingsEhShowTagTranslationsSummary = "Show tag translations instead of the original text (It takes time to download the data file)"

    override val settingsEhTagTranslationsSource = "Placeholder"

    override val settingsEhTagTranslationsSourceUrl = "https://placeholder"

    override val settingsEhFilter = "Blockers"

    override val settingsEhFilterSummary = "Block gallery or comment by title, uploader, tags and commenter"

    override val settingsReadReverseControls = "Reverse physical key controls"

    override val settingsBlockExtraneousAds = "[Experimental] Block extraneous ads"

    override val settingsAdsPlaceholder = "[Optional] Pick placeholder to replace ads"

    override val settingsDownload = "Download"

    override val settingsDownloadDownloadLocation = "Download location"

    override val settingsDownloadCantGetDownloadLocation = "Can\'t get download location"

    override val settingsDownloadMediaScan = "Allow media scan"

    override val settingsDownloadMediaScanSummaryOn = "Please hide your gallery apps away from other people"

    override val settingsDownloadMediaScanSummaryOff = "Most gallery apps will ignore pictures in the download path"

    override val settingsDownloadConcurrency = "Concurrency download"

    override val settingsDownloadConcurrencySummary = { p0: String ->
        "Up to %s images"
            .format(p0)
    }

    override val settingsDownloadDownloadDelay = "Download delay"

    override val settingsDownloadDownloadDelaySummary = { p0: String ->
        "Delay %s ms per download"
            .format(p0)
    }

    override val settingsDownloadDownloadTimeout = "Download timeout (in seconds)"

    override val settingsDownloadPreloadImage = "Preload image"

    override val settingsDownloadPreloadImageSummary = { p0: String ->
        "Preload next %s image"
            .format(p0)
    }

    override val settingsDownloadDownloadOriginImage = "Download original image"

    override val settingsDownloadDownloadOriginImageSummary = "Caution! May require GP"

    override val settingsDownloadSaveAsCbz = "Save as CBZ archive"

    override val settingsDownloadArchiveMetadata = "Archive metadata"

    override val settingsDownloadArchiveMetadataSummary = "Generate ComicInfo.xml on archive download"

    override val settingsDownloadReloadMetadata = "Reload metadata"

    override val settingsDownloadReloadMetadataSummary = "Regenerate the ComicInfo.xml for download items whose tags may have changed"

    override val settingsDownloadReloadMetadataSuccessfully = { p0: Int ->
        "Reload %d items successfully"
            .format(p0)
    }

    override val settingsDownloadReloadMetadataFailed = { p0: String ->
        "Reload metadata failed: %s"
            .format(p0)
    }

    override val settingsDownloadRestoreDownloadItems = "Restore download items"

    override val settingsDownloadRestoreDownloadItemsSummary = "Restore all download items in download location"

    override val settingsDownloadRestoreNotFound = "Not found download items to restore"

    override val settingsDownloadRestoreFailed = "Restore failed"

    override val settingsDownloadRestoreSuccessfully = { p0: Int ->
        "Restore %d items successfully"
            .format(p0)
    }

    override val settingsDownloadCleanRedundancy = "Clear download redundancy"

    override val settingsDownloadCleanRedundancySummary = "Remove gallery images which are not in download list but in download location"

    override val settingsDownloadCleanRedundancyNoRedundancy = "No redundancy"

    override val settingsDownloadCleanRedundancyDone = { p0: Int ->
        "Redundancy cleaning completed, clean-up %d items totally"
            .format(p0)
    }

    override val settingsAdvanced = "Advanced"

    override val settingsAdvancedSaveParseErrorBody = "Save HTML content when parsing error"

    override val settingsAdvancedSaveParseErrorBodySummary = "Html content may be privacy-sensitive"

    override val settingsAdvancedSaveCrashLog = "Save crash log when app crashes"

    override val settingsAdvancedSaveCrashLogSummary = "Crash logs help developers fix bugs"

    override val settingsAdvancedDumpLogcat = "Dump logcat"

    override val settingsAdvancedDumpLogcatSummary = "Save logcat to external storage"

    override val settingsAdvancedDumpLogcatFailed = "Dump logcat failed"

    override val settingsAdvancedDumpLogcatTo = { p0: String ->
        "Logcat dumped to %s"
            .format(p0)
    }

    override val settingsAdvancedReadCacheSize = "Read cache size"

    override val settingsAdvancedAppLanguageTitle = "App language"

    override val settingsAdvancedHardwareBitmapThreshold = "Hardware bitmap (better performance) threshold"

    override val settingsAdvancedHardwareBitmapThresholdSummary = "Try decreasing this if long images fail to load"

    override val settingsAdvancedExportData = "Export data"

    override val settingsAdvancedExportDataSummary = "Save data like download list, quick search list, to external storage"

    override val settingsAdvancedExportDataTo = { p0: String ->
        "Exported data to %s"
            .format(p0)
    }

    override val settingsAdvancedExportDataFailed = "Failed to export data"

    override val settingsAdvancedImportData = "Import data"

    override val settingsAdvancedImportDataSummary = "Load data which were previously saved"

    override val settingsAdvancedImportDataSuccessfully = "Data imported"

    override val settingsAdvancedBackupFavorite = "Backup favorite list"

    override val settingsAdvancedBackupFavoriteSummary = "Backup remote favorite list to local"

    override val settingsAdvancedBackupFavoriteStart = { p0: String ->
        "Backing up favorite list %s"
            .format(p0)
    }

    override val settingsAdvancedBackupFavoriteNothing = "Nothing to backup"

    override val settingsAdvancedBackupFavoriteSuccess = "Backup favorite list success"

    override val settingsAdvancedBackupFavoriteFailed = "Backup favorite list failed"

    override val settingsAbout = "About"

    override val settingsAboutDeclaration = "EhViewer"

    override val settingsAboutDeclarationSummary = "EhViewer is not affiliated with E-Hentai.org in any way"

    override val settingsAboutAuthor = "Author"

    override val settingsAboutAuthorSummary = "<del>Hippo &lt;ehviewersu@gmail.com&gt;</del><br><del>NekoInverter</del><br><del>飛鳥澪</del><br>Foolbar"

    override val settingsAboutLatestRelease = "Latest release"

    override val settingsAboutSource = "Source"

    override val settingsAboutVersion = "Build version"

    override val settingsAboutCommitTime = { p0: String ->
        "Committed at %s"
            .format(p0)
    }

    override val settingsAboutCheckForUpdates = "Check for updates"

    override val license = "License"

    override val cantReadTheFile = "Can\'t read the file"

    override val appLanguageSystem = "System Language (Default)"

    override val pleaseWait = "Please wait"

    override val cloudFavorites = "Cloud Favorites"

    override val localFavorites = "Local Favorites"

    override val searchBarHint = { p0: String ->
        "Search %s"
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

    override val deleteFavoritesDialogTitle = "Delete from favorites"

    override val deleteFavoritesDialogMessage = { p0: Int ->
        "Delete %d items from favorites?"
            .format(p0)
    }

    override val moveFavoritesDialogTitle = "Move favorites"

    override val defaultFavoritesCollection = "Default favorites collection"

    override val defaultFavoritesWarning = "You won\'t be able to add favorite notes if you enable this"

    override val letMeSelect = "Let me select"

    override val favoriteNote = "Favorite Note"

    override val collections = "Collections"

    override val errorSomethingWrongHappened = "Something wrong happened"

    override val fromTheFuture = "From the future"

    override val justNow = "Just now"

    override val yesterday = "Yesterday"

    override val someDaysAgo = { p0: Int ->
        "%d days ago"
            .format(p0)
    }

    override val archive = "Archive"

    override val archiveFree = "Free"

    override val archiveOriginal = "Original"

    override val archiveResample = "Resample"

    override val noArchives = "No Archives"

    override val downloadArchiveStarted = "Archive download started"

    override val downloadArchiveFailure = "Failed to download archive"

    override val downloadArchiveFailureNoHath = "Need H@H client for archive download"

    override val currentFunds = "Current Funds:"

    override val insufficientFunds = "Insufficient Funds"

    override val imageLimits = "Image Limits"

    override val imageLimitsSummary = "Used:"

    override val imageLimitsNormal = "No restrictions"

    override val imageLimitsRestricted = "Image resolution restricted to 1280x"

    override val resetCost = { p0: Int ->
        "Spend %d GP to reset"
            .format(p0)
    }

    override val reset = "Reset"

    override val settingsPrivacy = "Privacy"

    override val settingsPrivacySecure = "Prevent screenshots"

    override val settingsPrivacySecureSummary = "Prevent the content of the app from being taken screenshots of or shown in the \"Recent Apps\" list."

    override val clearSearchHistory = "Clear device search history"

    override val clearSearchHistorySummary = "Remove searches you have performed from this device"

    override val clearSearchHistoryConfirm = "Clear search history?"

    override val searchHistoryCleared = "Search history cleared"

    override val downloadService = "Download Service"

    override val favoriteName = "Favorite"

    override val darkTheme = "Dark theme"

    override val blackDarkTheme = "Black dark theme"

    override val harmonizeCategoryColor = "Harmonize category color for Dynamic Color"

    override val sortBy = "Sort by"

    override val addedTimeDesc = "Added time (descending)"

    override val addedTimeAsc = "Added time (ascending)"

    override val uploadedTimeDesc = "Uploaded time (descending)"

    override val uploadedTimeAsc = "Uploaded time (ascending)"

    override val titleAsc = "Title (ascending)"

    override val titleDesc = "Title (descending)"

    override val pageCountAsc = "Page count (ascending)"

    override val pageCountDesc = "Page count (descending)"

    override val groupByDownloadLabel = "Group by download label"

    override val downloadFilter = "Filter"

    override val downloadAll = "All"

    override val downloadStartAllReversed = "Start all (reversed)"

    override val noBrowserInstalled = "Just install a browser please."

    override val toplistAlltime = "All-Time"

    override val toplistPastyear = "Past Year"

    override val toplistPastmonth = "Past Month"

    override val toplistYesterday = "Yesterday"

    override val toplist = "Toplist"

    override val tagVoteDown = "Vote down"

    override val tagVoteUp = "Vote up"

    override val tagVoteWithdraw = "Withdraw vote"

    override val tagVoteSuccessfully = "Vote successfully"

    override val deleteSearchHistory = { p0: String ->
        "Delete \"%s\" from search history?"
            .format(p0)
    }

    override val actionAddTag = "Add tag"

    override val actionAddTagTip = "Enter new tags"

    override val commentUserUploader = { p0: String ->
        "%s (Uploader)"
            .format(p0)
    }

    override val noNetwork = "No network"

    override val settingsEhMeteredNetworkWarning = "Metered network warning"

    override val meteredNetworkWarning = "Connected to metered networks"

    override val readFrom = { p0: Int ->
        "Read page %d"
            .format(p0)
    }

    override val settingsEhRequestNews = "Timed request news page"

    override val settingsEhHideHvEvents = "Hide HV event Notifications"

    override val copyTrans = "Copy translation"

    override val defaultDownloadDirNotEmpty = "The default download directory is not empty!"

    override val resetDownloadLocation = "Reset to default"

    override val pickNewDownloadLocation = "Pick a new location"

    override val dontShowAgain = "Don\'t show again"

    override val openSettings = "Open settings"

    override val appLinkNotVerifiedMessage = "For Android 12 and newer, you need to manually add link to verified links in order to open E-Hentai links in EhViewer."

    override val appLinkNotVerifiedTitle = "App links not verified"

    override val openByDefault = "Open by default"

    override val backupBeforeUpdate = "Backup data before update"

    override val useCiUpdateChannel = "Use CI update channel"

    override val settingsPrivacyRequireUnlock = "Require Unlock"

    override val settingsPrivacyRequireUnlockDelay = "Lock Delay"

    override val settingsPrivacyRequireUnlockDelaySummary = { p0: String ->
        "No unlock is required when leaving App and returning within %s minute(s)"
            .format(p0)
    }

    override val settingsPrivacyRequireUnlockDelaySummaryImmediately = "Unlock is required whenever you return to this App"

    override val filterLabel = "Blocker Type"

    override val archivePasswd = "password"

    override val archiveNeedPasswd = "Archive need password"

    override val passwdWrong = "Password Wrong"

    override val passwdCannotBeEmpty = "Password can\'t be empty"

    override val listTileThumbSize = "Thumb size in detail mode"

    override val accountName = "Account"

    override val preloadThumbAggressively = "Preload thumbs aggressively"

    override val animateItems = "List item animations"

    override val animateItemsSummary = "Try disabling this if you are facing crashes / frame drops"

    override val autoUpdates = "Automatically check for updates"

    override val updateFrequencyNever = "Never"

    override val updateFrequencyDaily = "Daily"

    override val updateFrequency3days = "Every 3 days"

    override val updateFrequencyWeekly = "Weekly"

    override val updateFrequencyBiweekly = "Biweekly"

    override val updateFrequencyMonthly = "Monthly"

    override val updateFailed = { p0: String ->
        "Update failed: %s"
            .format(p0)
    }

    override val newVersionAvailable = "New version available!"

    override val alreadyLatestVersion = "Already the latest version"

    override val permissionDenied = "Permission denied"

    override val downloadGalleryFirst = "Please download the gallery first!"

    override val exportAsArchive = "Export as archive"

    override val exportAsArchiveSuccess = "Export succeed"

    override val exportAsArchiveFailed = "Export failed"

    override val prefCropBorders = "Crop borders"

    override val actionSettings = "Settings"

    override val prefRotationType = "Default rotation type"

    override val viewer = "Reading mode"

    override val actionMenu = "Menu"

    override val navZonePrev = "Prev"

    override val navZoneNext = "Next"

    override val navZoneLeft = "Left"

    override val navZoneRight = "Right"

    override val decodeImageError = "The image couldn\'t be loaded"

    override val actionRetry = "Retry"

    override val labelDefault = "Default"

    override val rotationFree = "Free"

    override val rotationPortrait = "Portrait"

    override val rotationReversePortrait = "Reverse portrait"

    override val rotationLandscape = "Landscape"

    override val rotationForcePortrait = "Locked portrait"

    override val rotationForceLandscape = "Locked landscape"

    override val leftToRightViewer = "Left to right"

    override val rightToLeftViewer = "Right to left"

    override val verticalViewer = "Vertical"

    override val webtoonViewer = "Webtoon"

    override val verticalPlusViewer = "Continuous vertical"

    override val pagerViewer = "Paged"

    override val prefFullscreen = "Fullscreen"

    override val prefCutoutShort = "Show content in cutout area"

    override val prefPageTransitions = "Animate page transitions"

    override val prefShowPageNumber = "Show page number"

    override val prefShowReaderSeekbar = "Show page jumping seekbar"

    override val prefDoubleTapToZoom = "Double tap to zoom"

    override val prefCustomBrightness = "Custom brightness"

    override val prefGrayscale = "Grayscale"

    override val prefInvertedColors = "Inverted"

    override val prefCustomColorFilter = "Custom color filter"

    override val prefColorFilterMode = "Color filter blend mode"

    override val filterModeMultiply = "Multiply"

    override val filterModeScreen = "Screen"

    override val filterModeOverlay = "Overlay"

    override val filterModeLighten = "Dodge / Lighten"

    override val filterModeDarken = "Burn / Darken"

    override val prefKeepScreenOn = "Keep screen on"

    override val prefReadWithTappingInverted = "Invert tap zones"

    override val tappingInvertedNone = "None"

    override val tappingInvertedHorizontal = "Horizontal"

    override val tappingInvertedVertical = "Vertical"

    override val tappingInvertedBoth = "Both"

    override val prefReadWithLongTap = "Show on long tap"

    override val prefReaderTheme = "Background color"

    override val whiteBackground = "White"

    override val grayBackground = "Gray"

    override val blackBackground = "Black"

    override val automaticBackground = "Auto"

    override val lNav = "L shaped"

    override val kindlishNav = "Kindle-ish"

    override val edgeNav = "Edge"

    override val rightAndLeftNav = "Right and Left"

    override val disabledNav = "Disabled"

    override val prefViewerNav = "Tap zones"

    override val prefImageScaleType = "Scale type"

    override val scaleTypeFitScreen = "Fit screen"

    override val scaleTypeStretch = "Stretch"

    override val scaleTypeFitWidth = "Fit width"

    override val scaleTypeFitHeight = "Fit height"

    override val scaleTypeOriginalSize = "Original size"

    override val scaleTypeSmartFit = "Smart fit"

    override val prefNavigatePan = "Pan wide images when tapping"

    override val prefLandscapeZoom = "Zoom landscape image"

    override val prefZoomStart = "Zoom start position"

    override val zoomStartAutomatic = "Automatic"

    override val zoomStartLeft = "Left"

    override val zoomStartRight = "Right"

    override val zoomStartCenter = "Center"

    override val rotationType = "Rotation type"

    override val prefCategoryReadingMode = "Reading mode"

    override val prefWebtoonSidePadding = "Side padding"

    override val webtoonSidePadding0 = "None"

    override val webtoonSidePadding5 = "5%"

    override val webtoonSidePadding10 = "10%"

    override val webtoonSidePadding15 = "15%"

    override val webtoonSidePadding20 = "20%"

    override val webtoonSidePadding25 = "25%"

    override val prefCategoryGeneral = "General"

    override val customFilter = "Custom filter"

    override val actionShare = "Share"

    override val actionCopy = "Copy"

    override val actionSave = "Save"

    override val actionSaveTo = "Save to…"

    override val wideColorGamut = "Use Display P3 color space"

    override val settingsEhRequestNewsTimepicker = "Set time to request news"

    override val darkThemeFollowSystem = "Follow system"

    override val darkThemeOff = "Always off"

    override val darkThemeOn = "Always on"

    override val blockedImage = "Blocked image"

    override val showBlockedImage = "Show blocked image"

    override val pageCount = { quantity: Int ->
        when (quantity) {
            1 -> "%d page"
            else -> "%d pages"
        }.format(quantity)
    }

    override val someMinutesAgo = { quantity: Int ->
        when (quantity) {
            1 -> "A minute ago"
            else -> "%d minutes ago"
        }.format(quantity)
    }

    override val someHoursAgo = { quantity: Int ->
        when (quantity) {
            1 -> "An hour ago"
            else -> "%d hours ago"
        }.format(quantity)
    }

    override val second = { quantity: Int ->
        when (quantity) {
            1 -> "sec"
            else -> "secs"
        }.format(quantity)
    }

    override val minute = { quantity: Int ->
        when (quantity) {
            1 -> "min"
            else -> "mins"
        }.format(quantity)
    }

    override val hour = { quantity: Int ->
        when (quantity) {
            1 -> "hour"
            else -> "hours"
        }.format(quantity)
    }

    override val day = { quantity: Int ->
        when (quantity) {
            1 -> "day"
            else -> "days"
        }.format(quantity)
    }

    override val year = { quantity: Int ->
        when (quantity) {
            1 -> "year"
            else -> "years"
        }.format(quantity)
    }
}