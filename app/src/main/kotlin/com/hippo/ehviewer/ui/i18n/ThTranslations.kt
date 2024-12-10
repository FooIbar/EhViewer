package com.hippo.ehviewer.ui.i18n

object ThTranslations : Translations by EnTranslations {
    override val homepage = "หน้าหลัก"

    override val whatsHot = "เรื่องที่กำลังมาแรงมากๆ"

    override val favourite = "รายการโปรด"

    override val history = "ประวัติเข้าชม"

    override val downloads = "ดาวน์โหลด"

    override val settings = "การตั้งค่า"

    override val username = "ชื่อผู้ใช้"

    override val password = "รหัสผ่าน"

    override val signIn = "ลงชื่อเข้าใช้"

    override val register = "ลงทะเบียน"

    override val signInViaWebview = "ลงชื่อเข้าใช้ผ่านมุมมองเว็บไซต์"

    override val signInFirst = "โปรดลงชื่อเข้าใช้ก่อนเป็นอันดับแรก"

    override val textIsEmpty = "ข้อความห้ามเว้นว่างไว้"

    override val waring = "คำเตือน"

    override val invalidDownloadLocation = "ดูเหมือนที่อยู่ในการดาวน์โหลดนั้นไม่พร้อมใช้งาน โปรดตั้งค่าในเมนูการตั้งค่า"

    override val clipboardGalleryUrlSnackMessage = "มีลิงค์ของเว็บแกลเลอรี่อยู่ในคลิปบอร์ด"

    override val clipboardGalleryUrlSnackAction = "ดู"

    override val errorTimeout = "หมดเวลา"

    override val errorUnknownHost = "ไม่รู้จักโฮสต์"

    override val errorRedirection = "มีการเปลี่ยนทางเยอะเกินไป"

    override val errorSocket = "เน็ตเวิร์คเกิดข้อผิดพลาด"

    override val errorUnknown = "มันแปลกๆนะ"

    override val errorCantFindActivity = "หาแอปพลิเคชั่นไม่เจอ"

    override val errorCannotParseTheUrl = "ไม่สามารถวิเคราะห์ URL"

    override val errorDecodingFailed = "การถอดรหัสล้มเหลว"

    override val errorReadingFailed = "การอ่านล้มเหลว"

    override val errorOutOfRange = "ไม่อยู่ในขอบเขต"

    override val errorParseError = "การวิเคราะห์ผิดพลาด"

    override val error509 = "509"

    override val errorInvalidUrl = "URL ไม่ถูกต้อง"

    override val errorGetPtokenError = "การรับ pToken มีปัญหา"

    override val errorCantSaveImage = "ไม่สามารถบันทึกรูปภาพได้"

    override val errorInvalidNumber = "จำนวนไม่ถูกต้อง"

    override val appWaring = "เนื้อหาภายในแอปพลิเคชั่นนี้มาจากอินเทอร์เน็ตทั้งหมด อาจมีเนื้อหาบางอย่างที่มีผลต่อร่างกายหรือจิตใจของคุณ คุณจะต้องยอมรับความเสี่ยงที่ดังกล่าว และดำเนินการต่อไป"

    override val errorUsernameCannotEmpty = "ชื่อผู้ใช้ห้ามเว้นว่างไว้"

    override val errorPasswordCannotEmpty = "รหัสผ่านห้ามเว้นว่างไว้"

    override val guestMode = "ดำเนินการต่อโดยไม่ต้องลงชื่อเข้าระบบ"

    override val signInFailed = "การลงชื่อเข้าใช้ล้มเหลว"

    override val getIt = "เข้าใจแล้ว ไม่ต้องแสดงอีก"

    override val galleryListSearchBarHintExhentai = "ค้นหาใน ExHentai"

    override val galleryListSearchBarHintEHentai = "ค้นหาใน E-Hentai"

    override val galleryListSearchBarOpenGallery = "เปิดแกลเลอรี่"

