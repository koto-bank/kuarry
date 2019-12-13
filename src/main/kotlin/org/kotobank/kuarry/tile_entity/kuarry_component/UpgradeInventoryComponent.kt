package org.kotobank.kuarry.tile_entity.kuarry_component

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.helper.InventoryHelper
import org.kotobank.kuarry.item.KuarryUpgrade
import org.kotobank.kuarry.item.LevelValues
import org.kotobank.kuarry.tile_entity.KuarryTileEntity
import kotlin.reflect.KClass

class UpgradeInventoryComponent(val parent: KuarryTileEntity) {
    internal val upgradeInventoryWidth = 2

    internal val upgradeInventoryHeight
        get() =
            LevelValues[parent.upgradeLevel].upgradeSlotLines

    internal val upgradeInventorySize
        get() = upgradeInventoryWidth * upgradeInventoryHeight

    /** A small inventory for upgrades not exposed via getCapability */
    internal val upgradeInventory = object : ItemStackHandler(upgradeInventorySize) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)

            parent.markDirty()
        }
    }

    fun onUpgradeLevelChanged() {
        val oldSize = upgradeInventory.slots

        // If the new size is different, the inventory needs to be expanded
        if (oldSize != upgradeInventorySize) {
            val oldHeight = oldSize / upgradeInventoryWidth
            // Copy all the items in the same order the player sees them by saving their width/height positions
            val oldItems = HashMap<Pair<Int, Int>, ItemStack>(oldSize)
            InventoryHelper.forEachPositionInInventory(upgradeInventoryWidth, oldHeight) {
                posInInventory: Int, widthPos: Int, heightPos: Int ->

                oldItems[Pair(widthPos, heightPos)] = upgradeInventory.getStackInSlot(posInInventory)

                false
            }

            // Set the new upgrade inventory size, after the field has been updated
            upgradeInventory.setSize(upgradeInventorySize)

            // Restore all the items to their original positions
            oldItems.forEach { (widthPos, heightPos), stack ->
                val pos = (widthPos * upgradeInventoryHeight) + heightPos

                upgradeInventory.setStackInSlot(pos, stack)
            }
        }
    }

    fun writeToNBT(compound: NBTTagCompound) {
        compound.apply {
            setTag("upgrade_inventory", upgradeInventory.serializeNBT())
        }
    }

    fun readToNBT(compound: NBTTagCompound) {
        compound.apply {
            upgradeInventory.deserializeNBT(getCompoundTag("upgrade_inventory"))
        }
    }

    /** Finds an upgrade in the inventory, or null if there was none.
     *
     * The inventory _should_ not contain more than one [ItemStack] of any upgrade,
     * therefore this function returns the first upgrade found.
     */
    fun upgradeInInventory(upgradeC: KClass<out Item>): ItemStack? {
        for (i in 0 until upgradeInventorySize) {
            val slotItemStack = upgradeInventory.getStackInSlot(i)
            if (!slotItemStack.isEmpty && slotItemStack.item::class == upgradeC) {
                return slotItemStack
            }
        }

        return null
    }

    /** Counts the amount of upgrades of specified type in the upgrade inventory. */
    fun upgradeCountInInventory(upgradeC: KClass<out Item>): Int =
            upgradeInInventory(upgradeC)?.count ?: 0

    /** A convenience function with a generic parameter that calls the [KClass]-receiving version. */
    internal inline fun <reified T : KuarryUpgrade> upgradeCountInInventory() = upgradeCountInInventory(T::class)
}