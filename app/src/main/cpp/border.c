#include <jni.h>
#include <math.h>
#include <stdbool.h>
#include <android/bitmap.h>
#include <stdlib.h>
#include <string.h>

/** A line will be considered as having content if 0.25% of it is filled. */
const float filledRatioLimit = 0.0025;

/** When the threshold is closer to 1, less content will be cropped. **/
#define THRESHOLD 0.75

const uint8_t thresholdForBlack = (uint8_t) (255.0 * THRESHOLD);

const uint8_t thresholdForWhite = (uint8_t) (255.0 - 255.0 * THRESHOLD);

static uint8_t inline grey(uint32_t pixel) {
    const uint8_t r = ((uint8_t *) &pixel)[0];
    const uint8_t g = ((uint8_t *) &pixel)[1];
    const uint8_t b = ((uint8_t *) &pixel)[2];
    return (r * 77 + g * 151 + b * 28) >> 8;
}

static bool inline isBlackPixel(const uint32_t *pixels, uint32_t width, uint32_t x, uint32_t y) {
    const uint32_t pixel = *((uint32_t *) pixels + (y * width + x));
    return grey(pixel) < thresholdForBlack;
}

static bool inline isWhitePixel(const uint32_t *pixels, uint32_t width, uint32_t x, uint32_t y) {
    const uint32_t pixel = *((uint32_t *) pixels + (y * width + x));
    return grey(pixel) > thresholdForWhite;
}

/** Return the first x position where there is a substantial amount of fill,
 * starting the search from the left. */
uint32_t findBorderLeft(uint32_t *pixels, uint32_t width, uint32_t height,
                        uint32_t top, uint32_t bottom) {
    int x, y;
    const uint32_t filledLimit = round(height * filledRatioLimit / 2);

    // Scan first line to detect dominant color
    uint32_t whitePixels = 0;
    uint32_t blackPixels = 0;

    for (y = top; y < bottom; y += 2) {
        if (isBlackPixel(pixels, width, 0, y)) {
            blackPixels++;
        } else if (isWhitePixel(pixels, width, 0, y)) {
            whitePixels++;
        }
    }

    bool (*detectFunc)(const uint32_t *, uint32_t, uint32_t, uint32_t) = isBlackPixel;
    if (whitePixels > filledLimit && blackPixels > filledLimit) {
        // Mixed fill found... don't crop anything
        return 0;
    } else if (blackPixels > filledLimit) {
        detectFunc = isWhitePixel;
    }

    // Scan vertical lines in search of filled lines
    for (x = 1; x < width; x++) {
        uint32_t filledCount = 0;

        for (y = top; y < bottom; y += 2) {
            if (detectFunc(pixels, width, x, y)) {
                filledCount++;
            }
        }

        if (filledCount > filledLimit) {
            // This line contains enough fill
            return x;
        }
    }

    // No fill found... don't crop anything
    return 0;
}

/** Return the first x position where there is a substantial amount of fill,
 * starting the search from the right. */
uint32_t findBorderRight(uint32_t *pixels, uint32_t width, uint32_t height,
                         uint32_t top, uint32_t bottom) {
    int x, y;
    const uint32_t filledLimit = round(height * filledRatioLimit / 2);

    // Scan first line to detect dominant color
    uint32_t whitePixels = 0;
    uint32_t blackPixels = 0;

    uint32_t lastX = width - 1;
    for (y = top; y < bottom; y += 2) {
        if (isBlackPixel(pixels, width, lastX, y)) {
            blackPixels++;
        } else if (isWhitePixel(pixels, width, lastX, y)) {
            whitePixels++;
        }
    }

    bool (*detectFunc)(const uint32_t *, uint32_t, uint32_t, uint32_t) = isBlackPixel;
    if (whitePixels > filledLimit && blackPixels > filledLimit) {
        // Mixed fill found... don't crop anything
        return width;
    } else if (blackPixels > filledLimit) {
        detectFunc = isWhitePixel;
    }

    // Scan vertical lines in search of filled lines
    for (x = width - 2; x > 0; x--) {
        uint32_t filledCount = 0;

        for (y = top; y < bottom; y += 2) {
            if (detectFunc(pixels, width, x, y)) {
                filledCount++;
            }
        }

        if (filledCount > filledLimit) {
            // This line contains enough fill
            return x + 1;
        }
    }

    // No fill found... don't crop anything
    return width;
}