    override val galleryListEmptyHit = "โลกนั้นนะมันกว้างงงงงใหญ่ไพรศาล และแพนด้านั่งโดดเดี่ยวเดียวดายในโลกกลมๆ"

    override val keywordSearch = "คีย์เวิร์ดในการค้นหา"

    override val imageSearch = "ค้นหาด้วยรูป"

    override val searchImage = "ค้นหาด้วยรูปภาพ"

    override val searchSh = "แสดงแกลเลอรี่ที่ถูกลบออก"

    override val searchSto = "แสดงผลเฉพาะแกลเลอรี่ที่มีไฟล์ Torrents"

    override val searchSr = "เรตติ้งขั้นต่ำ"

    override val selectImage = "เลือกรูปภาพ"

    override val selectImageFirst = "โปรดเลือกรูปภาพก่อน"

    override val addToFavourites = "เพิ่มเข้าในรายการโปรด"

    override val quickSearch = "ค้นหาอย่างด่วน"

    override val quickSearchTip = "แตะ \"+\" เพื่อเพิ่มการค้นหาอย่างด่วน"

    override val addQuickSearchDialogTitle = "เพิ่มค้นหาอย่างด่วน"

    override val nameIsEmpty = "ชื่อห้ามเว้นว่างไว้"

    override val delete = "ลบ"

    override val addQuickSearchTip = "รายการของแกลเลอรี่ต่างๆจะถูกบันทึกเป็นการค้นหาอย่างด่วน ต้องทำการค้นหาก่อนเพื่อบันทึกลงในรายการของแผงการค้นหา"

    override val readme = "อ่านก่อนสักนิด"

    override val imageSearchNotQuickSearch = "ไม่สามารถเพิ่มรูปในการค้นหาให้เป็นการค้นหาอย่างด่วนได้"

    override val duplicateQuickSearch = { p0: String ->
        "มีตัวค้นหาอย่างด่วนอยู่แล้วในชื่อ \"%s\"."
            .format(p0)
    }

    override val duplicateName = "ชื่อนี้มีการใช้งานอยู่แล้ว"

    override val goToHint = { p0: Int, p1: Int ->
        "หน้า %d, มีทั้งหมด %d หน้า"
            .format(p0, p1)
    }

    override val star2 = "2 ดาว"

    override val star3 = "3 ดาว"

    override val star4 = "4 ดาว"

    override val star5 = "5 ดาว"

    override val download = "ดาวน์โหลด"

    override val read = "อ่าน"

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

    override val share = "แชร์"

    override val rate = "คะแนน"

    override val similarGallery = "เหมือน"

    override val searchCover = "ค้นหาปก"

    override val noTags = "ไม่มีแท็ก"

    override val noComments = "ไม่มีความคิดเห็น"

    override val noMoreComments = "ไม่มีความเห็นเพิ่มเติมแล้ว"

    override val moreComment = "ความคิดเห็นเพิ่มเติม"

    override val refresh = "รีเฟรช"

    override val openInOtherApp = "เปิดด้วยแอปอื่น"

    override val rateSuccessfully = "ให้คะแนนเรียบร้อยแล้ว"

    override val rateFailed = "การให้คะแนนล้มเหลว"

    override val noTorrents = "ไม่มีไฟล์ torrents"

    override val torrents = "Torrents"

    override val notFavorited = "ยังไม่เพิ่มรายการโปรด"

    override val addFavoritesDialogTitle = "เพิ่มเข้ารายการโปรด"

    override val addToFavoriteSuccess = "เพิ่มเข้าในรายการโปรดแล้ว"

    override val removeFromFavoriteSuccess = "ลบออกจากรายการโปรดแล้ว"

    override val addToFavoriteFailure = "เกิดความล้มเหลวในการเพิ่มเข้าในรายการโปรด"

    override val removeFromFavoriteFailure = "เกิดข้อล้มเหลวในการลบออกจากรายการโปรด"

