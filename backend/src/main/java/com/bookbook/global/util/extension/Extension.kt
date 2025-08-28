package com.bookbook.global.util.extension

fun <T: Any> T?.getOrThrow(): T {
    return this ?: throw NoSuchElementException()
}