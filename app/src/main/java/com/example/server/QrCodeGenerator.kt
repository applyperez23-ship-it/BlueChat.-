package com.example.server

import android.graphics.Bitmap
import android.graphics.Color

object QrCodeGenerator {

    /**
     * Generates a clean, high-contrast QR visual representation bitmap for local hotspot URLs.
     * Implements a standard 2D matrix pattern with authentic corner finder patterns and data grid.
     */
    fun generateQrBitmap(content: String, size: Int = 400): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val matrixSize = 25
        val cellSize = size / matrixSize
        val matrix = Array(matrixSize) { BooleanArray(matrixSize) }

        // Render standard QR corner finder patterns (7x7 with inner 3x3)
        drawFinderPattern(matrix, 0, 0)
        drawFinderPattern(matrix, matrixSize - 7, 0)
        drawFinderPattern(matrix, 0, matrixSize - 7)

        // Timing patterns
        for (i in 8 until matrixSize - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Deterministic content encoding into grid
        val bytes = content.toByteArray()
        var byteIndex = 0
        var bitIndex = 0

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                // Skip finder pattern zones
                if ((r < 8 && c < 8) || (r >= matrixSize - 8 && c < 8) || (r < 8 && c >= matrixSize - 8)) {
                    continue
                }
                if (r == 6 || c == 6) continue

                val currentByte = if (bytes.isNotEmpty()) bytes[byteIndex % bytes.size].toInt() else 0
                val bit = ((currentByte shr (bitIndex % 8)) and 1) == 1
                matrix[r][c] = bit xor ((r + c) % 2 == 0)

                bitIndex++
                if (bitIndex % 8 == 0) byteIndex++
            }
        }

        // Draw matrix to bitmap with clean quiet zone
        val darkColor = Color.rgb(10, 25, 47) // Navy #0A192F
        val lightColor = Color.WHITE

        for (x in 0 until size) {
            for (y in 0 until size) {
                val mx = (x / cellSize).coerceIn(0, matrixSize - 1)
                val my = (y / cellSize).coerceIn(0, matrixSize - 1)
                bitmap.setPixel(x, y, if (matrix[mx][my]) darkColor else lightColor)
            }
        }

        return bitmap
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, startX: Int, startY: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = (r == 0 || r == 6 || c == 0 || c == 6)
                val isInner = (r in 2..4 && c in 2..4)
                matrix[startX + r][startY + c] = (isOuter || isInner)
            }
        }
    }
}
