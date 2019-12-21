package org.kotobank.kuarry.container.kuarry

import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.SlotItemHandler
import org.kotobank.kuarry.container.BaseContainer
import org.kotobank.kuarry.helper.InventoryHelper
import org.kotobank.kuarry.item.KuarryUpgrade
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class KuarryContainer(inventoryPlayer: InventoryPlayer, val tileEntity: KuarryTileEntity) : BaseContainer(inventoryPlayer) {

    companion object {
        private const val xStart = 8

        private const val inventoryYStart = 84
        private const val playerInventoryYStart = 145
        private const val playerHotbarYStart = 203

        private const val upgradeInventoryXStart = 190
        private const val upgradeInventoryYStart = 8
    }

    init {
        val inventory = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.NORTH)

        // Add all the slots from the kuarry inventory
        InventoryHelper.forEachPositionInInventory(KuarryTileEntity.inventoryWidth, KuarryTileEntity.inventoryHeight) {
            positionInInventory, widthPos, heightPos ->

            addSlotToContainer(
                    object : SlotItemHandler(
                            inventory,
                            positionInInventory,
                            xStart + (widthPos * slotSize),
                            inventoryYStart + (heightPos * slotSize)
                    ) {
                        override fun onSlotChanged() {
                            tileEntity.markDirty()
                        }
                    }
            )

            false
        }

        addPlayerInventory(xStart, playerInventoryYStart, playerHotbarYStart)

        // Add the upgrade inventory
        val upgradeInventoryComponent = tileEntity.upgradeInventoryComponent
        val upgradeInventory = upgradeInventoryComponent.upgradeInventory
        InventoryHelper.forEachPositionInInventory(upgradeInventoryComponent.upgradeInventoryWidth, upgradeInventoryComponent.upgradeInventoryHeight) {
            positionInInventory: Int, widthPos: Int, heightPos: Int ->

            addSlotToContainer(
                    object : SlotItemHandler(
                            upgradeInventory,
                            positionInInventory,
                            upgradeInventoryXStart + (widthPos * slotSize),
                            upgradeInventoryYStart + (heightPos * slotSize)
                    ) {
                        override fun onSlotChanged() {
                            tileEntity.markDirty()
                        }

                        override fun isItemValid(stack: ItemStack): Boolean {
                            val item = stack.item

                            return item is KuarryUpgrade &&
                                    // Check that there are no other ItemStacks of this type in the
                                    // upgrade inventory
                                    upgradeInventoryComponent.upgradeCountInInventory(item::class) == 0 &&
                                    // If there are incompatible upgrades defined, check that they are also
                                    // not in the inventory
                                    (if (item.incompatibleWith.isNotEmpty())
                                        item.incompatibleWith.all { upgradeInventoryComponent.upgradeCountInInventory(it) == 0 }
                                    else
                                        true)
                        }
                    }
            )

            false
        }
    }
}