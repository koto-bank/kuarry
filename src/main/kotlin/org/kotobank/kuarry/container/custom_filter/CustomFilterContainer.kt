package org.kotobank.kuarry.container.custom_filter

import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.ClickType
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.ItemStackHandler
import net.minecraftforge.items.SlotItemHandler
import org.kotobank.kuarry.container.BaseContainer
import org.kotobank.kuarry.item.KuarryCustomFilter

/** The container for the custom filter.
 *
 * @param player the player opening the container. It has to be the player, since an
 *               up to date version of the custom filter item stack is required for GUI
 */
class CustomFilterContainer(val player: EntityPlayer) : BaseContainer(player.inventory) {

    companion object {
        private const val xStart = 8
        private const val filterSlotsYStart = 12

        private const val playerInventoryYStart = 90
        private const val playerHotbarYStart = 148
    }

    /** The inventory capability instance.
     *
     * The instance should not ever be null, as it is initialized with the item.
     */
    private var inventory: ItemStackHandler =
            player.heldItemMainhand.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) as ItemStackHandler

    init {
        forEachPositionInInventory(KuarryCustomFilter.inventoryWidth, KuarryCustomFilter.inventoryHeight) {
            positionInInventory, widthPos, heightPos ->

            addSlotToContainer(
                    FilterSlot(
                            inventory,
                            positionInInventory,
                            xStart + (widthPos * slotSize),
                            filterSlotsYStart + (heightPos * slotSize)
                    )
            )

            false
        }

        addPlayerInventory(xStart, playerInventoryYStart, playerHotbarYStart)
    }

    override fun canInteractWith(playerIn: EntityPlayer) = true

    override fun slotClick(slotId: Int, dragType: Int, clickTypeIn: ClickType, player: EntityPlayer): ItemStack {
        val slot = if (slotId >= 0) getSlot(slotId) else null

        /** Check if the filter should allow the item stack to be put in.
         *
         * If it does not, then nothing should be transferred.
         */
        fun itemStackAllowed(itemStack: ItemStack): Boolean {
            require(slot != null)

            // If the item stack is empty of the item is not a block, don't allow it
            if (itemStack.isEmpty || Block.getBlockFromItem(itemStack.item) == Blocks.AIR) return false;

            var hasAnyAlready = false
            for (position in 0 until KuarryCustomFilter.inventorySize) {
                val stackInSlot = inventory.getStackInSlot(position)
                if (stackInSlot.item == itemStack.item) {
                    hasAnyAlready = true

                    break
                }
            }

            // If there's an item already, more should not be allowed
            return !hasAnyAlready
        }

        return when {
            slot is FilterSlot -> {
                when {
                    clickTypeIn == ClickType.QUICK_MOVE ->
                        slot.putStack(ItemStack.EMPTY)
                    itemStackAllowed(inventoryPlayer.itemStack) ->
                        slot.putStack(inventoryPlayer.itemStack)
                }

                inventoryPlayer.itemStack
            }
            slot != null && clickTypeIn == ClickType.QUICK_MOVE -> {
                // Check that the filter does not have that same that is in the slot already.
                // If it has, then don't actually insert anything into it
                if (itemStackAllowed(slot.stack)) {
                    for (position in 0 until KuarryCustomFilter.inventorySize) {
                        val filterSlot = inventorySlots[position]
                        if (filterSlot.stack.isEmpty) {
                            filterSlot.putStack(slot.stack)

                            break
                        }
                    }
                }

                inventoryPlayer.itemStack
            }
            else -> super.slotClick(slotId, dragType, clickTypeIn, player)
        }
    }

    /** A slot that normally doesn't accept items, so the base game's GUI won't put them there.
     *
     * If the item is put there through custom code, it should not be used up.
     * This slot also ensures that only one item is in the slot at all times.
     */
    class FilterSlot(inventory: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(inventory, index, x, y) {
        override fun decrStackSize(amount: Int): ItemStack = ItemStack.EMPTY
        override fun isItemValid(stack: ItemStack) = false
        override fun putStack(stack: ItemStack) {
            if (!stack.isEmpty) {
                val copy = stack.copy()

                copy.count = 1

                super.putStack(copy)
            } else {
                super.putStack(stack)
            }
        }

        override fun canTakeStack(playerIn: EntityPlayer) = false
    }
}