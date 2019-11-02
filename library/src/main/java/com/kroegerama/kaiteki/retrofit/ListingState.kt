package com.kroegerama.kaiteki.retrofit

enum class ListingState {
    IDLE,
    RUNNING,
    RETRYING,
    FINISHED;

    val isRunning get() = this === RUNNING || this === RETRYING
}