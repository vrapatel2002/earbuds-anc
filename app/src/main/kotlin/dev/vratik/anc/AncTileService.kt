package dev.vratik.anc

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.vratik.anc.proto.AncMode

/** Quick Settings tile: tap cycles High → Transparency → Off. */
class AncTileService : TileService() {
    private val cycle = listOf(AncMode.HIGH, AncMode.TRANSPARENCY, AncMode.OFF)

    override fun onStartListening() = render()

    override fun onClick() {
        val ui = Buds.ui.value
        if (!ui.connected) {
            BudsService.start(this)
            return
        }
        val next = cycle[(cycle.indexOf(ui.anc) + 1) % cycle.size]
        Buds.setAnc(next)
        render(next)
    }

    private fun render(mode: AncMode? = Buds.ui.value.anc) {
        val tile = qsTile ?: return
        val connected = Buds.ui.value.connected
        tile.state = if (connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (connected) mode?.name?.lowercase()?.replaceFirstChar { it.uppercase() } else "Tap to connect"
        tile.updateTile()
    }
}
