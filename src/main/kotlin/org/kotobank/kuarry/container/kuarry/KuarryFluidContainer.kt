package org.kotobank.kuarry.container.kuarry

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import org.kotobank.kuarry.container.BaseContainer
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryFluidContainer(playerInv: InventoryPlayer, val tileEntity: KuarryTileEntity) : BaseContainer(playerInv) {
    companion object {
        private const val xStart = 8
        private const val playerInventoryYStart = 50
        private const val playerHotbarYStart = 108
    }

    // TODO: fill buckets if player has them in the inventory?
    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack = ItemStack.EMPTY

    init {
        addPlayerInventory(xStart, playerInventoryYStart, playerHotbarYStart)
    }
}