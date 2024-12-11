package com.hippo.ehviewer.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.withStyle
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings

inline fun strings(f: Strings.() -> Unit) = Strings().apply(f)

class Strings {
    var appName: String = "EhViewer"
    var siteE: String = "e-hentai"
    var siteEx: String = "exhentai"
    var doujinshi: String = "DOUJINSHI"
    var manga: String = "MANGA"
    var artistCg: String = "ARTIST CG"
    var gameCg: String = "GAME CG"
    var western: String = "WESTERN"
    var nonH: String = "NON-H"
    var imageSet: String = "IMAGE SET"
    var cosplay: String = "COSPLAY"
    var asianPorn: String = "ASIAN PORN"
    var misc: String = "MISC"
    var homepage: String = "Homepage"
    var subscription: String = "Subscription"
    var whatsHot: String = "What's hot"
    var favourite: String = "Favourite"
    var history: String = "History"
    var downloads: String = "Downloads"
    var settings: String = "Settings"
    var username: String = "Username"
    var password: String = "Password"
    var signIn: String = "Sign in"
    var register: String = "Register"
    var signInViaWebview: String = "Sign in via WebView"
    var signInFirst: String = "Please sign in first"
    var textIsEmpty: String = "Text is empty"
    var waring: String = "Warning"
    var invalidDownloadLocation: String = "It seems download location is not available. Please set it in Settings."
    var clipboardGalleryUrlSnackMessage: String = "There is a gallery URL in the clipboard"
    var clipboardGalleryUrlSnackAction: String = "View"
    var errorTimeout: String = "Timeout"
    var errorUnknownHost: String = "Unknown host"
    var errorRedirection: String = "Too many redirections"
    var errorSocket: String = "Network error"
    var errorUnknown: String = "Weird"
    var errorCantFindActivity: String = "Can't find the application"
    var errorCannotParseTheUrl: String = "Can't parse the URL"
    var errorDecodingFailed: String = "Decoding failed"
    var errorReadingFailed: String = "Reading Failed"
    var errorOutOfRange: String = "Out of range"
    var errorParseError: String = "Parse error"
    var error509: String = "509"
    var errorInvalidUrl: String = "Invalid URL"
    var errorGetPtokenError: String = "Get pToken error"
    var errorCantSaveImage: String = "Can't save image"
    var errorInvalidNumber: String = "Invalid number"
    var appWaring: String = "The content of this application is from the Internet. Some of it may do physical or mental harm to you. You have learnt the risks above and would like to undertake them. By continuing to use it, you agree to the above terms."
    var appWaring2: String = "By continuing to use it, you agree to the above terms."
    var errorUsernameCannotEmpty: String = "Username cannot be empty"
    var errorPasswordCannotEmpty: String = "Password cannot be empty"
    var guestMode: String = "Guest mode"
    var signInFailed: String = "Sign in failed"
    var signInFailedTip: (String) -> String = { a -> "If this issue continues, try \"$a\"." }
    var getIt: String = "Got it"
    var galleryListSearchBarHintExhentai: String = "Search ExHentai"
    var galleryListSearchBarHintEHentai: String = "Search E-Hentai"
    var galleryListSearchBarOpenGallery: String = "Open the gallery"
    var galleryListEmptyHit: String = "The World is Big and the panda sit alone"
    var galleryListEmptyHitSubscription: String = "Subscribe to tags in Settings->EH->My tags"
    var keywordSearch: String = "Keyword search"
    var imageSearch: String = "Image search"
    var searchImage: String = "Image Search"
    var searchSh: String = "Expunged"
    var searchSto: String = "Has Torrent"
    var searchSr: String = "Minimum Rating"
    var searchSpTo: String = "to"
    var searchSpErr1: String = "The page range maximum cannot be below 10"
    var searchSpErr2: String = "The page range is too narrow"
    var searchSpSuffix: String = ""
    var searchSf: String = "Disable default filters for:"
    var searchSfl: String = "Language"
    var searchSfu: String = "Uploader"
    var searchSft: String = "Tags"
    var selectImage: String = "Select image"
    var selectImageFirst: String = "Please select image first"
    var addToFavourites: String = "Add to favourites"
    var removeFromFavourites: String = "Remove from favourites"
    var deleteDownloads: String = "Delete downloads"
    var quickSearch: String = "Quick search"
    var quickSearchTip: String = "Tap \"+\" to add Quick Search"
    var addQuickSearchDialogTitle: String = "Add Quick Search"
    var translateTagForTagger: String = "Use tag translation"
    var nameIsEmpty: String = "Name is empty"
    var delete: String = "Delete"
    var addQuickSearchTip: String = "The state of gallery list will be saved as quick search. Perform a search first to save the state of search panel."
    var readme: String = "README"
    var imageSearchNotQuickSearch: String = "Can't add image search as quick search"
    var duplicateQuickSearch: (String) -> String = { a -> "A duplicate quick search exists. The name is \"$a\"." }
    var duplicateName: String = "This name is already in use."
    var saveProgress: String = "Save progress"
    var deleteQuickSearch: (String) -> String = { a -> "Delete quick search \"$a\"?" }
    var goToHint: (Int, Int) -> String = { a, b -> "Page $a, total $b pages" }
    var any: String = "Any"
    var star2: String = "2 stars"
    var star3: String = "3 stars"
    var star4: String = "4 stars"
    var star5: String = "5 stars"
    var download: String = "Download"
    var read: String = "Read"
    var favoredTimes: (Int) -> String = { a -> "\u2665 $a" }
    var ratingText: (String, Float, Int) -> String = { a, b, c -> "%s (%.2f, %d)".format(a, b, c) }
    var torrentCount: (Int) -> String = { a -> "Torrent ($a)" }
    var share: String = "Share"
    var rate: String = "Rate"
    var similarGallery: String = "Similar"
    var searchCover: String = "Search Cover"
    var noTags: String = "No tags"
    var noComments: String = "No comments"
    var noMoreComments: String = "No more comments"
    var moreComment: String = "More comments"
    var refresh: String = "Refresh"
    var viewOriginal: String = "View original image"
    var openInOtherApp: String = "Open with other app"
    var clearImageCache: String = "Clear image cache"
    var clearImageCacheConfirm: String = "Clear all cached images for this gallery?"
    var imageCacheCleared: String = "Image cache cleared"
    var rateSuccessfully: String = "Rate successfully"
    var rateFailed: String = "Rate failed"
    var noTorrents: String = "No torrents"
    var torrents: String = "Torrents"
    var notFavorited: String = "Not favorited"
    var addFavoritesDialogTitle: String = "Add to favorites"
    var addToFavoriteSuccess: String = "Added to favorites"
    var removeFromFavoriteSuccess: String = "Removed from favorites"
    var addToFavoriteFailure: String = "Failed to add to favorites"
    var removeFromFavoriteFailure: String = "Failed to remove from favorites"
    var filterTheUploader: (String) -> String = { a -> "Block the uploader \"$a\"?" }
    var filterTheTag: (String) -> String = { a -> "Block the tag \"$a\"?" }
    var filterAdded: String = "Blocker added"
    var newerVersionAvailable: String = "There are newer versions of this gallery available."
    var newerVersionTitle: (String, String) -> String = { a, b -> "$a, added $b" }
    var rating10: String = "MASTERPIECE"
    var rating9: String = "AMAZING"
    var rating8: String = "GREAT"
    var rating7: String = "GOOD"
    var rating6: String = "OKAY"
    var rating5: String = "MEDIOCRE"
    var rating4: String = "BAD"
    var rating3: String = "AWFUL"
    var rating2: String = "PAINFUL"
    var rating1: String = "UNBEARABLE"
    var rating0: String = "DISASTER"
    var ratingNone: String = "(´_ゝ`)"
    var galleryInfo: String = "Gallery Info"
    var copiedToClipboard: String = "Copied to clipboard"
    var keyGid: String = "GID"
    var keyToken: String = "Token"
    var keyUrl: String = "URL"
    var keyTitle: String = "Title"
    var keyTitleJpn: String = "Jpn Title"
    var keyThumb: String = "Thumb"
    var keyCategory: String = "Category"
    var keyUploader: String = "Uploader"
    var keyPosted: String = "Posted"
    var keyParent: String = "Parent"
    var keyVisible: String = "Visible"
    var keyLanguage: String = "Language"
    var keyPages: String = "Pages"
    var keySize: String = "Size"
    var keyFavoriteCount: String = "Favorite count"
    var keyFavorited: String = "Favorited"
    var keyRatingCount: String = "Rating count"
    var keyRating: String = "Rating"
    var keyTorrents: String = "Torrents"
    var keyTorrentUrl: String = "Torrent URL"
    var galleryComments: String = "Gallery Comments"
    var commentSuccessfully: String = "Comment post successfully"
    var commentFailed: String = "Failed to post the comment"
    var copyCommentText: String = "Copy comment text"
    var blockCommenter: String = "Block the commenter"
    var filterTheCommenter: (String) -> String = { a -> "Block the commenter \"$a\"?" }
    var editComment: String = "Edit comment"
    var editCommentSuccessfully: String = "The comment has been edited"
    var editCommentFailed: String = "Failed to edit the comment"
    var voteUp: String = "Vote up"
    var cancelVoteUp: String = "Cancel up-vote"
    var voteDown: String = "Vote down"
    var cancelVoteDown: String = "Cancel down-vote"
    var voteUpSuccessfully: String = "Voted up successfully"
    var cancelVoteUpSuccessfully: String = "Cancel up-vote successfully"
    var voteDownSuccessfully: String = "Voted down successfully"
    var cancelVoteDownSuccessfully: String = "Down-vote cancelled successfully"
    var voteFailed: String = "Vote failed"
    var checkVoteStatus: String = "View vote details"
    var clickMoreComments: String = "Click to load more comments"
    var lastEdited: (String) -> String = { a -> "Last edited: $a" }
    var goTo: String = "Go to"
    var sceneDownloadTitle: (String) -> String = { a -> "Download - $a" }
    var noDownloadInfo: String = "Download items will be shown here"
    var downloadStateNone: String = "Idle"
    var downloadStateWait: String = "Waiting"
    var downloadStateDownloading: String = "Downloading"
    var downloadStateDownloaded: String = "Downloaded"
    var downloadStateFailed: String = "Failed"
    var downloadStateFailed2: (Int) -> String = { a -> "$a incomplete" }
    var downloadStateFinish: String = "Done"
    var stat509AlertTitle: String = "509 Alert"
    var stat509AlertText: String = "Image limit has been reached. Please stop download and have a relax."
    var statDownloadDoneTitle: String = "Download Finished"
    var statDownloadDoneTextSucceeded: (Int) -> String = { a -> "$a succeeded" }
    var statDownloadDoneTextFailed: (Int) -> String = { a -> "$a failed" }
    var statDownloadDoneTextMix: (Int, Int) -> String = { a, b -> "$a succeeded, $b failed" }
    var statDownloadDoneLineSucceeded: (String) -> String = { a -> "Succeeded: $a" }
    var statDownloadDoneLineFailed: (String) -> String = { a -> "Failed: $a" }
    var downloadRemoveDialogTitle: String = "Remove Download Item"
    var downloadRemoveDialogMessage: (String) -> String = { a -> "Remove $a from download list ?" }
    var downloadRemoveDialogMessage2: (Int) -> String = { a -> "Remove $a items from download list ?" }
    var downloadRemoveDialogCheckText: String = "Delete image files"
    var statDownloadActionStopAll: String = "Stop all"
    var defaultDownloadLabelName: String = "Default"
    var downloadMoveDialogTitle: String = "Move"
    var downloadLabels: String = "Download labels"
    var downloadStartAll: String = "Start all"
    var downloadStopAll: String = "Stop all"
    var downloadResetReadingProgress: String = "Reset reading progress"
    var resetReadingProgressMessage: String = "Reset the reading progress of all downloaded galleries?"
    var downloadServiceLabel: String = "EhViewer Download Service"
    var downloadSpeedText: (String) -> String = { a -> a }
    var downloadSpeedText2: (String, String) -> String = { a, b -> "$a, $b left" }
    var rememberDownloadLabel: String = "Remember download label"
    var defaultDownloadLabel: String = "Default download label"
    var addedToDownloadList: String = "Added to download list"
    var selectGroupingMode: String = "Select Grouping Mode"
    var selectGroupingModeCustom: String = "Custom"
    var selectGroupingModeArtist: String = "Artist"
    var unknownArtists: String = "Unknown"
    var add: String = "Add"
    var newLabelTitle: String = "New label"
    var labelTextIsEmpty: String = "Label text is empty"
    var labelTextIsInvalid: String = "\"Default\" is an invalid label"
    var labelTextExist: String = "Label exists"
    var renameLabelTitle: String = "Rename label"
    var deleteLabel: (String) -> String = { a -> "Delete label \"$a\"?" }
    var noHistory: String = "Viewed galleries will be shown here"
    var clearAll: String = "Clear all"
    var clearAllHistory: String = "Clear all history?"
    var filter: String = "Blockers"
    var filterTitle: String = "Title"
    var filterUploader: String = "Uploader"
    var filterTag: String = "Tag"
    var filterTagNamespace: String = "Tag namespace"
    var filterCommenter: String = "Commenter"
    var filterComment: String = "Comment Regex"
    var deleteFilter: (String) -> String = { a -> "Delete blocker \"$a\"?" }
    var addFilter: String = "Add blocker"
    var showDefinition: String = "Show definition"
    var filterText: String = "Blocker text"
    var filterTip: String = "This blocking system filters the result of EHentai website blocking system.\n\nTitle Blocker: exclude the gallery whose title contains the word.\n\nUploader Blocker: exclude the gallery which was uploaded by the uploader.\n\nTag Blocker: exclude the gallery which contain the tag, it takes more time to get gallery list.\n\nTag Namespace Blocker: exclude the gallery which contain the tag namespace, it takes more time to get gallery list.\n\nCommenter Blocker: exclude the comments posted by the commenter.\n\nComment Blocker: exclude the comments matching the regex."
    var uConfig: String = "EHentai settings"
    var applyTip: String = "Tap the check mark to save the settings"
    var myTags: String = "My tags"
    var shareImage: String = "Share image"
    var imageSaved: (String) -> String = { a -> "Image saved to $a" }
    var settingsEh: String = "EH"
    var settingsEhSignOut: String = "Sign out"
    var settingsEhIdentityCookiesSigned: String = "Identity cookies can be used to sign in to this account.<br><b>KEEP IT SAFE</b>"
    var settingsEhIdentityCookiesGuest: String = "Guest mode"
    var settingsEhClearIgneous: String = "Clear igneous"
    var settingsUConfig: String = "EHentai settings"
    var settingsUConfigSummary: String = "Settings on EHentai website"
    var settingsMyTags: String = "My tags"
    var settingsMyTagsSummary: String = "My tags on EHentai website"
    var settingsEhGallerySite: String = "Gallery site"
    var settingsEhLaunchPage: String = "Launch page"
    var settingsEhListMode: String = "List mode"
    var settingsEhListModeDetail: String = "Detail"
    var settingsEhListModeThumb: String = "Thumb"
    var settingsEhDetailSize: String = "Detail width"
    var settingsEhDetailSizeLong: String = "Long"
    var settingsEhDetailSizeShort: String = "Short"
    var settingsEhThumbColumns: String = "Thumb columns"
    var settingsEhForceEhThumb: String = "Use e-hentai thumbnail server"
    var settingsEhForceEhThumbSummary: String = "Try disabling this if you have trouble loading thumbnails"
    var settingsEhShowJpnTitle: String = "Show Japanese title"
    var settingsEhShowJpnTitleSummary: String = "Require enabling Japanese Title in Settings on EHentai website"
    var settingsEhShowGalleryPages: String = "Show gallery pages"
    var settingsEhShowGalleryPagesSummary: String = "Display the number of pages in the gallery list"
    var settingsEhShowVoteStatus: String = "Show tag vote status"
    var settingsEhShowGalleryComments: String = "Show gallery comments"
    var settingsEhShowGalleryCommentsSummary: String = "Show comments on the gallery details page"
    var settingsEhShowGalleryCommentThreshold: String = "Comment score threshold"
    var settingsEhShowGalleryCommentThresholdSummary: String = "Hide comments at or below this score (-101 disables)"
    var settingsEhShowTagTranslations: String = "Show tag translations"
    var settingsEhShowTagTranslationsSummary: String = "Show tag translations instead of the original text (It takes time to download the data file)"
    var settingsEhTagTranslationsSource: String = "Placeholder"
    var settingsEhTagTranslationsSourceUrl: String = "https://placeholder"
    var settingsEhFilter: String = "Blockers"
    var settingsEhFilterSummary: String = "Block gallery or comment by title, uploader, tags and commenter"
    var settingsReadReverseControls: String = "Reverse physical key controls"
    var settingsBlockExtraneousAds: String = "[Experimental] Block extraneous ads"
    var settingsAdsPlaceholder: String = "[Optional] Pick placeholder to replace ads"
    var settingsDownload: String = "Download"
    var settingsDownloadDownloadLocation: String = "Download location"
    var settingsDownloadCantGetDownloadLocation: String = "Can't get download location"
    var settingsDownloadMediaScan: String = "Allow media scan"
    var settingsDownloadMediaScanSummaryOn: String = "Please hide your gallery apps away from other people"
    var settingsDownloadMediaScanSummaryOff: String = "Most gallery apps will ignore pictures in the download path"
    var settingsDownloadConcurrency: String = "Concurrency download"
    var settingsDownloadConcurrencySummary: (Int) -> String = { a -> "Up to $a images" }
    var settingsDownloadDownloadDelay: String = "Download delay"
    var settingsDownloadDownloadDelaySummary: (Int) -> String = { a -> "Delay $a ms per download" }
    var settingsDownloadDownloadTimeout: String = "Download timeout (in seconds)"
    var settingsDownloadPreloadImage: String = "Preload image"
    var settingsDownloadPreloadImageSummary: (Int) -> String = { a -> "Preload next $a image" }
    var settingsDownloadDownloadOriginImage: String = "Download original image"
    var settingsDownloadDownloadOriginImageSummary: String = "Caution! May require GP"
    var settingsDownloadSaveAsCbz: String = "Save as CBZ archive"
    var settingsDownloadArchiveMetadata: String = "Archive metadata"
    var settingsDownloadArchiveMetadataSummary: String = "Generate ComicInfo.xml on archive download"
    var settingsDownloadReloadMetadata: String = "Reload metadata"
    var settingsDownloadReloadMetadataSummary: String = "Regenerate the ComicInfo.xml for download items whose tags may have changed"
    var settingsDownloadReloadMetadataSuccessfully: (Int) -> String = { a -> "Reload $a items successfully" }
    var settingsDownloadReloadMetadataFailed: (String) -> String = { a -> "Reload metadata failed: $a" }
    var settingsDownloadRestoreDownloadItems: String = "Restore download items"
    var settingsDownloadRestoreDownloadItemsSummary: String = "Restore all download items in download location"
    var settingsDownloadRestoreNotFound: String = "Not found download items to restore"
    var settingsDownloadRestoreFailed: String = "Restore failed"
    var settingsDownloadRestoreSuccessfully: (Int) -> String = { a -> "Restore $a items successfully" }
    var settingsDownloadCleanRedundancy: String = "Clear download redundancy"
    var settingsDownloadCleanRedundancySummary: String = "Remove gallery images which are not in download list but in download location"
    var settingsDownloadCleanRedundancyNoRedundancy: String = "No redundancy"
    var settingsDownloadCleanRedundancyDone: (Int) -> String = { a -> "Redundancy cleaning completed, clean-up $a items totally" }
    var settingsAdvanced: String = "Advanced"
    var settingsAdvancedSaveParseErrorBody: String = "Save HTML content when parsing error"
    var settingsAdvancedSaveParseErrorBodySummary: String = "Html content may be privacy-sensitive"
    var settingsAdvancedSaveCrashLog: String = "Save crash log when app crashes"
    var settingsAdvancedSaveCrashLogSummary: String = "Crash logs help developers fix bugs"
    var settingsAdvancedDumpLogcat: String = "Dump logcat"
    var settingsAdvancedDumpLogcatSummary: String = "Save logcat to external storage"
    var settingsAdvancedDumpLogcatFailed: String = "Dump logcat failed"
    var settingsAdvancedDumpLogcatTo: (String) -> String = { a -> "Logcat dumped to $a" }
    var settingsAdvancedReadCacheSize: String = "Read cache size"
    var settingsAdvancedAppLanguageTitle: String = "App language"
    var settingsAdvancedHardwareBitmapThreshold: String = "Hardware bitmap (better performance) threshold"
    var settingsAdvancedHardwareBitmapThresholdSummary: String = "Try decreasing this if long images fail to load"
    var settingsAdvancedExportData: String = "Export data"
    var settingsAdvancedExportDataSummary: String = "Save data like download list, quick search list, to external storage"
    var settingsAdvancedExportDataTo: (String) -> String = { a -> "Exported data to $a" }
    var settingsAdvancedExportDataFailed: String = "Failed to export data"
    var settingsAdvancedImportData: String = "Import data"
    var settingsAdvancedImportDataSummary: String = "Load data which were previously saved"
    var settingsAdvancedImportDataSuccessfully: String = "Data imported"
    var settingsAdvancedBackupFavorite: String = "Backup favorite list"
    var settingsAdvancedBackupFavoriteSummary: String = "Backup remote favorite list to local"
    var settingsAdvancedBackupFavoriteStart: (String) -> String = { a -> "Backing up favorite list $a" }
    var settingsAdvancedBackupFavoriteNothing: String = "Nothing to backup"
    var settingsAdvancedBackupFavoriteSuccess: String = "Backup favorite list success"
    var settingsAdvancedBackupFavoriteFailed: String = "Backup favorite list failed"
    var settingsAbout: String = "About"
    var settingsAboutDeclaration: String = "EhViewer"
    var settingsAboutDeclarationSummary: String = "EhViewer is not affiliated with E-Hentai.org in any way"
    var settingsAboutAuthor: String = "Author"
    var settingsAboutAuthorSummary: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            appendLine("Hippo")
            appendLine("NekoInverter")
            appendLine("飛鳥澪")
        }
        append("Foolbar")
    }
    var settingsAboutLatestRelease: String = "Latest release"
    var settingsAboutSource: String = "Source"
    var settingsAboutVersion: String = "Build version"
    var settingsAboutCommitTime: (String) -> String = { a -> "Committed at $a" }
    var settingsAboutCheckForUpdates: String = "Check for updates"
    var license: String = "License"
    var cantReadTheFile: String = "Can't read the file"
    var appLanguageSystem: String = "System Language (Default)"
    var pleaseWait: String = "Please wait"
    var cloudFavorites: String = "Cloud Favorites"
    var localFavorites: String = "Local Favorites"
    var searchBarHint: (String) -> String = { a -> "Search $a" }
    var favoritesTitle: (String) -> String = { a -> a }
    var favoritesTitle2: (String, String) -> String = { a, b -> "$a - $b" }
    var deleteFavoritesDialogTitle: String = "Delete from favorites"
    var deleteFavoritesDialogMessage: (Int) -> String = { a -> "Delete $a items from favorites?" }
    var moveFavoritesDialogTitle: String = "Move favorites"
    var defaultFavoritesCollection: String = "Default favorites collection"
    var defaultFavoritesWarning: String = "You won't be able to add favorite notes if you enable this"
    var letMeSelect: String = "Let me select"
    var favoriteNote: String = "Favorite Note"
    var collections: String = "Collections"
    var errorSomethingWrongHappened: String = "Something wrong happened"
    var fromTheFuture: String = "From the future"
    var justNow: String = "Just now"
    var yesterday: String = "Yesterday"
    var someDaysAgo: (Int) -> String = { a -> "$a days ago" }
    var archive: String = "Archive"
    var archiveFree: String = "Free"
    var archiveOriginal: String = "Original"
    var archiveResample: String = "Resample"
    var noArchives: String = "No Archives"
    var downloadArchiveStarted: String = "Archive download started"
    var downloadArchiveFailure: String = "Failed to download archive"
    var downloadArchiveFailureNoHath: String = "Need H@H client for archive download"
    var currentFunds: String = "Current Funds:"
    var insufficientFunds: String = "Insufficient Funds"
    var imageLimits: String = "Image Limits"
    var imageLimitsSummary: String = "Used:"
    var imageLimitsNormal: String = "No restrictions"
    var imageLimitsRestricted: String = "Image resolution restricted to 1280x"
    var resetCost: (Int) -> String = { a -> "Spend $a GP to reset" }
    var reset: String = "Reset"
    var settingsPrivacy: String = "Privacy"
    var settingsPrivacySecure: String = "Prevent screenshots"
    var settingsPrivacySecureSummary: String = "Prevent the content of the app from being taken screenshots of or shown in the \"Recent Apps\" list."
    var clearSearchHistory: String = "Clear device search history"
    var clearSearchHistorySummary: String = "Remove searches you have performed from this device"
    var clearSearchHistoryConfirm: String = "Clear search history?"
    var searchHistoryCleared: String = "Search history cleared"
    var downloadService: String = "Download Service"
    var keyFavoriteName: String = "Favorite"
    var darkTheme: String = "Dark theme"
    var blackDarkTheme: String = "Black dark theme"
    var harmonizeCategoryColor: String = "Harmonize category color for Dynamic Color"
    var sortBy: String = "Sort by"
    var addedTimeDesc: String = "Added time (descending)"
    var addedTimeAsc: String = "Added time (ascending)"
    var uploadedTimeDesc: String = "Uploaded time (descending)"
    var uploadedTimeAsc: String = "Uploaded time (ascending)"
    var titleAsc: String = "Title (ascending)"
    var titleDesc: String = "Title (descending)"
    var pageCountAsc: String = "Page count (ascending)"
    var pageCountDesc: String = "Page count (descending)"
    var groupByDownloadLabel: String = "Group by download label"
    var downloadFilter: String = "Filter"
    var downloadAll: String = "All"
    var downloadStartAllReversed: String = "Start all (reversed)"
    var noBrowserInstalled: String = "Just install a browser please."
    var toplistAlltime: String = "All-Time"
    var toplistPastyear: String = "Past Year"
    var toplistPastmonth: String = "Past Month"
    var toplistYesterday: String = "Yesterday"
    var toplist: String = "Toplist"
    var tagVoteDown: String = "Vote down"
    var tagVoteUp: String = "Vote up"
    var tagVoteWithdraw: String = "Withdraw vote"
    var tagVoteSuccessfully: String = "Vote successfully"
    var deleteSearchHistory: (String) -> String = { a -> "Delete \"$a\" from search history?" }
    var actionAddTag: String = "Add tag"
    var actionAddTagTip: String = "Enter new tags"
    var commentUserUploader: (String) -> String = { a -> "$a (Uploader)" }
    var noNetwork: String = "No network"
    var settingsEhMeteredNetworkWarning: String = "Metered network warning"
    var meteredNetworkWarning: String = "Connected to metered networks"
    var readFrom: (Int) -> String = { a -> "Read page $a" }
    var settingsEhRequestNews: String = "Timed request news page"
    var settingsEhHideHvEvents: String = "Hide HV event Notifications"
    var copyTrans: String = "Copy translation"
    var defaultDownloadDirNotEmpty: String = "The default download directory is not empty!"
    var resetDownloadLocation: String = "Reset to default"
    var pickNewDownloadLocation: String = "Pick a new location"
    var dontShowAgain: String = "Don\'t show again"
    var openSettings: String = "Open settings"
    var appLinkNotVerifiedMessage: String = "For Android 12 and newer, you need to manually add link to verified links in order to open E-Hentai links in EhViewer."
    var appLinkNotVerifiedTitle: String = "App links not verified"
    var openByDefault: String = "Open by default"
    var backupBeforeUpdate: String = "Backup data before update"
    var useCiUpdateChannel: String = "Use CI update channel"
    var settingsPrivacyRequireUnlock: String = "Require Unlock"
    var settingsPrivacyRequireUnlockDelay: String = "Lock Delay"
    var settingsPrivacyRequireUnlockDelaySummary: (Int) -> String = { a -> "No unlock is required when leaving App and returning within $a minute(s)" }
    var settingsPrivacyRequireUnlockDelaySummaryImmediately: String = "Unlock is required whenever you return to this App"
    var filterLabel: String = "Blocker Type"
    var archivePasswd: String = "password"
    var archiveNeedPasswd: String = "Archive need password"
    var passwdWrong: String = "Password Wrong"
    var passwdCannotBeEmpty: String = "Password can't be empty"
    var listTileThumbSize: String = "Thumb size in detail mode"
    var accountName: String = "Account"
    var preloadThumbAggressively: String = "Preload thumbs aggressively"
    var animateItems: String = "List item animations"
    var animateItemsSummary: String = "Try disabling this if you are facing crashes / frame drops"
    var autoUpdates: String = "Automatically check for updates"
    var updateFrequencyNever: String = "Never"
    var updateFrequencyDaily: String = "Daily"
    var updateFrequency3days: String = "Every 3 days"
    var updateFrequencyWeekly: String = "Weekly"
    var updateFrequencyBiweekly: String = "Biweekly"
    var updateFrequencyMonthly: String = "Monthly"
    var updateFailed: (String) -> String = { a -> "Update failed: $a" }
    var newVersionAvailable: String = "New version available!"
    var alreadyLatestVersion: String = "Already the latest version"
    var permissionDenied: String = "Permission denied"
    var downloadGalleryFirst: String = "Please download the gallery first!"
    var exportAsArchive: String = "Export as archive"
    var exportAsArchiveSuccess: String = "Export succeed"
    var exportAsArchiveFailed: String = "Export failed"
    var prefCropBorders: String = "Crop borders"
    var actionSettings: String = "Settings"
    var prefRotationType: String = "Default rotation type"
    var viewer: String = "Reading mode"
    var actionMenu: String = "Menu"
    var navZonePrev: String = "Prev"
    var navZoneNext: String = "Next"
    var navZoneLeft: String = "Left"
    var navZoneRight: String = "Right"
    var decodeImageError: String = "The image couldn't be loaded"
    var actionRetry: String = "Retry"
    var labelDefault: String = "Default"
    var rotationFree: String = "Free"
    var rotationPortrait: String = "Portrait"
    var rotationReversePortrait: String = "Reverse portrait"
    var rotationLandscape: String = "Landscape"
    var rotationForcePortrait: String = "Locked portrait"
    var rotationForceLandscape: String = "Locked landscape"
    var leftToRightViewer: String = "Left to right"
    var rightToLeftViewer: String = "Right to left"
    var verticalViewer: String = "Vertical"
    var webtoonViewer: String = "Webtoon"
    var verticalPlusViewer: String = "Continuous vertical"
    var pagerViewer: String = "Paged"
    var prefFullscreen: String = "Fullscreen"
    var prefCutoutShort: String = "Show content in cutout area"
    var prefPageTransitions: String = "Animate page transitions"
    var prefShowPageNumber: String = "Show page number"
    var prefShowReaderSeekbar: String = "Show page jumping seekbar"
    var prefDoubleTapToZoom: String = "Double tap to zoom"
    var prefCustomBrightness: String = "Custom brightness"
    var prefGrayscale: String = "Grayscale"
    var prefInvertedColors: String = "Inverted"
    var prefCustomColorFilter: String = "Custom color filter"
    var prefColorFilterMode: String = "Color filter blend mode"
    var filterModeMultiply: String = "Multiply"
    var filterModeScreen: String = "Screen"
    var filterModeOverlay: String = "Overlay"
    var filterModeLighten: String = "Dodge / Lighten"
    var filterModeDarken: String = "Burn / Darken"
    var prefKeepScreenOn: String = "Keep screen on"
    var prefReadWithTappingInverted: String = "Invert tap zones"
    var tappingInvertedNone: String = "None"
    var tappingInvertedHorizontal: String = "Horizontal"
    var tappingInvertedVertical: String = "Vertical"
    var tappingInvertedBoth: String = "Both"
    var prefReadWithLongTap: String = "Show on long tap"
    var prefReaderTheme: String = "Background color"
    var whiteBackground: String = "White"
    var grayBackground: String = "Gray"
    var blackBackground: String = "Black"
    var automaticBackground: String = "Auto"
    var lNav: String = "L shaped"
    var kindlishNav: String = "Kindle-ish"
    var edgeNav: String = "Edge"
    var rightAndLeftNav: String = "Right and Left"
    var disabledNav: String = "Disabled"
    var prefViewerNav: String = "Tap zones"
    var prefImageScaleType: String = "Scale type"
    var scaleTypeFitScreen: String = "Fit screen"
    var scaleTypeStretch: String = "Stretch"
    var scaleTypeFitWidth: String = "Fit width"
    var scaleTypeFitHeight: String = "Fit height"
    var scaleTypeOriginalSize: String = "Original size"
    var scaleTypeSmartFit: String = "Smart fit"
    var prefNavigatePan: String = "Pan wide images when tapping"
    var prefLandscapeZoom: String = "Zoom landscape image"
    var prefZoomStart: String = "Zoom start position"
    var zoomStartAutomatic: String = "Automatic"
    var zoomStartLeft: String = "Left"
    var zoomStartRight: String = "Right"
    var zoomStartCenter: String = "Center"
    var rotationType: String = "Rotation type"
    var prefCategoryReadingMode: String = "Reading mode"
    var prefWebtoonSidePadding: String = "Side padding"
    var webtoonSidePadding0: String = "None"
    var webtoonSidePadding5: String = "5%"
    var webtoonSidePadding10: String = "10%"
    var webtoonSidePadding15: String = "15%"
    var webtoonSidePadding20: String = "20%"
    var webtoonSidePadding25: String = "25%"
    var prefCategoryGeneral: String = "General"
    var customFilter: String = "Custom filter"
    var actionShare: String = "Share"
    var actionCopy: String = "Copy"
    var actionSave: String = "Save"
    var actionSaveTo: String = "Save to…"
    var wideColorGamut: String = "Use Display P3 color space"
    var settingsEhRequestNewsTimepicker: String = "Set time to request news"
    var darkThemeFollowSystem: String = "Follow system"
    var darkThemeOff: String = "Always off"
    var darkThemeOn: String = "Always on"
    var blockedImage: String = "Blocked image"
    var showBlockedImage: String = "Show blocked image"
    var pageCount: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "$quantity page"
            else -> "$quantity pages"
        }
    }
    var someMinutesAgo: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "A minute ago"
            else -> "$quantity minutes ago"
        }
    }
    var someHoursAgo: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "An hour ago"
            else -> "$quantity hours ago"
        }
    }
    var second: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "sec"
            else -> "secs"
        }
    }
    var minute: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "min"
            else -> "mins"
        }
    }
    var hour: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "hour"
            else -> "hours"
        }
    }
    var day: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "day"
            else -> "days"
        }
    }
    var year: (quantity: Int) -> String = { quantity ->
        when (quantity) {
            1 -> "year"
            else -> "years"
        }
    }
}

