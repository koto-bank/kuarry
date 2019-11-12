package org.kotobank.kuarry.container

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

/** A base class for container handling.
 *
 * Contains a base implementation of some functions that should be useful in most GUIs.
 */
abstract class BaseContainer(val inventoryPlayer: InventoryPlayer) : Container() {
    // Player inventory size is constant, 3 x 9 + toolbar of width 9
    val playerInventoryWidth = 9
    val playerInventoryHeight = 3
    val playerHotbarSize = 9

    val slotSize = 18

    companion object {
        /** Runs a function, passing it a position in the inventory matrix, traversing it from left to right, top to bottom.
         *
         * @param width Width of the inventory
         * @param height Height of the inventory
         * @param func The function which is run on each position, where "position" is the slot number in the inventory, and
         *             "widthPos" & "heightPos" are the corresponding column and line in the inventory.
         *             If this function returns "true", then iteration is stopped.
         */
        internal fun forEachPositionInInventory(width: Int, height: Int, func: (position: Int, widthPos: Int, heightPos: Int) -> Boolean) {
            for (i in 0 until height) {
                for (j in 0 until width) {
                    val position = (j * height) + i

                    if (func(position, j, i)) return
                }
            }
        }
    }

    /** Adds the player's inventory to the container.
     *
     * @param xStart where to start adding slots on the X axis
     * @param yStart where to start adding slots on the Y axis
     * @param hotbarYStart where to start adding the hotbar slots on the Y axis
     */
    fun addPlayerInventory(xStart: Int, yStart: Int, hotbarYStart: Int) {
        forEachPositionInInventory(playerInventoryWidth, playerInventoryHeight) { posWithoutHotbar, widthPos, heightPos ->
            // Add hotbar to the inventory position, since hotbar is actually in the beginning of the inventory
            val positionInInventory = posWithoutHotbar + playerHotbarSize

            addSlotToContainer(Slot(
                    inventoryPlayer,
                    positionInInventory,
                    xStart + (widthPos * slotSize),
                    yStart + (heightPos * slotSize)
            ))

            false
        }

        // Now draw the player's hotbar
        for (k in 0 until 9) {
            addSlotToContainer(Slot(inventoryPlayer, k, xStart + (k * slotSize), hotbarYStart))
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