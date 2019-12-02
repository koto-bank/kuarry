package org.kotobank.kuarry.integration.autopushing

import buildcraft.lib.inventory.ItemTransactorHelper
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.ItemStackHandler


class BuildcraftAutopusher(world: World, pos: BlockPos, inventory: ItemStackHandler, width: Int, height: Int)
    : BaseAutopusher(world, pos, inventory, width, height) {

    override fun tryPushingToSide(toTileEntity: TileEntity?, fromSide: EnumFacing, itemStack: ItemStack): ItemStack {
        val injectable = ItemTransactorHelper.getInjectable(toTileEntity, fromSide)

        return injectable.injectItem(itemStack, true, fromSide.opposite, null, 0.0)
    }
}