val EnStrings = Strings()

@Suppress("ktlint:standard:property-naming")
object Locales {
    const val En = "en"
    const val De = "de"
    const val Es = "es"
    const val Fr = "fr"
    const val Ja = "ja"
    const val Ko = "ko"
    const val NbNo = "nb-NO"
    const val Th = "th"
    const val Tr = "tr"
    const val ZhCn = "zh-CN"
    const val ZhHk = "zh-HK"
    const val ZhTw = "zh-TW"
}

val Translations = mapOf(
    Locales.En to EnStrings,
    Locales.De to DeStrings,
    Locales.Es to EsStrings,
    Locales.Fr to FrStrings,
    Locales.Ja to JaStrings,
    Locales.Ko to KoStrings,
    Locales.NbNo to NbNoStrings,
    Locales.Th to ThStrings,
    Locales.Tr to TrStrings,
    Locales.ZhCn to ZhCnStrings,
    Locales.ZhHk to ZhHkStrings,
    Locales.ZhTw to ZhTwStrings,
)

val LocalStrings: ProvidableCompositionLocal<Strings> =
    staticCompositionLocalOf { EnStrings }

@Composable
fun ProvideTranslations(content: @Composable () -> Unit) = ProvideStrings(
    rememberStrings(
        translations = Translations,
        currentLanguageTag = Locale.current.resolveLanguageTag(),
    ),
    provider = LocalStrings,
    content = content,
)

fun getStrings(locale: Locale = Locale.current) = Translations[locale.resolveLanguageTag()] ?: EnStrings

fun Locale.resolveLanguageTag() = when (val language = language) {
    "zh" -> when (script) {
        "Hans" -> Locales.ZhCn
        else -> when (region) {
            "HK" -> Locales.ZhHk
            "MO", "TW" -> Locales.ZhTw
            else -> Locales.ZhCn
        }
    }
    "nb" -> Locales.NbNo
    else -> language
}