/** Return the first y position where there is a substantial amount of fill,
 * starting the search from the top. */
uint32_t findBorderTop(uint32_t *pixels, uint32_t width, uint32_t height) {
    int x, y;
    const uint32_t filledLimit = round(width * filledRatioLimit / 2);

    // Scan first line to detect dominant color
    uint32_t whitePixels = 0;
    uint32_t blackPixels = 0;

    for (x = 0; x < width; x += 2) {
        if (isBlackPixel(pixels, width, x, 0)) {
            blackPixels++;
        } else if (isWhitePixel(pixels, width, x, 0)) {
            whitePixels++;
        }
    }

    bool (*detectFunc)(const uint32_t *, uint32_t, uint32_t, uint32_t) = isBlackPixel;
    if (whitePixels > filledLimit && blackPixels > filledLimit) {
        // Mixed fill found... don't crop anything
        return 0;
    } else if (blackPixels > filledLimit) {
        detectFunc = isWhitePixel;
    }

    // Scan horizontal lines in search of filled lines
    for (y = 1; y < height; y++) {
        uint32_t filledCount = 0;

        for (x = 0; x < width; x += 2) {
            if (detectFunc(pixels, width, x, y)) {
                filledCount++;
            }
        }

        if (filledCount > filledLimit) {
            // This line contains enough fill
            return y;
        }
    }

    // No fill found... don't crop anything
    return 0;
}

/** Return the first y position where there is a substantial amount of fill,
 * starting the search from the bottom. */
uint32_t findBorderBottom(uint32_t *pixels, uint32_t width, uint32_t height) {
    int x, y;
    const uint32_t filledLimit = round(width * filledRatioLimit / 2);

    // Scan first line to detect dominant color
    uint32_t whitePixels = 0;
    uint32_t blackPixels = 0;
    uint32_t lastY = height - 1;

    for (x = 0; x < width; x += 2) {
        if (isBlackPixel(pixels, width, x, lastY)) {
            blackPixels++;
        } else if (isWhitePixel(pixels, width, x, lastY)) {
            whitePixels++;
        }
    }

    bool (*detectFunc)(const uint32_t *, uint32_t, uint32_t, uint32_t) = isBlackPixel;
    if (whitePixels > filledLimit && blackPixels > filledLimit) {
        // Mixed fill found... don't crop anything
        return height;
    } else if (blackPixels > filledLimit) {
        detectFunc = isWhitePixel;
    }

    // Scan horizontal lines in search of filled lines
    for (y = height - 2; y > 0; y--) {
        uint32_t filledCount = 0;

        for (x = 0; x < width; x += 2) {
            if (detectFunc(pixels, width, x, y)) {
                filledCount++;
            }
        }

        if (filledCount > filledLimit) {
            // This line contains enough fill
            return y + 1;
        }
    }

    // No fill found... don't crop anything
    return height;
}

JNIEXPORT jintArray JNICALL
Java_com_hippo_ehviewer_image_ImageKt_detectBorder(JNIEnv *env, jclass clazz, jobject bitmap) {
    void *pixels = NULL;
    AndroidBitmapInfo info;
    memset(&info, 0, sizeof(info));
    AndroidBitmap_getInfo(env, bitmap, &info);
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    uint32_t width = info.width;
    uint32_t height = info.height;
    uint32_t top = findBorderTop(pixels, width, height);
    uint32_t bottom = findBorderBottom(pixels, width, height);
    uint32_t left = findBorderLeft(pixels, width, height, top, bottom);
    uint32_t right = findBorderRight(pixels, width, height, top, bottom);
    int array[4] = {left, top, right, bottom};
    AndroidBitmap_unlockPixels(env, bitmap);
    jintArray ret = (*env)->NewIntArray(env, 4);
    (*env)->SetIntArrayRegion(env, ret, 0, 4, array);
    return ret;
}
