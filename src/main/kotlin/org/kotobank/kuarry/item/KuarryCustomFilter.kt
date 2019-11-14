package org.kotobank.kuarry.item

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModGUIHandler

class KuarryCustomFilter : KuarryUpgrade() {

    companion object {
        internal const val inventoryWidth = 9
        internal const val inventoryHeight = 3
        internal const val inventorySize = inventoryWidth * inventoryHeight

        val defaultNBT = NBTTagCompound().apply {
            setString("mode", Mode.Blacklist.name)
            setString("blacklist_mode", BlacklistMode.Additional.name)
        }

        /** Gets the [Mode] of the filter from the [ItemStack]'s NBT. */
        fun mode(stack: ItemStack) =
                Mode.valueOf((stack.tagCompound ?: defaultNBT).getString("mode"))

        /** Gets the [BlacklistMode] of the filter from the [ItemStack]'s NBT. */
        fun blacklistMode(stack: ItemStack) =
                BlacklistMode.valueOf((stack.tagCompound ?: defaultNBT).getString("blacklist_mode"))

        private fun switchString(itemStack: ItemStack, field: String, tfFunction: (oldValue: String) -> String) {
            itemStack.tagCompound =
                    (itemStack.tagCompound ?: defaultNBT).apply {
                        setString(field, tfFunction(getString(field)))
                    }
        }

        fun switchMode(itemStack: ItemStack) {
            switchString(itemStack, "mode") { oldVal ->
                when (Mode.valueOf(oldVal)) {
                    Mode.Blacklist -> Mode.Whitelist
                    Mode.Whitelist -> Mode.Blacklist
                }.name
            }
        }

        fun switchBlacklistMode(itemStack: ItemStack) {
            switchString(itemStack, "blacklist_mode") { oldVal ->
                when (BlacklistMode.valueOf(oldVal)) {
                    BlacklistMode.Additional -> BlacklistMode.Only
                    BlacklistMode.Only -> BlacklistMode.Additional
                }.name
            }
        }
    }

    enum class Mode {
        Whitelist, Blacklist
    }

    enum class BlacklistMode {
        Only, Additional
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> =
            if (handIn == EnumHand.MAIN_HAND) {
                // Open the GUI. Pass zero XYZ because they don't matter.
                playerIn.openGui(KuarryMod, KuarryModGUIHandler.CUSTOM_FILTER, worldIn, 0, 0, 0)

                ActionResult.newResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(handIn))
            } else {
                ActionResult.newResult(EnumActionResult.PASS, playerIn.getHeldItem(handIn))
            }

    override fun initCapabilities(stack: ItemStack, nbt: NBTTagCompound?): ICapabilityProvider? =
            CustomFilterCapabilityProvider()

    class CustomFilterCapabilityProvider : ICapabilityProvider, INBTSerializable<NBTTagCompound> {
        val inventory = ItemStackHandler(inventorySize)

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) =
                capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? =
                when (capability) {
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> inventory as T
                    else -> null
                }

        override fun serializeNBT(): NBTTagCompound =
                NBTTagCompound().apply {
                    setTag("inventory", inventory.serializeNBT())
                }

        override fun deserializeNBT(nbt: NBTTagCompound?) {
            if (nbt != null) {
                with (nbt) {
                    inventory.deserializeNBT(getCompoundTag("inventory"))
                }
            }
        }
    }
}