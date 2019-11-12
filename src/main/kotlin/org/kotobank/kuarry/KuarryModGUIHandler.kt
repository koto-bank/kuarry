package org.kotobank.kuarry

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import org.kotobank.kuarry.container.custom_filter.CustomFilterContainer
import org.kotobank.kuarry.container.custom_filter.CustomFilterGUIContainer
import org.kotobank.kuarry.container.kuarry.*
import org.kotobank.kuarry.item.KuarryCustomFilter
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryModGUIHandler : IGuiHandler {
    companion object {
        const val KUARRY = 0
        const val CUSTOM_FILTER = 1
    }

    @Suppress("UNCHECKED_CAST")
    override fun getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Container? {
        return when (ID) {
            KUARRY -> KuarryContainer(
                    player.inventory,
                    world.getTileEntity(BlockPos(x, y, z)) as KuarryTileEntity
            )
            CUSTOM_FILTER ->
                // Only open the GUI only when there is a custom filter in the main hand
                if (player.heldItemMainhand.item is KuarryCustomFilter) {
                    CustomFilterContainer(player)
                } else {
                    null
                }
            else -> null
        }
    }

    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        return when (ID) {
            KUARRY ->
                getServerGuiElement(ID, player, world, x, y, z)?.let {
                    require(it is KuarryContainer)

                    KuarryGUIContainer(it)
                }
                CUSTOM_FILTER ->
                    getServerGuiElement(ID, player, world, x, y, z)?.let {
                        require(it is CustomFilterContainer)

                        CustomFilterGUIContainer(it)
                    }

            else -> null
        }
    }
}