    override val rating10 = "นี้มันผลงานมาสเตอร์พีคชัดๆ!"

    override val rating9 = "อะเมซิ่งมากๆ!"

    override val rating8 = "เยี่ยม!"

    override val rating7 = "ดี!"

    override val rating6 = "ก็โอเคนะ"

    override val rating5 = "ก็ไม่แย่นะ"

    override val rating4 = "แย่"

    override val rating3 = "แย่มาก/กลัวแล้ว"

    override val rating2 = "โอ๊ยยย~"

    override val rating1 = "ไม่ไหวแล้วนะ!"

    override val rating0 = "พังพินาศสุดๆไปเลย!"

    override val galleryInfo = "ข้อมูลแกลเลอรี่"

    override val copiedToClipboard = "คัดลอกลงคลิปบอร์ดแล้ว"

    override val keyGid = "Gid"

    override val keyToken = "โทเคน"

    override val keyUrl = "URL"

    override val keyTitle = "ชื่อเรื่อง"

    override val keyTitleJpn = "ชื่อเรื่องญี่ปุ่น"

    override val keyThumb = "ภาพตัวอย่าง"

    override val keyCategory = "หมวดหมู่"

    override val keyUploader = "ผู้อัพโหลด"

    override val keyPosted = "โพสต์เมื่อ"

    override val keyParent = "Parent"

    override val keyVisible = "แสดง"

    override val keyLanguage = "ภาษา"

    override val keyPages = "หน้า"

    override val keySize = "ขนาดไฟล์"

    override val keyFavoriteCount = "จำนวนรายการโปรด"

    override val keyFavorited = "เพิ่มรายการโปรด"

    override val keyRatingCount = "คนให้คะแนน"

    override val keyRating = "คะแนน"

    override val keyTorrents = "ไฟล์ Torrents"

    override val keyTorrentUrl = "Torrent URL"

    override val galleryComments = "การแสดงความคิดเห็นในแกลเลอรี่"

    override val commentSuccessfully = "แสดงความคิดเห็นเสร็จเรียบร้อยแล้ว"

    override val commentFailed = "การแสดงความคิดเห็นล้มเหลว"

    override val copyCommentText = "คัดลอกข้อความของความคิดเห็น"

    override val voteUp = "ถูกใจ"

    override val cancelVoteUp = "ยกเลิกโหวตถูกใจ"

    override val voteDown = "ไม่ถูกใจ"

    override val cancelVoteDown = "ยกเลิกโหวตไม่ถูกใจ"

    override val voteUpSuccessfully = "โหวดถูกใจแล้ว"

    override val cancelVoteUpSuccessfully = "ยกเลิกโหวตถูกใจแล้ว"

    override val voteDownSuccessfully = "โหวดไม่ถูกใจเสร็จแล้ว"

    override val cancelVoteDownSuccessfully = "ยกเลิกโหวดไม่ถูกใจแล้ว"

    override val voteFailed = "การโหวตล้มเหลว"

    override val checkVoteStatus = "ดูรายละเอียดการโหวต"

    override val goTo = "ไปที่"

    override val sceneDownloadTitle = { p0: String ->
        "ดาวน์โหลด - %s"
            .format(p0)
    }

    override val noDownloadInfo = "รายการดาวน์โหลดจะอยู่ตรงนี้"

    override val downloadStateNone = "Idle"

    override val downloadStateWait = "กำลังรอ"

    override val downloadStateDownloading = "กำลังดาวน์โหลด"

    override val downloadStateDownloaded = "ดาวน์โหลดแล้ว"

    override val downloadStateFailed = "ล้มเหลว"

    override val downloadStateFailed2 = { p0: Int ->
        "%d ที่ไม่เสร็จ"
            .format(p0)
    }

    override val downloadStateFinish = "เสร็จ"

    override val stat509AlertTitle = "คำเตือน 509"

    override val stat509AlertText = "การลิมิตของรูปภาพต่อวันถึงลิมิตแล้ว กรุณาหยุดโหลดและพักผ่อนบ้างนะ"

