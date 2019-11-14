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
        }

        /** Gets the [Mode] of the filter from the [ItemStack]'s NBT. */
        fun mode(stack: ItemStack) =
                Mode.valueOf((stack.tagCompound ?: defaultNBT).getString("mode"))

        fun switchMode(itemStack: ItemStack) {
            val compound = itemStack.tagCompound ?: defaultNBT

            with (compound) {
                setString(
                        "mode",
                        when (mode(itemStack)) {
                            Mode.Blacklist -> Mode.Whitelist
                            Mode.Whitelist -> Mode.Blacklist
                        }.name
                )
            }

            itemStack.tagCompound = compound
        }
    }

    enum class Mode {
        Whitelist, Blacklist
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