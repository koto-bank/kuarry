package org.kotobank.kuarry.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagByteArray
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.*
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.INBTSerializable
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModGUIHandler
import kotlin.reflect.KClass

abstract class KuarryUpgrade : Item() {
    /** Since items don't have an initialization function and using an open member
     *  in the constructor doesn't work properly, this has to be set by the caller with [setMaxStackSize]
     */
    open val stackSize = 1

    open val incompatibleWith: KClass<out KuarryUpgrade>? = null

    open val tooltipStrings = listOf<String>()

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.addAll(tooltipStrings)
    }
}

class KuarryXBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val tooltipStrings = listOf("Upgrades the kuarry to mine one more chunk along the X dimension")
}

class KuarryZBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val tooltipStrings = listOf("Upgrades the kuarry to mine one more chunk along the Z dimension")
}

class KuarrySilkTouchUpgrade : KuarryUpgrade() {
    override val tooltipStrings = listOf(
            "Makes the kuarry retrieve the blocks as they are, instead of mining them",
            "Not compatible with the luck upgrade"
    )

    override val incompatibleWith = KuarryLuckUpgrade::class
}

class KuarryLuckUpgrade : KuarryUpgrade() {

    override val tooltipStrings = listOf(
            "Makes the kuarry mine more resources from some ores",
            "Not compatible with the silk touch upgrade"
    )

    override fun getHasSubtypes() = true

    override fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        for (level in 0..2) {
            items.add(ItemStack(this, 1, level))
        }
    }

    override fun getUnlocalizedName(stack: ItemStack): String = "${super.getUnlocalizedName()}_${getMetadata(stack)}"

    override val incompatibleWith = KuarrySilkTouchUpgrade::class

    fun registerSubitemModels() {
        for (level in 0..2) {
            ModelLoader.setCustomModelResourceLocation(
                    this,
                    level,
                    ModelResourceLocation("${registryName}_$level", "inventory")
            )
        }
    }
}

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