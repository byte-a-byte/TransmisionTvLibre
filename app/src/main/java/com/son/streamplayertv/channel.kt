package com.son.streamplayertv

enum class Category(val displayName: String) {
    TODOS("Todos"),
    DIBUJOS("Dibujos Animados"),
    ANIME("Anime"),
    PELICULAS("Películas");

    override fun toString(): String = displayName

    companion object {
        fun fromRemoteName(name: String): Category? {
            val normalized = name.trim().lowercase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")

            return when {
                normalized.contains("anime") -> ANIME
                normalized.contains("dibujo") || normalized.contains("infantil") -> DIBUJOS
                normalized.contains("pelicula") || normalized.contains("cine") -> PELICULAS
                normalized == "todos" -> TODOS
                else -> values().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            }
        }
    }
}
data class Channel(
    val id: Int,
    val name: String,
    val categories: List<Category>,
    val streamUrl: String
)

data class CatalogDto(
    val series: List<SeriesDto> = emptyList(),
    val movies: List<MovieDto> = emptyList()
)

data class SeriesDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val episodes: List<EpisodeDto> = emptyList()
)

data class EpisodeDto(
    val id: String,
    val title: String,
    val streamUrl: String,
    val episodeNumber: Int? = null
)

data class MovieDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val streamUrl: String
)

data class PlaybackItem(
    val title: String,
    val streamUrl: String
)

object ChannelRepository {

    var channels = listOf(
        Channel(
            id = 1,
            name = "CN City En Vivo",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://envivo.cncity.live/hls/envivo.m3u8"
        ),
        Channel(
            id = 2,
            name = "Cartoon Network Latin America",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://bantel-cdn1.iptvperu.tv:1936/btnscrtn/cartoonnetwork.stream/chunks.m3u8"
        ),
        Channel(
            id = 3,
            name = "Boomerang (United States)",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "http://23.237.104.106:8080/USA_BOOMERANG/index.m3u8"
        ),
        Channel(
            id = 4,
            name = "Nickelodeon en espanol",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5d8d08395f39465da6fb3ec4.m3u8"
        ),
        Channel(
            id = 5,
            name = "Disney Junior (Ingles)",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "http://tvsen5.aynascope.net/disney/index.m3u8"
        ),
        Channel(
            id = 6,
            name = "Tooncast",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://streaming.onnethn.com/TOONCAST/playlist.m3u8"
        ),
        Channel(
            id = 7,
            name = "Bob Esponja Pantalones Cuadrados",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-6254598f5083f800076d8563.m3u8"
        ),
        Channel(
            id = 8,
            name = "Avatar: La Leyenda de Aang",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-6759ee82bd523200083b4f0f.m3u8"
        ),
        Channel(
            id = 9,
            name = "BabyFirst",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "http://181.78.106.127:5000/play/a0sa/index.m3u8"
        ),
        Channel(
            id = 10,
            name = "Pluto TV Kids",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5dd6dae8ce788b0009eaf77b.m3u8"
        ),
        Channel(
            id = 11,
            name = "Pluto TV Junior",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5dcde2ac4bc6c500094ab45b.m3u8"
        ),
    )

    fun getChannelsByCategory(category: Category): List<Channel> {
        if (category == Category.TODOS) return channels
        return channels.filter { channel -> channel.categories.contains(category) }
    }
}
