package io.github.dudubarbaro.unolauncher.ui

private val knownNames = mapOf("com.waze" to "Waze", "com.spotify.music" to "Spotify")

/** Atalho salvo → app instalado. "nome:X" acha o app pelo nome; sem app, devolve pacote vazio. */
fun resolveShortcut(saved: String, apps: List<AppEntry>): AppEntry {
    if (saved.startsWith("nome:")) {
        val name = saved.removePrefix("nome:")
        return apps.firstOrNull { it.label.equals(name, ignoreCase = true) }
            ?: apps.firstOrNull { it.label.startsWith(name, ignoreCase = true) }
            ?: AppEntry(name, "", null)
    }
    return apps.firstOrNull { it.pkg == saved } ?: AppEntry(knownNames[saved] ?: saved, saved, null)
}
