package com.monday8am.lottierecorder.recording

import android.graphics.Rect

/**
 * Calculates the bounds to fit a content of a given size (contentWidth, contentHeight)
 * inside a target container (canvasWidth, canvasHeight), maintaining the content's
 * aspect ratio and centering it.
 *
 * @param contentWidth The original width of the content to fit.
 * @param contentHeight The original height of the content to fit.
 * @param canvasWidth The width of the container to fit the content into.
 * @param canvasHeight The height of the container to fit the content into.
 * @return A [Rect] representing the new bounds for the content, scaled and centered.
 */
internal fun calculateFitInsideBounds(
    contentWidth: Int,
    contentHeight: Int,
    canvasWidth: Int,
    canvasHeight: Int
): Rect {
    if (contentWidth == 0 || contentHeight == 0 || canvasWidth == 0 || canvasHeight == 0) {
        return Rect(0, 0, canvasWidth, canvasHeight) // Avoid division by zero, return full canvas
    }

    val contentAspectRatio = contentWidth.toFloat() / contentHeight.toFloat()
    val canvasAspectRatio = canvasWidth.toFloat() / canvasHeight.toFloat()

    var finalWidth: Int
    var finalHeight: Int

    if (contentAspectRatio > canvasAspectRatio) {
        // Content is wider relative to canvas, so width is the limiting factor
        finalWidth = canvasWidth
        finalHeight = (canvasWidth / contentAspectRatio).toInt()
    } else {
        // Content is taller relative to canvas (or aspect ratios are equal),
        // so height is the limiting factor
        finalHeight = canvasHeight
        finalWidth = (canvasHeight * contentAspectRatio).toInt()
    }

    // Calculate centering
    val left = (canvasWidth - finalWidth) / 2
    val top = (canvasHeight - finalHeight) / 2
    val right = left + finalWidth
    val bottom = top + finalHeight

    return Rect(left, top, right, bottom)
}
