package com.strukov.processor.extensions

import java.io.OutputStream

internal fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}