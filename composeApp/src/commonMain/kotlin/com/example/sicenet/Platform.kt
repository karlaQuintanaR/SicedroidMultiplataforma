package com.example.sicenet

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform