package com.hippo.ehviewer.client.parser

import android.os.Parcelable
import com.hippo.ehviewer.client.exception.InsufficientFundsException
import com.hippo.ehviewer.client.exception.ParseException
import java.nio.ByteBuffer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

object HomeParser {
    private val PATTERN_FUNDS = Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private const val INSUFFICIENT_FUNDS = "Insufficient funds."

    fun parse(body: ByteBuffer) = runCatching {
        unmarshalParsingAs<Limits>(body, ::parseLimit)
    }.getOrElse { throw ParseException("Parse image limits error", it) }

    fun parseResetLimits(body: String) {
        if (body.contains(INSUFFICIENT_FUNDS)) {
            throw InsufficientFundsException()
        }
    }

    fun parseFunds(body: String): Funds {
        PATTERN_FUNDS.find(body)?.groupValues?.run {
            val fundsC = ParserUtils.parseInt(get(1), 0)
            val fundsGP = ParserUtils.parseInt(get(2), 0) * 1000
            return Funds("%,d+".format(fundsGP), "%,d".format(fundsC))
        }
        throw ParseException("Parse funds error")
    }

    @Parcelize
    data class Result(val limits: Limits, val funds: Funds) : Parcelable
}

@Parcelize
data class Funds(val gp: String, val credit: String) : Parcelable

@Parcelize
@Serializable
data class Limits(val current: Int = 0, val maximum: Int = 0, val resetCost: Int = 0) : Parcelable

private external fun parseLimit(body: ByteBuffer, limit: Int = body.limit()): Int
