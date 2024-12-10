package com.hippo.ehviewer.ui.i18n

object JaTranslations : Translations by EnTranslations {
    override val homepage = "ホーム"
    override val subscription = "購読"
    override val whatsHot = "人気"
    override val favourite = "お気に入り"
    override val history = "履歴"
    override val downloads = "ダウンロード"
    override val settings = "設定"
    override val username = "ユーザー名"
    override val password = "パスワード"
    override val signIn = "ログイン"
    override val register = "登録"
    override val signInViaWebview = "ウェブでログイン"
    override val signInFirst = "ログインしてください"
    override val textIsEmpty = "テキストを入力してください"
    override val waring = "警告"
    override val invalidDownloadLocation = "ダウンロードパスは現在利用できません。ダウンロードパスを設定してください。"
    override val clipboardGalleryUrlSnackMessage = "クリップボードからギャラリーの URL が検出されました。"
    override val clipboardGalleryUrlSnackAction = "表示"
    override val errorTimeout = "タイムアウト"
    override val errorUnknownHost = "不明なホスト"
    override val errorRedirection = "リダイレクトが多すぎます"
    override val errorSocket = "ネットワークエラー"
    override val errorUnknown = "不明なエラー"
    override val errorCantFindActivity = "アクティビティが見つかりません。"
    override val errorCannotParseTheUrl = "リンクを解析できません"
    override val errorDecodingFailed = "デコードに失敗しました"
    override val errorReadingFailed = "読み込めませんでした"
    override val errorOutOfRange = "範囲外"
    override val errorParseError = "ファイルの解析に失敗しました"
    override val error509 = "509 エラー"
    override val errorInvalidUrl = "無効なリンクです"
    override val errorGetPtokenError = "pToken 取得エラー"
    override val errorCantSaveImage = "画像を保存できませんでした"
    override val errorInvalidNumber = "無効な数字です"
    override val appWaring = "このアプリの内容はインターネットから取得したものです。その内容の一部は心身に悪影響を与える可能性があります。それでも続行しますか？"
    override val appWaring2 = "利用を続行することにより、上記の規約に同意したものとみなされます。"
    override val errorUsernameCannotEmpty = "ユーザー名は空欄にできません"
    override val errorPasswordCannotEmpty = "パスワードは空欄にできません"
    override val guestMode = "ゲストモード"
    override val signInFailed = "ログインに失敗しました"
    override val signInFailedTip = { a: String -> "この問題が続く場合は「$a」をお試しください。" }
    override val getIt = "了解"
    override val galleryListSearchBarHintExhentai = "ExHentai を検索"
    override val galleryListSearchBarHintEHentai = "E-Hentai を検索"
    override val galleryListSearchBarOpenGallery = "ギャラリーを開く"
    override val galleryListEmptyHit = "何も見つかりません"
    override val galleryListEmptyHitSubscription = "リストは空です。\n設定 -> EH -> マイタグでタグを購読できます。"
    override val keywordSearch = "キーワードで検索"
    override val imageSearch = "画像で検索"
    override val searchImage = "画像検索"
    override val searchSh = "削除済みのギャラリーを表示"
    override val searchSto = "Torrent のあるギャラリーのみを表示"
    override val searchSr = "評価の下限"
    override val searchSpTo = "から"
    override val searchSpErr1 = "ページ範囲の最大値を 10 以下にすることはできません"
    override val searchSpErr2 = "ページ範囲が狭すぎます"
    override val searchSpSuffix = ""
    override val searchSf = "デフォルトのフィルターで無効:"
    override val searchSfl = "言語"
    override val searchSfu = "アップローダー"
    override val searchSft = "タグ"
    override val selectImage = "画像を選択"
    override val selectImageFirst = "画像を選択してください"
    override val addToFavourites = "お気に入りに追加"
    override val removeFromFavourites = "お気に入りから削除"
    override val deleteDownloads = "ダウンロードを削除"
    override val quickSearch = "クイック検索"
    override val quickSearchTip = "「+」をタップしてクイック検索を追加します"
    override val addQuickSearchDialogTitle = "クイック検索を追加"
    override val nameIsEmpty = "タイトルが空欄です"
    override val delete = "削除"
    override val addQuickSearchTip = "ギャラリー検索結果の設定などはクイック検索として保存されます。検索の設定などを保存するには、まず検索を実行してください。"
    override val readme = "README"
    override val imageSearchNotQuickSearch = "画像検索をクイック検索として追加できませんでした"
    override val duplicateQuickSearch = { a: String -> "「$a」のクイック検索はすでに存在しています。" }
    override val duplicateName = "この名前はすでに使用されています。"
    override val saveProgress = "進捗を保存"
    override val deleteQuickSearch = { a: String -> "クイック検索の「$a」を削除しますか？" }
    override val goToHint = { a: Int, b: Int -> "ページ: $a - 合計: $b ページ" }
    override val any = "すべて"
    override val star2 = "2 つ星"
    override val star3 = "3 つ星"
    override val star4 = "4 つ星"
    override val star5 = "5 つ星"
    override val download = "ダウンロード"
    override val read = "読む"
    override val favoredTimes = { a: Int -> "\u2665 $a" }
    override val ratingText = { a: String, b: Int -> "%s (%.2f - %d)".format(a, b) }
    override val torrentCount = { a: Int -> "Torrent ($a)" }
    override val share = "共有"
    override val rate = "評価"
    override val similarGallery = "類似のギャラリー"
    override val searchCover = "カバーを検索"
    override val noTags = "タグなし"
    override val noComments = "コメントなし"
    override val noMoreComments = "コメントはこれ以上ありません"
    override val moreComment = "他のコメントを見る"
    override val refresh = "更新"
    override val viewOriginal = "オリジナルの画像を表示"
    override val openInOtherApp = "他のアプリで開く"
    override val clearImageCache = "画像キャッシュを消去"
    override val clearImageCacheConfirm = "このギャラリーのすべての画像キャッシュを消去しますか？"
    override val imageCacheCleared = "画像キャッシュを消去しました"
    override val rateSuccessfully = "評価しました"
    override val rateFailed = "評価できませんでした"
    override val noTorrents = "Torrent なし"
    override val torrents = "Torrent"
    override val notFavorited = "お気に入り未追加"
    override val addFavoritesDialogTitle = "お気に入りに追加"
    override val addToFavoriteSuccess = "お気に入りに追加しました"
    override val removeFromFavoriteSuccess = "お気に入りから削除しました"
    override val addToFavoriteFailure = "お気に入りの追加に失敗しました"
    override val removeFromFavoriteFailure = "お気に入りからの削除に失敗しました"
    override val filterTheUploader = { a: String -> "アップローダーの「$a」をブロックしますか？" }
    override val filterTheTag = { a: String -> "「$a」のタグをブロックしますか？" }
    override val filterAdded = "ブロックを追加しました"
    override val newerVersionAvailable = "このギャラリーの新しいバージョンが利用可能です。"
    override val newerVersionTitle = { a: String, b: String -> "$a、$b を追加しました" }
    override val rating10 = "ものすごくいい"
    override val rating9 = "すごくいい"
    override val rating8 = "とてもいい"
    override val rating7 = "いい"
    override val rating6 = "まあまあ"
    override val rating5 = "普通"
    override val rating4 = "悪い"
    override val rating3 = "とても悪い"
    override val rating2 = "目の障害になる"
    override val rating1 = "悪くて呼吸できない"
    override val rating0 = "…"
    override val galleryInfo = "ギャラリー情報"
    override val copiedToClipboard = "クリップボードにコピーしました"
    override val keyGid = "GID"
    override val keyToken = "トークン"
    override val keyUrl = "URL"
    override val keyTitle = "タイトル"
    override val keyTitleJpn = "日本語のタイトル"
    override val keyThumb = "サムネイル"
    override val keyCategory = "カテゴリー"
    override val keyUploader = "アップローダー"
    override val keyPosted = "アップロード日時"
    override val keyParent = "親ギャラリー"
    override val keyVisible = "可視性"
    override val keyLanguage = "言語"
    override val keyPages = "ページ数"
    override val keySize = "サイズ"
    override val keyFavoriteCount = "お気に入りの数"
    override val keyFavorited = "お気に入りに追加済み"
    override val keyRatingCount = "評価の数"
    override val keyRating = "評価"
    override val keyTorrents = "Torrent"
    override val keyTorrentUrl = "Torrent URL"
    override val galleryComments = "ギャラリーのコメント"
    override val commentSuccessfully = "コメントを送信しました"
    override val commentFailed = "コメントに失敗しました"
    override val copyCommentText = "コメントをコピー"
    override val blockCommenter = "コメントをしたユーザーをブロック"
    override val filterTheCommenter = { a: String -> "「$a」をブロックしますか？" }
    override val editComment = "コメントを編集"
    override val editCommentSuccessfully = "コメントが編集されました"
    override val editCommentFailed = "コメントの編集に失敗しました"
    override val voteUp = "高評価"
    override val cancelVoteUp = "高評価を取り消す"
    override val voteDown = "低評価"
    override val cancelVoteDown = "低評価を取り消す"
    override val voteUpSuccessfully = "高評価しました"
    override val cancelVoteUpSuccessfully = "高評価を取り消しました"
    override val voteDownSuccessfully = "低評価しました"
    override val cancelVoteDownSuccessfully = "低評価を取り消しました"
    override val voteFailed = "評価に失敗しました"
    override val checkVoteStatus = "評価の詳細を表示"
    override val clickMoreComments = "タップしてさらにコメントを読み込む"
    override val lastEdited = { a: String -> "最終更新: $a" }
    override val formatBold = "太字"
    override val formatItalic = "斜体"
    override val formatUnderline = "下線"
    override val formatStrikethrough = "打ち消し線"
    override val formatUrl = "URL"
    override val formatPlain = "プレーンテキスト"
    override val goTo = "ページに移動"
    override val sceneDownloadTitle = { a: String -> "ダウンロード - $a" }
    override val noDownloadInfo = "ダウンロードタスクはここに表示されます"
    override val downloadStateNone = "一時停止中"
    override val downloadStateWait = "待機中"
    override val downloadStateDownloading = "ダウンロード中"
    override val downloadStateDownloaded = "ダウンロード済み"
    override val downloadStateFailed = "ダウンロードエラー"
    override val downloadStateFailed2 = { a: Int -> "$a 件が未完成" }
    override val downloadStateFinish = "ダウンロード完了"
    override val stat509AlertTitle = "509 アラート"
    override val stat509AlertText =
        "ダウンロード制限に達しました。しばらくしてからもう一度やり直してください。"
    override val statDownloadDoneTitle = "ダウンロード完了"
    override val statDownloadDoneTextSucceeded = { p0: Int ->
        "%d 件が成功"
            .format(p0)
    }
    override val statDownloadDoneTextFailed = { p0: Int ->
        "%d 件が失敗"
            .format(p0)
    }
    override val statDownloadDoneTextMix = { p0: Int, p1: Int ->
        "%d 件のダウンロードが成功、%d 件が失敗"
            .format(p0, p1)
    }
    override val statDownloadDoneLineSucceeded = { p0: String ->
        "成功: %s"
            .format(p0)
    }
    override val statDownloadDoneLineFailed = { p0: String ->
        "失敗: %s"
            .format(p0)
    }
    override val downloadRemoveDialogTitle = "ダウンロードを削除"
    override val downloadRemoveDialogMessage = { p0: String ->
        "ダウンロードリストから「%s」を削除しますか？"
            .format(p0)
    }
    override val downloadRemoveDialogMessage2 = { p0: Int ->
        "ダウンロードリストから %d 件のタスクを削除しますか？"
            .format(p0)
    }
    override val downloadRemoveDialogCheckText = "画像ファイルを削除"
    override val statDownloadActionStopAll = "すべて停止"
    override val defaultDownloadLabelName = "デフォルト"
    override val downloadMoveDialogTitle = "移動"
    override val downloadLabels = "ダウンロードラベル"
    override val downloadStartAll = "すべて開始"
    override val downloadStopAll = "すべて停止"
    override val downloadResetReadingProgress = "閲覧進捗をリセット"
    override val resetReadingProgressMessage =
        "すべてのダウンロード済みのギャラリーの閲覧進捗をリセットしますか？"
    override val downloadServiceLabel = "EhViewer ダウンロードサービス"
    override val downloadSpeedText = { p0: String ->
        "%s"
            .format(p0)
    }
    override val downloadSpeedText2 = { p0: String, p1: String ->
        "%s - 残り: %s"
            .format(p0, p1)
    }
    override val rememberDownloadLabel = "ダウンロードラベルを記憶"
    override val defaultDownloadLabel = "デフォルトのダウンロードラベル"
    override val addedToDownloadList = "ダウンロードリストに追加しました"
    override val selectGroupingMode = "グループモードを選択"
    override val selectGroupingModeCustom = "カスタム"
    override val selectGroupingModeArtist = "アーティスト"
    override val unknownArtists = "不明"
    override val add = "追加"
    override val newLabelTitle = "ラベルを作成"
    override val labelTextIsEmpty = "ラベルテキストを入力してください"
    override val labelTextIsInvalid = "「デフォルト」は無効なラベルです"
    override val labelTextExist = "ラベルはすでに存在しています"
    override val renameLabelTitle = "ラベルの名前を変更"
    override val deleteLabel = { p0: String ->
        "「%s」のラベルを削除しますか？"
            .format(p0)
    }
    override val noHistory = "閲覧したギャラリーはここに表示されます"
    override val clearAll = "すべて消去"
    override val clearAllHistory = "履歴をすべて消去しますか？"
    override val filter = "ギャラリーをブロック"
    override val filterTitle = "タイトル"
    override val filterUploader = "アップローダー"
    override val filterTag = "タグ"
    override val filterTagNamespace = "タグの名前空間"
    override val filterCommenter = "コメント"
    override val filterComment = "コメントの正規表現"
    override val deleteFilter = { p0: String ->
        "「%s」のブロックを削除しますか？"
            .format(p0)
    }
    override val addFilter = "ブロックを追加"
    override val showDefinition = "ヘルプを表示"
    override val filterText = "テキストをブロック"
    override val filterTip =
        "E-Hentai のギャラリーリストからブロックしたものを除外します。\n\nタイトルをブロック: ブロックしたテキストを含むギャラリーを除外します。\n\nアップローダーをブロック: 該当するアップローダーを除外します。\n\nタグをブロック: 該当するタグを含むギャラリーを除外します。ギャラリーリストの取得に時間がかかる可能性があります。\n\nタグ名前空間をブロック: 該当するタグ名前空間を含むギャラリーを除外します。ギャラリーリストの取得に時間がかかる可能性があります。\n\nコメント投稿者をブロック: 該当するコメント投稿者が投稿したコメントを除外します。\n\nコメントをブロック: 正規表現に一致するコメントを除外します。"
    override val uConfig = "E-Hentai の設定"
    override val applyTip = "右上隅のチェックマークをタップして設定を保存します"
    override val myTags = "マイタグ"
    override val shareImage = "画像を共有"
    override val imageSaved = { p0: String ->
        "画像は「%s」に保存されました"
            .format(p0)
    }
    override val settingsEh = "EH"
    override val settingsEhSignOut = "ログアウト"
    override val settingsEhIdentityCookiesSigned =
        "このアカウントは Identity Cookie を使用してログインできます。<br><b>これは安全に保管してください</b>"
    override val settingsEhIdentityCookiesGuest = "ゲストモード"
    override val settingsEhClearIgneous = "Igneous を消去"
    override val settingsUConfig = "E-Hentai の設定"
    override val settingsUConfigSummary = "E-Hentai ウェブサイト内の設定をします"
    override val settingsMyTags = "マイタグ"
    override val settingsMyTagsSummary = "E-Hentai のウェブサイトでタグを管理します"
    override val settingsEhGallerySite = "ギャラリーサイト"
    override val settingsEhLaunchPage = "起動ページ"
    override val settingsEhListMode = "リストモード"
    override val settingsEhListModeDetail = "詳細"
    override val settingsEhListModeThumb = "サムネイル"
    override val settingsEhDetailSize = "詳細情報の幅"
    override val settingsEhDetailSizeLong = "長い"
    override val settingsEhDetailSizeShort = "短い"
    override val settingsEhThumbColumns = "サムネイルのカラム数"
    override val settingsEhForceEhThumb = "E-Hentai サムネイルサーバーを使用する"
    override val settingsEhForceEhThumbSummary =
        "サムネイルの読み込みに問題がある場合は無効化してください"
    override val settingsEhShowJpnTitle = "日本語のタイトルを表示"
    override val settingsEhShowJpnTitleSummary =
        "E-Hentai ウェブサイトの設定で日本語のタイトルを有効化する必要があります"
    override val settingsEhShowGalleryPages = "ギャラリーページ数を表示"
    override val settingsEhShowGalleryPagesSummary = "リストにギャラリーのページ数を表示します"
    override val settingsEhShowGalleryComments = "ギャラリーのコメントを表示"
    override val settingsEhShowGalleryCommentsSummary =
        "ギャラリーの詳細ページにコメントを表示します"
    override val settingsEhShowGalleryCommentThreshold = "コメントスコアのしきい値"
    override val settingsEhShowGalleryCommentThresholdSummary =
        "このスコア以下のコメントを非表示にします (-101 は無効)"
    override val settingsEhShowTagTranslations = "タグの翻訳を表示"
    override val settingsEhShowTagTranslationsSummary =
        "元のテキストの代わりに翻訳したタグを表示します (データファイルのダウンロードに時間がかかります)"
    override val settingsEhTagTranslationsSource = "プレースホルダー"
    override val settingsEhTagTranslationsSourceUrl = "https://placeholder"
    override val settingsEhFilter = "ギャラリーをブロック"
    override val settingsEhFilterSummary =
        "タイトル、アップローダー、タグ、コメントの投稿者またはギャラリーをブロックします"
    override val settingsBlockExtraneousAds = "[試験的] 関係のない広告をブロック"
    override val settingsAdsPlaceholder = "[任意] 広告を置換するプレースホルダーを選択"
    override val settingsDownload = "ダウンロード"
    override val settingsDownloadDownloadLocation = "ダウンロード先"
    override val settingsDownloadCantGetDownloadLocation = "ダウンロード先を取得できません"
    override val settingsDownloadMediaScan = "メディアスキャンを許可する"
    override val settingsDownloadMediaScanSummaryOn =
        "ギャラリーアプリで他の人に見せないようにします"
    override val settingsDownloadMediaScanSummaryOff =
        "ほとんどのギャラリーアプリでダウンロード先のパスを無視します"
    override val settingsDownloadConcurrency = "ダウンロードのスレッド数"
    override val settingsDownloadConcurrencySummary = { p0: String ->
        "同時に最大 %s 枚の画像をダウンロードします"
            .format(p0)
    }
    override val settingsDownloadDownloadDelay = "ダウンロードの遅延"
    override val settingsDownloadDownloadDelaySummary = { p0: String ->
        "ダウンロードで %s ミリ秒の遅延をさせます"
            .format(p0)
    }
    override val settingsDownloadDownloadTimeout = "ダウンロードのタイムアウト (秒単位)"
    override val settingsDownloadPreloadImage = "画像をプリロード"
    override val settingsDownloadPreloadImageSummary = { p0: String ->
        "%s 枚の画像をプリロードします"
            .format(p0)
    }
    override val settingsDownloadDownloadOriginImage = "オリジナルの画像をダウンロードする"
    override val settingsDownloadDownloadOriginImageSummary = "注意！GP が必要になる可能性があります"
    override val settingsDownloadSaveAsCbz = "CBZ アーカイブで保存"
    override val settingsDownloadArchiveMetadata = "アーカイブのメタデータ"
    override val settingsDownloadArchiveMetadataSummary =
        "アーカイブのダウンロード時に ComicInfo.xml を生成します"
    override val settingsDownloadReloadMetadata = "メタデータを再読み込み"
    override val settingsDownloadReloadMetadataSummary =
        "タグが変更された可能性のあるダウンロード項目の ComicInfo.xml を再生成します"
    override val settingsDownloadReloadMetadataSuccessfully = { p0: Int ->
        "%d 個の項目を再読み込みしました"
            .format(p0)
    }
    override val settingsDownloadReloadMetadataFailed = { p0: String ->
        "メタデータの再読み込みに失敗: %s"
            .format(p0)
    }
    override val settingsDownloadRestoreDownloadItems = "ダウンロードタスクを復元"
    override val settingsDownloadRestoreDownloadItemsSummary =
        "ダウンロードディレクトリのダウンロードタスクを復元します"
    override val settingsDownloadRestoreNotFound = "復元可能なダウンロードが見つかりません"
    override val settingsDownloadRestoreFailed = "復元できませんでした"
    override val settingsDownloadRestoreSuccessfully = { p0: Int ->
        "%d 件のタスクが復元されました"
            .format(p0)
    }
    override val settingsDownloadCleanRedundancy = "ダウンロードフォルダの不要なファイルを整理"
    override val settingsDownloadCleanRedundancySummary =
        "ダウンロードディレクトリからダウンロードタスクにない画像ファイルを削除します"
    override val settingsDownloadCleanRedundancyNoRedundancy =
        "不要なファイルが見つかりませんでした"
    override val settingsDownloadCleanRedundancyDone = { p0: Int ->
        "%d 件のファイルを削除しました"
            .format(p0)
    }
    override val settingsAdvanced = "その他の設定"
    override val settingsAdvancedSaveParseErrorBody = "解析の失敗時に HTML ファイルを保存"
    override val settingsAdvancedSaveParseErrorBodySummary =
        "HTML ファイルに個人情報が含まれている場合があります"
    override val settingsAdvancedSaveCrashLog = "アプリのクラッシュ時にレポートを保存"
    override val settingsAdvancedSaveCrashLogSummary = "クラッシュレポートはバグの修正に役立ちます"
    override val settingsAdvancedDumpLogcat = "Logcat をダンプ"
    override val settingsAdvancedDumpLogcatSummary = "Logcat のログを内部ストレージに保存します"
    override val settingsAdvancedDumpLogcatFailed = "Logcat のダンプに失敗しました"
    override val settingsAdvancedDumpLogcatTo = { p0: String ->
        "Logcat のログが「%s」にダンプされました"
            .format(p0)
    }
    override val settingsAdvancedReadCacheSize = "読書用キャッシュのサイズ"
    override val settingsAdvancedAppLanguageTitle = "アプリの言語"
    override val settingsAdvancedHardwareBitmapThreshold =
        "ハードウェアビットマップ (パフォーマンス向上) のしきい値"
    override val settingsAdvancedHardwareBitmapThresholdSummary =
        "長い画像の読み込みに失敗する場合はこれを減らしてみてください"
    override val settingsAdvancedExportData = "データをエクスポート"
    override val settingsAdvancedExportDataSummary =
        "ダウンロードリストやクイック検索などのデータを内部ストレージに保存します"
    override val settingsAdvancedExportDataTo = { p0: String ->
        "データを「%s」にエクスポート"
            .format(p0)
    }
    override val settingsAdvancedExportDataFailed = "データをエクスポートできませんでした"
    override val settingsAdvancedImportData = "データをインポート"
    override val settingsAdvancedImportDataSummary = "以前に保存したデータを読み込みます"
    override val settingsAdvancedImportDataSuccessfully = "データをインポートしました"
    override val settingsAdvancedBackupFavorite = "お気に入りリストをバックアップ"
    override val settingsAdvancedBackupFavoriteSummary =
        "リモートのお気に入りリストをローカルにバックアップします"
    override val settingsAdvancedBackupFavoriteStart = { p0: String ->
        "お気に入りリスト「%s」をバックアップ中です"
            .format(p0)
    }
    override val settingsAdvancedBackupFavoriteNothing = "バックアップするものがありません"
    override val settingsAdvancedBackupFavoriteSuccess =
        "お気に入りリストのバックアップに成功しました"
    override val settingsAdvancedBackupFavoriteFailed =
        "お気に入りリストのバックアップに失敗しました"
    override val settingsAbout = "このアプリについて"
    override val settingsAboutDeclarationSummary = "EhViewer は E-Hentai.org と一切関係はありません"
    override val settingsAboutAuthor = "開発者"
    override val settingsAboutLatestRelease = "最新のリリース"
    override val settingsAboutSource = "ソースコード"
    override val settingsAboutVersion = "ビルドバージョン"
    override val settingsAboutCommitTime = { p0: String ->
        "%s にコミットされました"
            .format(p0)
    }
    override val settingsAboutCheckForUpdates = "更新を確認"
    override val license = "ライセンス"
    override val cantReadTheFile = "ファイルを読み取れませんでした"
    override val appLanguageSystem = "システム言語 (デフォルト)"
    override val pleaseWait = "お待ちください"
    override val cloudFavorites = "クラウドのお気に入り"
    override val localFavorites = "ローカルのお気に入り"
    override val searchBarHint = { p0: String ->
        "%s を検索"
            .format(p0)
    }
    override val favoritesTitle = { p0: String ->
        "%s"
            .format(p0)
    }
    override val favoritesTitle2 = { a: String, b: String -> "$a - $b" }
    override val deleteFavoritesDialogTitle = "お気に入りから削除"
    override val deleteFavoritesDialogMessage = { a: Int -> "$a 件の項目をお気に入りリストから削除しますか？" }
    override val moveFavoritesDialogTitle = "お気に入りを移動"
    override val defaultFavoritesCollection = "デフォルトのお気に入りリスト"
    override val defaultFavoritesWarning = "これを有効化するとお気に入りのメモを追加することができなくなります"
    override val letMeSelect = "手動で選択"
    override val favoriteNote = "お気に入りのメモ"
    override val collections = "コレクション"
    override val errorSomethingWrongHappened = "エラーが発生しました"
    override val fromTheFuture = "未来から"
    override val justNow = "たった今"
    override val yesterday = "昨日"
    override val someDaysAgo = { a: Int -> "$a 日前" }
    override val archive = "圧縮パッケージ"
    override val noArchives = "アーカイブなし"
    override val downloadArchiveStarted = "アーカイブのダウンロードを開始しました"
    override val downloadArchiveFailure = "アーカイブをダウンロードできませんでした"
    override val archiveFree = "自由"
    override val archiveOriginal = "オリジナル"
    override val archiveResample = "リサンプル"
    override val downloadArchiveFailureNoHath = "アーカイブのダウンロードは H@H クライアントが必要です"
    override val currentFunds = "現在の資金:"
    override val insufficientFunds = "資金が不足しています"
    override val imageLimits = "画像の制限"
    override val imageLimitsSummary = "使用中:"
    override val imageLimitsNormal = "制限なし"
    override val imageLimitsRestricted = "画面解像度は 1280x に制限されます"
    override val resetCost = { a: Int -> "$a GP を使用してリセット" }
    override val reset = "リセット"
    override val settingsPrivacy = "プライバシー"
    override val settingsPrivacySecure = "スクリーンショットを抑制する"
    override val settingsPrivacySecureSummary = "アプリのコンテンツがスクリーンショットで撮影されたり「最近使用したアプリ」のリストに表示されないようにします"
    override val clearSearchHistory = "デバイスの検索履歴を消去"
    override val clearSearchHistorySummary = "このデバイスから検索履歴を消去します"
    override val clearSearchHistoryConfirm = "検索履歴を消去しますか？"
    override val searchHistoryCleared = "検索履歴を消去しました"
    override val downloadService = "ダウンロードサービス"
    override val keyFavoriteName = "お気に入り"
    override val darkTheme = "ダークテーマ"
    override val blackDarkTheme = "ブラックダークテーマ"
    override val harmonizeCategoryColor = "カテゴリの色をダイナミックカラーで調和させる"
    override val sortBy = "並べ替え"
    override val addedTimeDesc = "追加された時間 (昇順)"
    override val addedTimeAsc = "追加された時間 (昇順)"
    override val uploadedTimeDesc = "アップロードされた時間 (昇順)"
    override val uploadedTimeAsc = "アップロードされた時間 (昇順)"
    override val titleAsc = "タイトル (昇順)"
    override val titleDesc = "タイトル (降順)"
    override val pageCountAsc = "ページ数 (昇順)"
    override val pageCountDesc = "ページ数 (降順)"
    override val groupByDownloadLabel = "ダウンロードラベルでグループにする"
    override val downloadFilter = "フィルター"
    override val downloadAll = "すべて"
    override val downloadStartAllReversed = "すべて開始 (逆順)"
    override val noBrowserInstalled = "ブラウザをインストールしてください。"
    override val toplistAlltime = "すべての期間"
    override val toplistPastyear = "昨年"
    override val toplistPastmonth = "先月"
    override val toplistYesterday = "昨日"
    override val toplist = "トップリスト"
    override val tagVoteDown = "評価を下げる"
    override val tagVoteUp = "評価を上げる"
    override val tagVoteSuccessfully = "評価が成功しました"
    override val deleteSearchHistory = { a: String -> "検索履歴から「$a」を削除しますか？" }
    override val actionAddTag = "タグを追加"
    override val actionAddTagTip = "新しいタグを入力してください"
    override val commentUserUploader = { a: String -> "$a (アップローダー)" }
    override val noNetwork = "ネットワークがありません"
    override val settingsEhMeteredNetworkWarning = "従量制ネットワークの警告"
    override val meteredNetworkWarning = "従量制ネットワークに接続中です"
    override val readFrom = { a: Int -> "$a ページを読む" }
    override val settingsEhRequestNews = "時限リクエストのニュースページ"
    override val settingsEhHideHvEvents = "HV イベント通知を隠す"
    override val copyTrans = "翻訳をコピー"
    override val defaultDownloadDirNotEmpty = "デフォルトのダウンロードディレクトリが空ではありません！"
    override val resetDownloadLocation = "デフォルトにリセット"
    override val pickNewDownloadLocation = "新しい場所を選択"
    override val dontShowAgain = "今後表示しない"
    override val openSettings = "設定を開く"
    override val appLinkNotVerifiedMessage = "Android 12 以降の場合、EhViewer で E-Hentai のリンクを開くには確認済みのリンクを手動で追加する必要があります。"
    override val appLinkNotVerifiedTitle = "アプリのリンクが確認されていません"
    override val openByDefault = "デフォルトで開く"
    override val backupBeforeUpdate = "更新前にデータをバックアップする"
    override val useCiUpdateChannel = "CI 更新チャンネルを使用する"
    override val settingsPrivacyRequireUnlock = "ロックの解除を要求する"
    override val settingsPrivacyRequireUnlockDelay = "ロックの遅延"
    override val settingsPrivacyRequireUnlockDelaySummary = { a: String -> "$a 分以内にアプリに戻る場合はロックの解除を要求しません" }
    override val settingsPrivacyRequireUnlockDelaySummaryImmediately = "このアプリに戻るたびにロックの解除を要求します"
    override val filterLabel = "ブロッカーのタイプ"
    override val archivePasswd = "パスワード"
    override val archiveNeedPasswd = "アーカイブにはパスワードが必要です"
    override val passwdWrong = "パスワードが違います"
    override val passwdCannotBeEmpty = "パスワードは空欄にできません"
    override val listTileThumbSize = "詳細モードのサムネイルの大きさ"
    override val accountName = "アカウント"
    override val preloadThumbAggressively = "サムネイルを積極的にプリロードする"
    override val animateItems = "リスト項目のアニメーション"
    override val animateItemsSummary = "クラッシュやフレームドロップが発生する場合はこれを無効化してください"
    override val autoUpdates = "自動で更新を確認する"
    override val updateFrequencyNever = "しない"
    override val updateFrequencyDaily = "毎日"
    override val updateFrequency3days = "3 日ごと"
    override val updateFrequencyWeekly = "毎週"
    override val updateFrequencyBiweekly = "隔週"
    override val updateFrequencyMonthly = "毎月"
    override val updateFailed = { a: String -> "更新に失敗: $a" }
    override val newVersionAvailable = "新しいバージョンがあります！"
    override val alreadyLatestVersion = "すでに最新のバージョンです"
    override val permissionDenied = "権限がありません"
    override val downloadGalleryFirst = "まずはギャラリーをダウンロードしてください！"
    override val exportAsArchive = "アーカイブでエクスポート"
    override val exportAsArchiveSuccess = "エクスポートが成功しました"
    override val exportAsArchiveFailed = "エクスポートに失敗しました"
    override val prefCropBorders = "境界線をクロップ"
    override val actionSettings = "設定"
    override val prefRotationType = "デフォルトの回転タイプ"
    override val viewer = "読書モード"
    override val actionMenu = "メニュー"
    override val navZonePrev = "前へ"
    override val navZoneNext = "次へ"
    override val navZoneLeft = "左"
    override val navZoneRight = "右"
    override val decodeImageError = "画像を読み込めませんでした"
    override val actionRetry = "再試行"
    override val labelDefault = "デフォルト"
    override val rotationFree = "自由"
    override val rotationPortrait = "縦方向"
    override val rotationReversePortrait = "縦方向 (反転)"
    override val rotationLandscape = "横方向"
    override val rotationForcePortrait = "縦方向 (固定)"
    override val rotationForceLandscape = "横方向 (固定)"
    override val leftToRightViewer = "左から右"
    override val rightToLeftViewer = "右から左"
    override val verticalViewer = "垂直"
    override val webtoonViewer = "ウェブトゥーン"
    override val verticalPlusViewer = "連続した垂直"
    override val pagerViewer = "ページ"
    override val prefFullscreen = "全画面"
    override val prefCutoutShort = "カットアウト領域にコンテンツを表示"
    override val prefPageTransitions = "ページ遷移アニメーション"
    override val prefShowPageNumber = "ページ番号を表示"
    override val prefShowReaderSeekbar = "ページジャンプシークバーを表示"
    override val prefDoubleTapToZoom = "ダブルタップで拡大"
    override val prefCustomBrightness = "明るさをカスタマイズ"
    override val prefGrayscale = "グレースケール"
    override val prefInvertedColors = "色の反転"
    override val prefCustomColorFilter = "カスタムカラーフィルター"
    override val prefColorFilterMode = "カラーフィルターブレンドモード"
    override val filterModeMultiply = "乗算"
    override val filterModeScreen = "画面"
    override val filterModeOverlay = "オーバーレイ"
    override val filterModeLighten = "覆い焼き / 明るい"
    override val filterModeDarken = "焼き込み / 暗い"
    override val prefKeepScreenOn = "常に画面を ON にする"
    override val prefReadWithTappingInverted = "タップゾーンを反転"
    override val tappingInvertedNone = "なし"
    override val tappingInvertedHorizontal = "横"
    override val tappingInvertedVertical = "縦"
    override val tappingInvertedBoth = "両方"
    override val prefReadWithLongTap = "長押しで表示"
    override val prefReaderTheme = "背景の色"
    override val whiteBackground = "ホワイト"
    override val grayBackground = "グレー"
    override val blackBackground = "ブラック"
    override val automaticBackground = "自動"
    override val lNav = "L 字型"
    override val kindlishNav = "Kindle-ish"
    override val edgeNav = "エッジ"
    override val rightAndLeftNav = "右と左"
    override val disabledNav = "無効"
    override val prefViewerNav = "タップゾーン"
    override val prefImageScaleType = "大きさのタイプ"
    override val scaleTypeFitScreen = "画面に合わせる"
    override val scaleTypeStretch = "ストレッチ"
    override val scaleTypeFitWidth = "幅に合わせる"
    override val scaleTypeFitHeight = "高さに合わせる"
    override val scaleTypeOriginalSize = "オリジナルのサイズ"
    override val scaleTypeSmartFit = "スマートフィット"
    override val prefNavigatePan = "タップでワイドに画像をパンする"
    override val prefLandscapeZoom = "横方向の画像を拡大"
    override val prefZoomStart = "拡大の開始位置"
    override val zoomStartAutomatic = "自動"
    override val zoomStartLeft = "左"
    override val zoomStartRight = "右"
    override val zoomStartCenter = "中央"
    override val rotationType = "回転のタイプ"
    override val prefCategoryReadingMode = "読書モード"
    override val prefWebtoonSidePadding = "両端の余白"
    override val webtoonSidePadding0 = "なし"
    override val webtoonSidePadding5 = "5%"
    override val webtoonSidePadding10 = "10%"
    override val webtoonSidePadding15 = "15%"
    override val webtoonSidePadding20 = "20%"
    override val webtoonSidePadding25 = "25%"
    override val prefCategoryGeneral = "一般"
    override val customFilter = "カスタムフィルター"
    override val actionShare = "共有"
    override val actionCopy = "コピー"
    override val actionSave = "保存"
    override val actionSaveTo = "名前を付けて保存…"
    override val wideColorGamut = "Display P3 カラースペースを使用する"
    override val settingsEhRequestNewsTimepicker = "ニュースをリクエストする時間を設定"
    override val darkThemeFollowSystem = "システムに従う"
    override val darkThemeOff = "常に OFF にする"
    override val darkThemeOn = "常に ON にする"
    override val blockedImage = "ブロックされた画像"
    override val showBlockedImage = "ブロックされた画像を表示"
    override val pageCount = { a: Int -> "$a ページ" }
    override val someMinutesAgo = { a: Int -> "$a 分前" }
    override val someHoursAgo = { a: Int -> "$a 時間前" }
    override val second = { _: Int -> "秒" }
    override val minute = { _: Int -> "分" }
    override val hour = { _: Int -> "時" }
    override val day = { _: Int -> "日" }
    override val year = { _: Int -> "年" }
}
