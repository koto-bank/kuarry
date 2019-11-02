package org.kotobank.kuarry.container

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.*
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.SlotItemHandler
import org.kotobank.kuarry.item.KuarryUpgrade
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryContainer(inventoryPlayer: InventoryPlayer, val tileEntity: KuarryTileEntity) : Container() {

    companion object {
        private const val xStart = 8

        private const val inventoryYStart = 84
        private const val playerInventoryYStart = 145
        private const val playerHotbarYStart = 203

        private const val upgradeInventoryXStart = 190
        private const val upgradeInventoryYStart = 8

        private const val slotSize = 18
    }

    init {
        val inventory = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH)

        // Add all the slots from the kuarry inventory
        for (i in 0 until tileEntity.inventoryHeight) {
            for (j in 0 until tileEntity.inventoryWidth) {
                val positionInInventory = (j * tileEntity.inventoryHeight) + i

                addSlotToContainer(
                        addSlotToContainer(
                                object : SlotItemHandler(
                                        inventory,
                                        positionInInventory,
                                        xStart + (j * slotSize),
                                        inventoryYStart + (i * slotSize)
                                ) {
                                    override fun onSlotChanged() {
                                        tileEntity.markDirty()
                                    }
                                }
                        )
                )
            }
        }

        // Player inventory size is constant, 3 x 9 + toolbar of width 9
        val playerInventoryWidth = 9
        val playerInventoryHeight = 3
        val playerHotbarSize = 9

        for (i in 0 until playerInventoryHeight) {
            for (j in 0 until playerInventoryWidth) {
                // Hotbar is at the beginning of the inventory, need to skip that for now
                val positionInInventory = ((j * playerInventoryHeight) + i) + playerHotbarSize;

                addSlotToContainer(Slot(
                        inventoryPlayer,
                        positionInInventory,
                        xStart + (j * slotSize),
                        playerInventoryYStart + (i * slotSize)
                ))
            }
        }

        // Now draw the player's hotbar
        for (k in 0 until 9) {
            addSlotToContainer(Slot(inventoryPlayer, k, xStart + (k * slotSize), playerHotbarYStart))
        }

        // Draw the upgrade inventory
        val upgradeInventory = tileEntity.upgradeInventory
        for (i in 0 until KuarryTileEntity.upgradeInventoryHeight) {
            for (j in 0 until KuarryTileEntity.upgradeInventoryWidth) {
                val positionInInventory = (j * KuarryTileEntity.upgradeInventoryHeight) + i

                addSlotToContainer(
                        addSlotToContainer(
                                object : SlotItemHandler(
                                        upgradeInventory,
                                        positionInInventory,
                                        upgradeInventoryXStart + (j * slotSize),
                                        upgradeInventoryYStart + (i * slotSize)
                                ) {
                                    override fun onSlotChanged() {
                                        tileEntity.markDirty()
                                    }

                                    override fun isItemValid(stack: ItemStack): Boolean {
                                        return if (stack.item is KuarryUpgrade) {
                                            // Find if any OTHER slot has that same upgrade. If so,
                                            // don't allow putting another on in a different slot
                                            var alreadyHas = false
                                            for (ii in 0 until KuarryTileEntity.upgradeInventorySize) {
                                                if (ii != positionInInventory && upgradeInventory.getStackInSlot(ii).item == stack.item) {
                                                    alreadyHas = true
                                                    break
                                                }
                                            }

                                            return !alreadyHas
                                        } else { false }
                                    }
                                }
                        )
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