    override val statDownloadDoneTitle = "ดาวน์โหลดเสร็จสิ้น"

    override val statDownloadDoneTextSucceeded = { p0: Int ->
        "%d ที่เสร็จ"
            .format(p0)
    }

    override val statDownloadDoneTextFailed = { p0: Int ->
        "%d ที่ล้มเหลว"
            .format(p0)
    }

    override val statDownloadDoneTextMix = { p0: Int, p1: Int ->
        "%d ที่เสร็จ, %d ที่ล้มเหลว"
            .format(p0, p1)
    }

    override val statDownloadDoneLineSucceeded = { p0: String ->
        "เสร็จแล้ว: %s"
            .format(p0)
    }

    override val statDownloadDoneLineFailed = { p0: String ->
        "ล้มเหลว: %s"
            .format(p0)
    }

    override val downloadRemoveDialogTitle = "ลบรายการดาวน์โหลด"

    override val downloadRemoveDialogMessage = { p0: String ->
        "ต้องการลบ %s ออกจากรายการดาวน์โหลดไหม?"
            .format(p0)
    }

    override val downloadRemoveDialogMessage2 = { p0: Int ->
        "ต้องการลบ %d รายการจากรายการดาวน์โหลดไหม?"
            .format(p0)
    }

    override val downloadRemoveDialogCheckText = "ลบไฟล์ภาพด้วย"

    override val statDownloadActionStopAll = "หยุดทั้งหมด"

    override val defaultDownloadLabelName = "ค่าเริ่มต้น"

    override val downloadMoveDialogTitle = "ย้าย"

    override val downloadLabels = "ป้ายชื่อดาวน์โหลด"

    override val downloadStartAll = "เริ่มทั้งหมด"

    override val downloadStopAll = "หยุดทั้งหมด"

    override val downloadResetReadingProgress = "รีเซ็ตตำแหน่งที่อ่าน"

    override val resetReadingProgressMessage = "รีเซ็ตตำแหน่งที่อ่านค้างไว้ทั้งหมดในแกลเลอรี่ที่โหลดไว้ทั้งหมดหรือไม่?"

    override val downloadServiceLabel = "บริการดาวน์โหลดของ EhViewer"

    override val downloadSpeedText = { p0: String ->
        "%s"
            .format(p0)
    }

    override val downloadSpeedText2 = { p0: String, p1: String ->
        "%s, %s left"
            .format(p0, p1)
    }

    override val rememberDownloadLabel = "จดจำป้ายชื่อดาวน์โหลด"

    override val defaultDownloadLabel = "ป้ายชื่อดาวน์โหลดเริ่มต้น"

    override val addedToDownloadList = "เพิ่มเข้าในรายการดาวน์โหลดแล้ว"

    override val add = "เพิ่ม"

    override val newLabelTitle = "ป้ายชื่ออันใหม่"

    override val labelTextIsEmpty = "ข้อความป้ายชื่อว่างอยู่"

    override val labelTextIsInvalid = "ชื่อ \"ค่าเริ่มต้น\" นั้นไม่ถูกต้อง"

    override val labelTextExist = "มีป้ายชื่อนี้อยู่แล้ว"

    override val renameLabelTitle = "เปลี่ยนชื่อ"

    override val noHistory = "ประวัติการเข้าชมแกลเลอรี่จะอยู่ตรงนี้"

    override val clearAll = "ลบทั้งหมด"

    override val clearAllHistory = "จะลบประวัติทั้งหมดเลยหรือไม่?"

    override val filterTitle = "ชื่อเรื่อง"

    override val filterUploader = "ผู้อัพโหลด"

    override val filterTag = "แท็ก"

    override val filterTagNamespace = "ชื่อของแท็ก"

    override val showDefinition = "แสดงความหมาย"

    override val uConfig = "การตั้งค่า EHentai"

