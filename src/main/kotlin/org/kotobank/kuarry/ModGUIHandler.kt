package org.kotobank.kuarry

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import org.kotobank.kuarry.container.KuarryContainer
import org.kotobank.kuarry.container.KuarryGUIContainer
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class ModGUIHandler : IGuiHandler {
    companion object {
        val KUARRY = 0
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Container? {
        return when (ID) {
            KUARRY -> KuarryContainer(
                    player!!.inventory,
                    @Suppress("UNCHECKED_CAST") world!!.getTileEntity(BlockPos(x, y, z)) as KuarryTileEntity
            )
            else -> null
        }
    }

    override fun getClientGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? {
        return when (ID) {
            KUARRY -> KuarryGUIContainer(
                    getServerGuiElement(ID, player, world, x, y, z)!!,
                    player!!.inventory
            )
            else -> null
        }
    }
}