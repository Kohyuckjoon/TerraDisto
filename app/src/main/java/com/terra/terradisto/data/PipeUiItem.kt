package com.terra.terradisto.data

data class PipeUiItem(
    val id: Int,
    var direction: String = "",
    var diameter: String = "",
    var height: String = "",
    var material: String = ""
)

enum class ActiveTarget {
    NONE, LID_SIZE, TOPI, CHAMBER_SIZE, CHAMBER_WIDTH, CHAMBER_HEIGHT, PIPE_SIZE, PIPE_HEIGHT_SIZE
}