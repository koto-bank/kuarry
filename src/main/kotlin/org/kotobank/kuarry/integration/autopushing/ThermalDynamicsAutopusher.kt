package org.kotobank.kuarry.integration.autopushing

import cofh.core.util.helpers.InventoryHelper
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.ItemStackHandler

class ThermalDynamicsAutopusher(world: World, pos: BlockPos, inventory: ItemStackHandler, width: Int, height: Int)
    : BaseAutopusher(world, pos, inventory, width, height) {

    override fun tryPushingToSide(toTileEntity: TileEntity?, fromSide: EnumFacing, itemStack: ItemStack): ItemStack {
        return InventoryHelper.addToInventory(toTileEntity, fromSide, itemStack)
    }
}