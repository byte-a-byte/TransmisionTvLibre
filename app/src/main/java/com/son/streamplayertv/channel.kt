package com.son.streamplayertv

// 1. Enum para categorías: elimina posibilidad de errores de tipeo
enum class Category(val displayName: String) {
    TODOS("Todos"),
    DIBUJOS("Dibujos Animados"),
    ANIME("Anime"),
    PELICULAS("Películas");

    override fun toString(): String = displayName
}
// 2. Modelo de datos fuertemente tipado
data class Channel(
    val id: Int,                   // ID numérico simple (1, 2, 3...)
    val name: String,
    val categories: List<Category>,        // Usamos la Enum
    val streamUrl: String
)

// 3. Repositorio de canales
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
        Channel(
            id = 12,
            name = "Naruto en espanol",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5ee92e72fb286e0007285fec.m3u8"
        ),
        Channel(
            id = 13,
            name = "Naruto Shippuden",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-64c92f965580090008084968.m3u8"
        ),
        Channel(
            id = 14,
            name = "One Piece",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5ff4b9ccf938f8000779eb99.m3u8"
        ),
        Channel(
            id = 15,
            name = "Detective Conan",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-629a066860ef810008267b70.m3u8"
        ),
        Channel(
            id = 16,
            name = "Yu-Gi-Oh! en espanol",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://amg01796-amg01796c18-rakuten-es-7491.playouts.now.amagi.tv/playlist/amg01796-fastmediafast-yugiohees-rakutenes/playlist.m3u8"
        ),
        Channel(
            id = 17,
            name = "Pluto TV Anime",
            categories = listOf(Category.DIBUJOS, Category.ANIME),
            streamUrl = "https://jmp2.uk/plu-5dcde17bf6591d0009839e02.m3u8"
        )
    )

    // Función de utilidad para obtener canales filtrados por categoría
    fun getChannelsByCategory(category: Category): List<Channel> {
        if (category == Category.TODOS) return channels
        return channels.filter { channel -> channel.categories.contains(category) }
    }
}