package io.github.dudubarbaro.unolauncher.ui

import io.github.dudubarbaro.unolauncher.settings.CONSIS_BY_NAME
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutsTest {
    private val apps = listOf(
        AppEntry("Waze", "com.waze", null),
        AppEntry("Consis Smart", "br.com.x.smart", null),
        AppEntry("Consis", "br.com.x.consis", null),
    )

    @Test fun byPackage() = assertEquals("Waze", resolveShortcut("com.waze", apps).label)

    @Test fun byExactNameBeforePrefix() = assertEquals("br.com.x.consis", resolveShortcut(CONSIS_BY_NAME, apps).pkg)

    @Test fun byNamePrefix() = assertEquals("br.com.x.smart", resolveShortcut(CONSIS_BY_NAME, apps.take(2)).pkg)

    @Test fun nameMissing() = assertEquals(AppEntry("Consis", "", null), resolveShortcut(CONSIS_BY_NAME, emptyList()))

    @Test fun knownPackageMissing() = assertEquals(AppEntry("Spotify", "com.spotify.music", null), resolveShortcut("com.spotify.music", apps))

    @Test fun unknownPackageMissing() = assertEquals("com.foo", resolveShortcut("com.foo", apps).label)

    /** Logo após o boot a lista de apps ainda está vazia: o toque procura o Consis de novo. */
    @Test fun nameShortcutLooksUpAgainOnTap() =
        assertEquals("br.com.x.consis", shortcutPackage(AppEntry("Consis", "", null)) { apps })

    @Test fun packageShortcutSkipsLookup() =
        assertEquals("com.waze", shortcutPackage(AppEntry("Waze", "com.waze", null)) { error("sem busca") })
}
