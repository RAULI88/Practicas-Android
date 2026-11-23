# Miniproyecto03 — Red Social Local

Desarrollo Móvil Nativo · Kotlin + Jetpack Compose

Red social local (MVVM): publicaciones con título, descripción, foto (cámara o galería) y fecha, guardadas con Room.

Arquitectura MVVM:
- `data/local` → Entity `Publicacion`, `PublicacionDao`, `AppDatabase` (Room, Singleton)
- `data/repository` → `PublicacionRepository`
- `ui/viewmodel` → `PublicacionViewModel` (StateFlow)
- `ui/screens` → `FeedScreen` (listado) y `NuevaPublicacionScreen` (formulario con cámara, galería y DatePicker)
