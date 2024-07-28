package com.ehviewer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: throw Exception("targetAppId not passed as instrumentation runner arg"),
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()

            // Onboarding
            if (device.hasObject(By.text("Username"))) {
                device.findObject(By.scrollable(true))?.run {
                    scrollUntil(Direction.DOWN, Until.hasObject(By.text("Guest mode")))
                }
                device.findObject(By.text("Guest mode")).click()
            }

            // Gallery list
            device.wait(Until.hasObject(By.text("NON-H")), 10_000)
            device.findObject(By.scrollable(true))?.run {
                fling(Direction.DOWN)
                click()
            }

            // Gallery detail
            device.wait(Until.hasObject(By.text("Similar")), 10_000)
            device.findObject(By.hasDescendant(By.text("Read")).scrollable(true))?.fling(Direction.DOWN)

            device.waitForIdle()
        }
    }
}