    override val applyTip = "แตะเครื่องหมายถูกเพื่อบันทึกการตั้งค่าแล้ว"

    override val shareImage = "แชร์รูปภาพ"

    override val imageSaved = { p0: String ->
        "ภาพถูกบันทึกไปที่ %s"
            .format(p0)
    }

    override val settingsEh = "EH"

    override val settingsEhSignOut = "ออกจากระบบ"

    override val settingsEhIdentityCookiesSigned = "คุกกี๊ยืนยันตัวตนสามารถใช้ลงชื่อเข้าสู่ระบบสำหรับบัญชีนี้เท่านั้น<br><b>เก็บ รักษา ให้ ปลอดภัย</b>"

    override val settingsEhIdentityCookiesGuest = "ยังไม่ลงชื่อเข้าสู่ระบบ"

    override val settingsUConfig = "การตั้งค่า EHentai"

    override val settingsUConfigSummary = "การตั้งค่าบนเว็บไซต์ EHentai"

    override val settingsEhGallerySite = "เว็บไซต์หลักของแกลเลอรี่"

    override val settingsEhListMode = "มุมองแบบรายการ"

    override val settingsEhListModeDetail = "แสดงรายละเอียด"

    override val settingsEhListModeThumb = "ภาพตัวอย่าง"

    override val settingsEhDetailSize = "ขนาดของรายละเอียด"

    override val settingsEhDetailSizeLong = "อย่างยาว"

    override val settingsEhDetailSizeShort = "อย่างสั้น"

    override val settingsEhShowJpnTitle = "แสดงชื่อเรื่องญี่ปุ่น"

    override val settingsEhShowGalleryPages = "แสดงจำนวนหน้าของแกลเลอรี่"

    override val settingsEhShowGalleryPagesSummary = "แสดงจำนวนหน้าที่มีอยู่ในแต่ละแกลเลอรี่"

    override val settingsEhShowTagTranslations = "แสดงแท็กการแปลภาษา"

    override val settingsEhShowTagTranslationsSummary = "แสดงแท็กการแปลภาษาแทนที่ข้อความต้นฉบับ (อาจใช้เวลามากขึ้นเพื่อดาวน์โหลดไฟล์ข้อมูล)"

    override val settingsEhTagTranslationsSource = "Placeholder"

    override val settingsEhTagTranslationsSourceUrl = "https://placeholder"

    override val settingsDownload = "การดาวน์โหลด"

    override val settingsDownloadDownloadLocation = "ตำแหน่งแหล่งดาวน์โหลด"

    override val settingsDownloadCantGetDownloadLocation = "ไม่สามารถเข้าถึงแหล่งดาวน์โหลดได้"

    override val settingsDownloadMediaScan = "การอนุญาตให้สแกนสื่อ"

    override val settingsDownloadMediaScanSummaryOn = "โปรดซ่อนแอปแกลเลอรี่หรือแอปดูรูปในเครื่องของคุณจากคนอื่นด้วย"

    override val settingsDownloadMediaScanSummaryOff = "แอปแกลเลอรี่ส่วนใหญ่จะข้ามการสแกนรูปภาพในแหล่งดาวน์โหลด"

    override val settingsDownloadConcurrency = "ดาวน์โหลดแบบหลายเธด"

    override val settingsDownloadConcurrencySummary = { p0: String ->
        "โหลดถึง %s ภาพ"
            .format(p0)
    }

    override val settingsDownloadPreloadImage = "โหลดภาพล่วงหน้า"

    override val settingsDownloadPreloadImageSummary = { p0: String ->
        "โหลดภาพล่วงหน้าไป %s ภาพ"
            .format(p0)
    }

    override val settingsDownloadDownloadOriginImage = "ดาวน์โหลดรูปภาพต้นฉบับ"

    override val settingsDownloadDownloadOriginImageSummary = "อันตรายนะ! คุณอาจจะเจอรหัสข้อผิดพลาด 509 ได้"

