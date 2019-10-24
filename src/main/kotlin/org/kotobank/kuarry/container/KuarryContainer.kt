package org.kotobank.kuarry.container

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.*
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.SlotItemHandler
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryContainer(inventoryPlayer: InventoryPlayer, tileEntity: KuarryTileEntity) : Container() {

    init {
        val inventory = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH)

        for (i in 0 until tileEntity.inventoryHeight) {
            for (j in 0 until tileEntity.inventoryWidth) {
                KuarryMod.logger.info("$i $j")
                val positionInInventory = (j * tileEntity.inventoryHeight) + i

                addSlotToContainer(
                        addSlotToContainer(object : SlotItemHandler(inventory, positionInInventory, 8 + j * 18, 84 + i * 18) {
                            override fun onSlotChanged() {
                                tileEntity.markDirty()
                            }
                        })
                )
            }
        }
    }


    override fun canInteractWith(playerIn: EntityPlayer) = true

    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack {
        // Copied from https://github.com/shadowfacts/ShadowMC/blob/1.11/src/main/java/net/shadowfacts/shadowmc/inventory/ContainerBase.java
        // TODO: rewrite

        var itemstack = ItemStack.EMPTY
        val slot = inventorySlots[index]

        if (slot != null && slot.hasStack) {
            val itemstack1 = slot.stack
            itemstack = itemstack1.copy()

            val containerSlots = inventorySlots.size - playerIn.inventory.mainInventory.size

            if (index < containerSlots) {
                if (!this.mergeItemStack(itemstack1, containerSlots, inventorySlots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.mergeItemStack(itemstack1, 0, containerSlots, false)) {
                return ItemStack.EMPTY
            }

            if (itemstack1.count == 0) {
                slot.putStack(ItemStack.EMPTY)
            } else {
                slot.onSlotChanged()
            }

            if (itemstack1.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(playerIn, itemstack1)
        }

        return itemstack
    }
}