package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.exception.CloudflareBypassException
import java.util.concurrent.CountDownLatch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class CloudflareInterceptor(context: Context) : WebViewInterceptor(context) {
    private val executor = ContextCompat.getMainExecutor(context)

    override fun shouldIntercept(response: Response): Boolean {
        return response.header(HEADER_NAME) == HEADER_VALUE
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        response.close()
        EhCookieStore.deleteCookie(request.url, COOKIE_NAME)
        resolveWithWebView(request)

        return chain.proceed(request)
    }

    private fun resolveWithWebView(originalRequest: Request) {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webview: WebView? = null

        var challengeFound = false
        var cloudflareBypassed = false

        val origRequestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)

        executor.execute {
            webview = createWebView()

            webview?.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return EhCookieStore.get(origRequestUrl.toHttpUrl())
                            .any { it.name == COOKIE_NAME }
                    }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

                    if (url == origRequestUrl && !challengeFound) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame) {
                        if (errorResponse.responseHeaders[HEADER_NAME] == HEADER_VALUE) {
                            // Found the Cloudflare challenge page.
                            challengeFound = true
                        } else {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }
            }

            webview?.loadUrl(origRequestUrl, headers)
        }

        latch.awaitFor30Seconds()

        executor.execute {
            webview?.run {
                stopLoading()
                destroy()
            }
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) {
            throw CloudflareBypassException()
        }
    }
}

private const val COOKIE_NAME = "cf_clearance"
private const val HEADER_NAME = "cf-mitigated"
private const val HEADER_VALUE = "challenge"