    override val settingsDownloadRestoreDownloadItems = "คืนรายการดาวน์โหลด"

    override val settingsDownloadRestoreDownloadItemsSummary = "คืนรายการดาวน์โหลดทั้งหมดในแหล่งดาวน์โหลด"

    override val settingsDownloadRestoreNotFound = "ไม่พบรายการดาวน์โหลดที่จะคืนค่า"

    override val settingsDownloadRestoreFailed = "การคืนค่าล้มเหลว"

    override val settingsDownloadRestoreSuccessfully = { p0: Int ->
        "การคืนค่า %d รายการสำเร็จแล้ว"
            .format(p0)
    }

    override val settingsDownloadCleanRedundancy = "ลบการดาวน์โหลดที่ไม่จำเป็น"

    override val settingsDownloadCleanRedundancySummary = "ลบรูปภาพแกลเลอรี่ที่ไม่ได้อยู่ในรายการดาวน์โหลด แต่อยู่ในแหล่งดาวน์โหลด"

    override val settingsDownloadCleanRedundancyNoRedundancy = "ไม่มีสิ่งที่ไม่จำเป็น"

    override val settingsDownloadCleanRedundancyDone = { p0: Int ->
        "การลบสิ่งที่ไม่จำเป็นเสร็จสิ้น ลบไปแล้วทั้งหมด %d ชิ้น"
            .format(p0)
    }

    override val settingsAdvanced = "ขั้นสูง"

    override val settingsAdvancedSaveParseErrorBody = "บันทึกเนื้อหาลง HTML เมื่อการวิเคราะห์ผิดพลาด"

    override val settingsAdvancedSaveParseErrorBodySummary = "เนื้อหาแบบ HTML อาจจะเป็นข้อมูลความเป็นส่วนตัวที่มีความอ่อนไหวได้"

    override val settingsAdvancedSaveCrashLog = "บันทึกข้อผิดพลาดเมื่อเกิด crash"

    override val settingsAdvancedSaveCrashLogSummary = "การบันทึกข้อผิดพลาดจาก crash จะช่วยให้ผู้พัฒนาหาและแก้ไขบัคต่างๆได้ง่าย"

    override val settingsAdvancedDumpLogcat = "สร้าง Logcat"

    override val settingsAdvancedDumpLogcatSummary = "บันทึก Logcat ไปที่พื้นที่จัดเก็บภายนอก"

    override val settingsAdvancedDumpLogcatFailed = "การสร้าง Logcat เกิดข้อผิดพลาด"

    override val settingsAdvancedDumpLogcatTo = { p0: String ->
        "Logcat ถูกสร้างอยู่ที่ %s"
            .format(p0)
    }

    override val settingsAdvancedReadCacheSize = "ขนาดในการเก็บไฟล์ชั่วคราวเพื่ออ่าน"

    override val settingsAdvancedAppLanguageTitle = "ภาษาของแอป"

    override val settingsAdvancedExportData = "สำเนาข้อมูล"

    override val settingsAdvancedExportDataSummary = "บันทึกข้อมูลต่างๆเช่นรายการดาวน์โหลด และค้นหาอย่างด่วนลงพื้นที่จัดเก็บภายนอก"

    override val settingsAdvancedExportDataTo = { p0: String ->
        "ได้สำเนาข้อมูลแล้วจัดเก็บที่ %s"
            .format(p0)
    }

    override val settingsAdvancedExportDataFailed = "การสำเนาข้อมูลล้มเหลว"

    override val settingsAdvancedImportData = "นำเข้าข้อมูล"

    override val settingsAdvancedImportDataSummary = "ดึงข้อมูลต่างๆจากไฟล์สำเนาที่ทำมาก่อนหน้านี้"

    override val settingsAdvancedImportDataSuccessfully = "การนำเข้าข้อมูลสำเร็จ"

    override val settingsAbout = "เกี่ยวกับ"

