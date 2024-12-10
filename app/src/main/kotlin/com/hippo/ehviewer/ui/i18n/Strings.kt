@file:Suppress("ktlint:standard:property-naming")

package com.hippo.ehviewer.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings

interface Strings {
    val appName: String
    val siteE: String
    val siteEx: String
    val doujinshi: String
    val manga: String
    val artistCg: String
    val gameCg: String
    val western: String
    val nonH: String
    val imageSet: String
    val cosplay: String
    val asianPorn: String
    val misc: String
    val homepage: String
    val subscription: String
    val whatsHot: String
    val favourite: String
    val history: String
    val downloads: String
    val settings: String
    val username: String
    val password: String
    val signIn: String
    val register: String
    val signInViaWebview: String
    val signInFirst: String
    val textIsEmpty: String
    val waring: String
    val invalidDownloadLocation: String
    val clipboardGalleryUrlSnackMessage: String
    val clipboardGalleryUrlSnackAction: String
    val errorTimeout: String
    val errorUnknownHost: String
    val errorRedirection: String
    val errorSocket: String
    val errorUnknown: String
    val errorCantFindActivity: String
    val errorCannotParseTheUrl: String
    val errorDecodingFailed: String
    val errorReadingFailed: String
    val errorOutOfRange: String
    val errorParseError: String
    val error509: String
    val errorInvalidUrl: String
    val errorGetPtokenError: String
    val errorCantSaveImage: String
    val errorInvalidNumber: String
    val appWaring: String
    val appWaring2: String
    val errorUsernameCannotEmpty: String
    val errorPasswordCannotEmpty: String
    val guestMode: String
    val signInFailed: String
    val signInFailedTip: (String) -> String
    val getIt: String
    val galleryListSearchBarHintExhentai: String
    val galleryListSearchBarHintEHentai: String
    val galleryListSearchBarOpenGallery: String
    val galleryListEmptyHit: String
    val galleryListEmptyHitSubscription: String
    val keywordSearch: String
    val imageSearch: String
    val searchImage: String
    val searchSh: String
    val searchSto: String
    val searchSr: String
    val searchSpTo: String
    val searchSpErr1: String
    val searchSpErr2: String
    val searchSpSuffix: String
    val searchSf: String
    val searchSfl: String
    val searchSfu: String
    val searchSft: String
    val selectImage: String
    val selectImageFirst: String
    val addToFavourites: String
    val removeFromFavourites: String
    val deleteDownloads: String
    val quickSearch: String
    val quickSearchTip: String
    val addQuickSearchDialogTitle: String
    val translateTagForTagger: String
    val nameIsEmpty: String
    val delete: String
    val addQuickSearchTip: String
    val readme: String
    val imageSearchNotQuickSearch: String
    val duplicateQuickSearch: (String) -> String
    val duplicateName: String
    val saveProgress: String
    val deleteQuickSearch: (String) -> String
    val goToHint: (Int, Int) -> String
    val any: String
    val star2: String
    val star3: String
    val star4: String
    val star5: String
    val download: String
    val read: String
    val favoredTimes: (Int) -> String
    val ratingText: (String, Float, Int) -> String
    val torrentCount: (Int) -> String
    val share: String
    val rate: String
    val similarGallery: String
    val searchCover: String
    val noTags: String
    val noComments: String
    val noMoreComments: String
    val moreComment: String
    val refresh: String
    val viewOriginal: String
    val openInOtherApp: String
    val clearImageCache: String
    val clearImageCacheConfirm: String
    val imageCacheCleared: String
    val rateSuccessfully: String
    val rateFailed: String
    val noTorrents: String
    val torrents: String
    val notFavorited: String
    val addFavoritesDialogTitle: String
    val addToFavoriteSuccess: String
    val removeFromFavoriteSuccess: String
    val addToFavoriteFailure: String
    val removeFromFavoriteFailure: String
    val filterTheUploader: (String) -> String
    val filterTheTag: (String) -> String
    val filterAdded: String
    val newerVersionAvailable: String
    val newerVersionTitle: (String, String) -> String
    val rating10: String
    val rating9: String
    val rating8: String
    val rating7: String
    val rating6: String
    val rating5: String
    val rating4: String
    val rating3: String
    val rating2: String
    val rating1: String
    val rating0: String
    val ratingNone: String
    val galleryInfo: String
    val copiedToClipboard: String
    val keyGid: String
    val keyToken: String
    val keyUrl: String
    val keyTitle: String
    val keyTitleJpn: String
    val keyThumb: String
    val keyCategory: String
    val keyUploader: String
    val keyPosted: String
    val keyParent: String
    val keyVisible: String
    val keyLanguage: String
    val keyPages: String
    val keySize: String
    val keyFavoriteCount: String
    val keyFavorited: String
    val keyRatingCount: String
    val keyRating: String
    val keyTorrents: String
    val keyTorrentUrl: String
    val galleryComments: String
    val commentSuccessfully: String
    val commentFailed: String
    val copyCommentText: String
    val blockCommenter: String
    val filterTheCommenter: (String) -> String
    val editComment: String
    val editCommentSuccessfully: String
    val editCommentFailed: String
    val voteUp: String
    val cancelVoteUp: String
    val voteDown: String
    val cancelVoteDown: String
    val voteUpSuccessfully: String
    val cancelVoteUpSuccessfully: String
    val voteDownSuccessfully: String
    val cancelVoteDownSuccessfully: String
    val voteFailed: String
    val checkVoteStatus: String
    val clickMoreComments: String
    val lastEdited: (String) -> String
    val goTo: String
    val sceneDownloadTitle: (String) -> String
    val noDownloadInfo: String
    val downloadStateNone: String
    val downloadStateWait: String
    val downloadStateDownloading: String
    val downloadStateDownloaded: String
    val downloadStateFailed: String
    val downloadStateFailed2: (Int) -> String
    val downloadStateFinish: String
    val stat509AlertTitle: String
    val stat509AlertText: String
    val statDownloadDoneTitle: String
    val statDownloadDoneTextSucceeded: (Int) -> String
    val statDownloadDoneTextFailed: (Int) -> String
    val statDownloadDoneTextMix: (Int, Int) -> String
    val statDownloadDoneLineSucceeded: (String) -> String
    val statDownloadDoneLineFailed: (String) -> String
    val downloadRemoveDialogTitle: String
    val downloadRemoveDialogMessage: (String) -> String
    val downloadRemoveDialogMessage2: (Int) -> String
    val downloadRemoveDialogCheckText: String
    val statDownloadActionStopAll: String
    val defaultDownloadLabelName: String
    val downloadMoveDialogTitle: String
    val downloadLabels: String
    val downloadStartAll: String
    val downloadStopAll: String
    val downloadResetReadingProgress: String
    val resetReadingProgressMessage: String
    val downloadServiceLabel: String
    val downloadSpeedText: (String) -> String
    val downloadSpeedText2: (String, String) -> String
    val rememberDownloadLabel: String
    val defaultDownloadLabel: String
    val addedToDownloadList: String
    val selectGroupingMode: String
    val selectGroupingModeCustom: String
    val selectGroupingModeArtist: String
    val unknownArtists: String
    val add: String
    val newLabelTitle: String
    val labelTextIsEmpty: String
    val labelTextIsInvalid: String
    val labelTextExist: String
    val renameLabelTitle: String
    val deleteLabel: (String) -> String
    val noHistory: String
    val clearAll: String
    val clearAllHistory: String
    val filter: String
    val filterTitle: String
    val filterUploader: String
    val filterTag: String
    val filterTagNamespace: String
    val filterCommenter: String
    val filterComment: String
    val deleteFilter: (String) -> String
    val addFilter: String
    val showDefinition: String
    val filterText: String
    val filterTip: String
    val uConfig: String
    val applyTip: String
    val myTags: String
    val shareImage: String
    val imageSaved: (String) -> String
    val settingsEh: String
    val settingsEhSignOut: String
    val settingsEhIdentityCookiesSigned: String
    val settingsEhIdentityCookiesGuest: String
    val settingsEhClearIgneous: String
    val settingsUConfig: String
    val settingsUConfigSummary: String
    val settingsMyTags: String
    val settingsMyTagsSummary: String
    val settingsEhGallerySite: String
    val settingsEhLaunchPage: String
    val settingsEhListMode: String
    val settingsEhListModeDetail: String
    val settingsEhListModeThumb: String
    val settingsEhDetailSize: String
    val settingsEhDetailSizeLong: String
    val settingsEhDetailSizeShort: String
    val settingsEhThumbColumns: String
    val settingsEhForceEhThumb: String
    val settingsEhForceEhThumbSummary: String
    val settingsEhShowJpnTitle: String
    val settingsEhShowJpnTitleSummary: String
    val settingsEhShowGalleryPages: String
    val settingsEhShowGalleryPagesSummary: String
    val settingsEhShowVoteStatus: String
    val settingsEhShowGalleryComments: String
    val settingsEhShowGalleryCommentsSummary: String
    val settingsEhShowGalleryCommentThreshold: String
    val settingsEhShowGalleryCommentThresholdSummary: String
    val settingsEhShowTagTranslations: String
    val settingsEhShowTagTranslationsSummary: String
    val settingsEhTagTranslationsSource: String
    val settingsEhTagTranslationsSourceUrl: String
    val settingsEhFilter: String
    val settingsEhFilterSummary: String
    val settingsReadReverseControls: String
    val settingsBlockExtraneousAds: String
    val settingsAdsPlaceholder: String
    val settingsDownload: String
    val settingsDownloadDownloadLocation: String
    val settingsDownloadCantGetDownloadLocation: String
    val settingsDownloadMediaScan: String
    val settingsDownloadMediaScanSummaryOn: String
    val settingsDownloadMediaScanSummaryOff: String
    val settingsDownloadConcurrency: String
    val settingsDownloadConcurrencySummary: (Int) -> String
    val settingsDownloadDownloadDelay: String
    val settingsDownloadDownloadDelaySummary: (Int) -> String
    val settingsDownloadDownloadTimeout: String
    val settingsDownloadPreloadImage: String
    val settingsDownloadPreloadImageSummary: (Int) -> String
    val settingsDownloadDownloadOriginImage: String
    val settingsDownloadDownloadOriginImageSummary: String
    val settingsDownloadSaveAsCbz: String
    val settingsDownloadArchiveMetadata: String
    val settingsDownloadArchiveMetadataSummary: String
    val settingsDownloadReloadMetadata: String
    val settingsDownloadReloadMetadataSummary: String
    val settingsDownloadReloadMetadataSuccessfully: (Int) -> String
    val settingsDownloadReloadMetadataFailed: (String) -> String
    val settingsDownloadRestoreDownloadItems: String
    val settingsDownloadRestoreDownloadItemsSummary: String
    val settingsDownloadRestoreNotFound: String
    val settingsDownloadRestoreFailed: String
    val settingsDownloadRestoreSuccessfully: (Int) -> String
    val settingsDownloadCleanRedundancy: String
    val settingsDownloadCleanRedundancySummary: String
    val settingsDownloadCleanRedundancyNoRedundancy: String
    val settingsDownloadCleanRedundancyDone: (Int) -> String
    val settingsAdvanced: String
    val settingsAdvancedSaveParseErrorBody: String
    val settingsAdvancedSaveParseErrorBodySummary: String
    val settingsAdvancedSaveCrashLog: String
    val settingsAdvancedSaveCrashLogSummary: String
    val settingsAdvancedDumpLogcat: String
    val settingsAdvancedDumpLogcatSummary: String
    val settingsAdvancedDumpLogcatFailed: String
    val settingsAdvancedDumpLogcatTo: (String) -> String
    val settingsAdvancedReadCacheSize: String
    val settingsAdvancedAppLanguageTitle: String
    val settingsAdvancedHardwareBitmapThreshold: String
    val settingsAdvancedHardwareBitmapThresholdSummary: String
    val settingsAdvancedExportData: String
    val settingsAdvancedExportDataSummary: String
    val settingsAdvancedExportDataTo: (String) -> String
    val settingsAdvancedExportDataFailed: String
    val settingsAdvancedImportData: String
    val settingsAdvancedImportDataSummary: String
    val settingsAdvancedImportDataSuccessfully: String
    val settingsAdvancedBackupFavorite: String
    val settingsAdvancedBackupFavoriteSummary: String
    val settingsAdvancedBackupFavoriteStart: (String) -> String
    val settingsAdvancedBackupFavoriteNothing: String
    val settingsAdvancedBackupFavoriteSuccess: String
    val settingsAdvancedBackupFavoriteFailed: String
    val settingsAbout: String
    val settingsAboutDeclaration: String
    val settingsAboutDeclarationSummary: String
    val settingsAboutAuthor: String
    val settingsAboutAuthorSummary: AnnotatedString
    val settingsAboutLatestRelease: String
    val settingsAboutSource: String
    val settingsAboutVersion: String
    val settingsAboutCommitTime: (String) -> String
    val settingsAboutCheckForUpdates: String
    val license: String
    val cantReadTheFile: String
    val appLanguageSystem: String
    val pleaseWait: String
    val cloudFavorites: String
    val localFavorites: String
    val searchBarHint: (String) -> String
    val favoritesTitle: (String) -> String
    val favoritesTitle2: (String, String) -> String
    val deleteFavoritesDialogTitle: String
    val deleteFavoritesDialogMessage: (Int) -> String
    val moveFavoritesDialogTitle: String
    val defaultFavoritesCollection: String
    val defaultFavoritesWarning: String
    val letMeSelect: String
    val favoriteNote: String
    val collections: String
    val errorSomethingWrongHappened: String
    val fromTheFuture: String
    val justNow: String
    val yesterday: String
    val someDaysAgo: (Int) -> String
    val archive: String
    val archiveFree: String
    val archiveOriginal: String
    val archiveResample: String
    val noArchives: String
    val downloadArchiveStarted: String
    val downloadArchiveFailure: String
    val downloadArchiveFailureNoHath: String
    val currentFunds: String
    val insufficientFunds: String
    val imageLimits: String
    val imageLimitsSummary: String
    val imageLimitsNormal: String
    val imageLimitsRestricted: String
    val resetCost: (Int) -> String
    val reset: String
    val settingsPrivacy: String
    val settingsPrivacySecure: String
    val settingsPrivacySecureSummary: String
    val clearSearchHistory: String
    val clearSearchHistorySummary: String
    val clearSearchHistoryConfirm: String
    val searchHistoryCleared: String
    val downloadService: String
    val keyFavoriteName: String
    val darkTheme: String
    val blackDarkTheme: String
    val harmonizeCategoryColor: String
    val sortBy: String
    val addedTimeDesc: String
    val addedTimeAsc: String
    val uploadedTimeDesc: String
    val uploadedTimeAsc: String
    val titleAsc: String
    val titleDesc: String
    val pageCountAsc: String
    val pageCountDesc: String
    val groupByDownloadLabel: String
    val downloadFilter: String
    val downloadAll: String
    val downloadStartAllReversed: String
    val noBrowserInstalled: String
    val toplistAlltime: String
    val toplistPastyear: String
    val toplistPastmonth: String
    val toplistYesterday: String
    val toplist: String
    val tagVoteDown: String
    val tagVoteUp: String
    val tagVoteWithdraw: String
    val tagVoteSuccessfully: String
    val deleteSearchHistory: (String) -> String
    val actionAddTag: String
    val actionAddTagTip: String
    val commentUserUploader: (String) -> String
    val noNetwork: String
    val settingsEhMeteredNetworkWarning: String
    val meteredNetworkWarning: String
    val readFrom: (Int) -> String
    val settingsEhRequestNews: String
    val settingsEhHideHvEvents: String
    val copyTrans: String
    val defaultDownloadDirNotEmpty: String
    val resetDownloadLocation: String
    val pickNewDownloadLocation: String
    val dontShowAgain: String
    val openSettings: String
    val appLinkNotVerifiedMessage: String
    val appLinkNotVerifiedTitle: String
    val openByDefault: String
    val backupBeforeUpdate: String
    val useCiUpdateChannel: String
    val settingsPrivacyRequireUnlock: String
    val settingsPrivacyRequireUnlockDelay: String
    val settingsPrivacyRequireUnlockDelaySummary: (Int) -> String
    val settingsPrivacyRequireUnlockDelaySummaryImmediately: String
    val filterLabel: String
    val archivePasswd: String
    val archiveNeedPasswd: String
    val passwdWrong: String
    val passwdCannotBeEmpty: String
    val listTileThumbSize: String
    val accountName: String
    val preloadThumbAggressively: String
    val animateItems: String
    val animateItemsSummary: String
    val autoUpdates: String
    val updateFrequencyNever: String
    val updateFrequencyDaily: String
    val updateFrequency3days: String
    val updateFrequencyWeekly: String
    val updateFrequencyBiweekly: String
    val updateFrequencyMonthly: String
    val updateFailed: (String) -> String
    val newVersionAvailable: String
    val alreadyLatestVersion: String
    val permissionDenied: String
    val downloadGalleryFirst: String
    val exportAsArchive: String
    val exportAsArchiveSuccess: String
    val exportAsArchiveFailed: String
    val prefCropBorders: String
    val actionSettings: String
    val prefRotationType: String
    val viewer: String
    val actionMenu: String
    val navZonePrev: String
    val navZoneNext: String
    val navZoneLeft: String
    val navZoneRight: String
    val decodeImageError: String
    val actionRetry: String
    val labelDefault: String
    val rotationFree: String
    val rotationPortrait: String
    val rotationReversePortrait: String
    val rotationLandscape: String
    val rotationForcePortrait: String
    val rotationForceLandscape: String
    val leftToRightViewer: String
    val rightToLeftViewer: String
    val verticalViewer: String
    val webtoonViewer: String
    val verticalPlusViewer: String
    val pagerViewer: String
    val prefFullscreen: String
    val prefCutoutShort: String
    val prefPageTransitions: String
    val prefShowPageNumber: String
    val prefShowReaderSeekbar: String
    val prefDoubleTapToZoom: String
    val prefCustomBrightness: String
    val prefGrayscale: String
    val prefInvertedColors: String
    val prefCustomColorFilter: String
    val prefColorFilterMode: String
    val filterModeMultiply: String
    val filterModeScreen: String
    val filterModeOverlay: String
    val filterModeLighten: String
    val filterModeDarken: String
    val prefKeepScreenOn: String
    val prefReadWithTappingInverted: String
    val tappingInvertedNone: String
    val tappingInvertedHorizontal: String
    val tappingInvertedVertical: String
    val tappingInvertedBoth: String
    val prefReadWithLongTap: String
    val prefReaderTheme: String
    val whiteBackground: String
    val grayBackground: String
    val blackBackground: String
    val automaticBackground: String
    val lNav: String
    val kindlishNav: String
    val edgeNav: String
    val rightAndLeftNav: String
    val disabledNav: String
    val prefViewerNav: String
    val prefImageScaleType: String
    val scaleTypeFitScreen: String
    val scaleTypeStretch: String
    val scaleTypeFitWidth: String
    val scaleTypeFitHeight: String
    val scaleTypeOriginalSize: String
    val scaleTypeSmartFit: String
    val prefNavigatePan: String
    val prefLandscapeZoom: String
    val prefZoomStart: String
    val zoomStartAutomatic: String
    val zoomStartLeft: String
    val zoomStartRight: String
    val zoomStartCenter: String
    val rotationType: String
    val prefCategoryReadingMode: String
    val prefWebtoonSidePadding: String
    val webtoonSidePadding0: String
    val webtoonSidePadding5: String
    val webtoonSidePadding10: String
    val webtoonSidePadding15: String
    val webtoonSidePadding20: String
    val webtoonSidePadding25: String
    val prefCategoryGeneral: String
    val customFilter: String
    val actionShare: String
    val actionCopy: String
    val actionSave: String
    val actionSaveTo: String
    val wideColorGamut: String
    val settingsEhRequestNewsTimepicker: String
    val darkThemeFollowSystem: String
    val darkThemeOff: String
    val darkThemeOn: String
    val blockedImage: String
    val showBlockedImage: String
    val pageCount: (quantity: Int) -> String
    val someMinutesAgo: (quantity: Int) -> String
    val someHoursAgo: (quantity: Int) -> String
    val second: (quantity: Int) -> String
    val minute: (quantity: Int) -> String
    val hour: (quantity: Int) -> String
    val day: (quantity: Int) -> String
    val year: (quantity: Int) -> String
}

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
