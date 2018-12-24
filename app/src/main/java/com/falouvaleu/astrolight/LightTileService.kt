package com.falouvaleu.astrolight

import android.annotation.SuppressLint
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.falouvaleu.astrolight.LightService.Companion.DISABLE_LIGHT
import com.falouvaleu.astrolight.LightService.Companion.ENABLE_LIGHT

@SuppressLint("NewApi")
class LightTileService: TileService() {
    override fun onStartListening() {
        val tile = qsTile
        tile.icon = Icon.createWithResource(this, R.drawable.shooting_star)
        tile.label = "AstroLight"
        tile.contentDescription = "Filtre de lumière rouge"
        tile.state = if (LightService.lightOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

        tile.updateTile()
    }

    override fun onClick() {
        if (LightService.lightOn) {
            LightService.request(DISABLE_LIGHT)
            qsTile.state = Tile.STATE_INACTIVE
        } else {
            LightService.request(ENABLE_LIGHT)
            qsTile.state = Tile.STATE_ACTIVE
        }
        qsTile.updateTile()
    }
}