    override val settingsAboutDeclarationSummary = "EhViewer ไม่มีความแตกต่างจากตัวเว็บ E-Hentai.org แต่อย่างใด"

    override val settingsAboutAuthor = "นักพัฒนา"

    override val settingsAboutSource = "ซอร์สโค้ด"

    override val settingsAboutVersion = "เวอร์ชั่นแอป"

    override val settingsAboutCheckForUpdates = "ตรวจสอบการอัพเดต"

    override val cantReadTheFile = "ไม่สามารถอ่านไฟล์ได้"

    override val appLanguageSystem = "ภาษาของระบบ (ค่าเริ่มต้น)"

    override val pleaseWait = "รอสักครู่"

    override val cloudFavorites = "รายการโปรดบนเว็บ"

    override val localFavorites = "รายการโปรดบนเครื่อง"

    override val searchBarHint = { p0: String ->
        "ค้นหา %s"
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

    override val deleteFavoritesDialogTitle = "ลบออกจากรายการโปรด"

    override val deleteFavoritesDialogMessage = { p0: Int ->
        "ต้องการลบ %d รายการออกจากรายการโปรดหรือไม่?"
            .format(p0)
    }

    override val moveFavoritesDialogTitle = "ย้ายรายการโปรด"

    override val defaultFavoritesCollection = "ค่าเริ่มต้นของที่เก็บรายการโปรด"

    override val letMeSelect = "ขอให้ฉันเลือกนะ"

    override val collections = "คอลเลคชั่น"

    override val errorSomethingWrongHappened = "มีสิ่งผิดปกติบางอย่างเกิดขึ้น"

    override val fromTheFuture = "จากอนาคต"

    override val justNow = "เดี๋ยวนี้"

    override val yesterday = "เมื่อวาน"

    override val someDaysAgo = { p0: Int ->
        "%d วันที่ผ่านมา"
            .format(p0)
    }

    override val archive = "การบีบอัด"

    override val noArchives = "ยังไม่มีไฟล์บีบอัด"

    override val downloadArchiveStarted = "เริ่มการดาวน์โหลดบีบอัด"

    override val downloadArchiveFailure = "การดาวน์โหลดไฟล์บีบอัดล้มเหลว"

    override val downloadArchiveFailureNoHath = "ต้องการ H@H client เพื่อโหลดไฟล์บีบอัด"

    override val settingsPrivacy = "ความเป็นส่วนตัว"

    override val settingsPrivacySecure = "ป้องกันการบันทึกภาพหน้าจอ"

    override val settingsPrivacySecureSummary = "ป้องกันเนื้อหาต่างๆของตัวแอปจากการถูกบันทึกหน้าจอ หรือถูกแสดงในหน้ารายการ \"แอพล่าสุด\" ตอนสลับหรือระหว่างสลับไปแอปอื่น."

    override val downloadService = "บริการดาวน์โหลด"

    override val favoriteName = "รายการโปรด"

    override val pageCount = { quantity: Int ->
        when (quantity) {
            else -> "%d หน้า"
        }.format(quantity)
    }

    override val someMinutesAgo = { quantity: Int ->
        when (quantity) {
            else -> "%d นาทีที่ผ่านมา"
        }.format(quantity)
    }

    override val someHoursAgo = { quantity: Int ->
        when (quantity) {
            else -> "%d ชั่วโมงที่ผ่านมา"
        }.format(quantity)
    }

    override val second = { quantity: Int ->
        when (quantity) {
            else -> "วิ"
        }.format(quantity)
    }

    override val minute = { quantity: Int ->
        when (quantity) {
            else -> "นาที"
        }.format(quantity)
    }

    override val hour = { quantity: Int ->
        when (quantity) {
            else -> "ชั่วโมง"
        }.format(quantity)
    }

    override val day = { quantity: Int ->
        when (quantity) {
            else -> "วัน"
        }.format(quantity)
    }

    override val year = { quantity: Int ->
        when (quantity) {
            else -> "ปี"
        }.format(quantity)
    }
}