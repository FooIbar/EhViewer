package com.hippo.ehviewer.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hippo.ehviewer.R
import org.xmlpull.v1.XmlPullParser

fun Context.getLanguages(): Map<String, String> {
    val languages = mutableMapOf("system" to getString(R.string.app_language_system))
    resources.getXml(R.xml._generated_res_locale_config).use { parser ->
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "locale") {
                val tag = parser.getAttributeValue(0).removeSuffix("-US")
                LocaleListCompat.forLanguageTags(tag)[0]?.let { locale ->
                    languages[tag] = locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
                }
            }
            event = parser.next()
        }
    }
    return languages
}

fun getAppLanguage(): String {
    val locale = AppCompatDelegate.getApplicationLocales()[0]
    return when (val language = locale?.language) {
        null -> "system"
        "zh", "nb" -> "$language-${locale.country}"
        else -> language
    }
}

fun setAppLanguage(language: String) {
    val locales = if (language == "system") {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(language)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
