# Agregar canales y catálogo bajo demanda

## Canales en vivo

Agrega un objeto a `channels.json` y a `app/src/main/assets/channels.json`. El primer archivo actualiza el catálogo remoto; el segundo mantiene el mismo catálogo disponible sin conexión.

```json
{
  "id": 30,
  "name": "Nombre del canal",
  "categories": ["DIBUJOS"],
  "streamUrl": "https://servidor-autorizado.example/live.m3u8"
}
```

Usa un `id` único y una señal que tengas autorización para redistribuir. La app reproduce direcciones compatibles como HLS (`.m3u8`) y MP4. Las páginas oficiales que requieren su propio reproductor, inicio de sesión o DRM se abren desde su sitio; no se deben reemplazar por enlaces privados extraídos del servicio.

Para añadir un acceso web oficial nuevo, agrega una entrada a `officialPeruLiveLinks` en `MainActivity.kt`. América TVGO ya está disponible en **En vivo** como enlace oficial.

## Series y películas a la carta

Agrega el contenido a `catalog.json` y copia el mismo JSON a `app/src/main/assets/catalog.json`. Cada episodio es un elemento independiente; al elegirlo, la reproducción empieza desde el inicio. Mientras se reproduce una serie, el menú de episodios permite cambiar directamente a otro episodio. Las flechas izquierda y derecha adelantan o retroceden diez segundos.

```json
{
  "series": [
    {
      "id": "serie-01",
      "title": "Nombre de la serie",
      "description": "Descripción breve",
      "posterUrl": "https://servidor-autorizado.example/poster.jpg",
      "episodes": [
        {
          "id": "serie-01-ep-01",
          "episodeNumber": 1,
          "title": "Episodio 1",
          "streamUrl": "https://servidor-autorizado.example/episodio-01.m3u8"
        }
      ]
    }
  ],
  "movies": [
    {
      "id": "pelicula-01",
      "title": "Nombre de la película",
      "description": "Descripción breve",
      "posterUrl": "https://servidor-autorizado.example/poster-pelicula.jpg",
      "streamUrl": "https://servidor-autorizado.example/pelicula.mp4"
    }
  ]
}
```

Las direcciones `.example` son marcadores de posición: reemplázalas por URLs reales, estables y autorizadas antes de publicar. Si cambias contenido localmente, actualiza también los archivos de `assets`; al publicar, sube los JSON de la raíz para que la app los descargue.
