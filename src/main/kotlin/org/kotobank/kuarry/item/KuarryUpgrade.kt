package org.kotobank.kuarry.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
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

