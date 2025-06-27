package com.monday8am.lottierecorder.model 

internal sealed class Scene {
    data class Title(
        val name: String,
        val date: String,
        val duration: String,
        val avgSpeed: String,
        val distance: String,
        val elevationUp: String,
        val elevationDown: String,
        val participantNames: List<String>,
        val participantAvatars: List<String>,
        val coverUrl: String,
    ) : Scene()
    data class Map(val url: String) : Scene()
    data class Slideshow(val imageUrls: List<String>) : Scene()
    data object End : Scene()
}
