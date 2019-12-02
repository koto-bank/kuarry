package org.kotobank.kuarry.integration.autopushing

import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.items.ItemStackHandler

class Autopusher(tileEntity: TileEntity, inventory: ItemStackHandler, width: Int, height: Int) {
    companion object {
        /** References to [BaseAutopusher] constructors and mods that are required to activate them. */
        private val availablePushers = listOf(
                Pair("buildcraftcore", ::BuildcraftAutopusher),
                Pair("thermaldynamics", ::ThermalDynamicsAutopusher)
        )

        /** Whether any of the mods that provide autopushing receivers are loaded. */
        val isEnabled by lazy {
            availablePushers.any { (mod, _) -> Loader.isModLoaded(mod) }
        }
    }

    /** A list of all autopushers that should be tried in order. */
    private val pushers by lazy {
        availablePushers.mapNotNull { (mod, pusherClassInit) ->
            if (Loader.isModLoaded(mod))
                pusherClassInit(tileEntity.world, tileEntity.pos, inventory, width, height)
            else
                null
        }
    }

    /** Tries pushing to any of the autopushing receivers. */
    fun tryPushing() {
        for (pusher in pushers) {
            if (pusher.tryPushing()) break
        }
    }
}