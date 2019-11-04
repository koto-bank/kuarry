package org.kotobank.kuarry.item

import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

abstract class KuarryUpgrade : Item() {
    open val stackSize = 1

    init {
        maxStackSize = stackSize;
    }

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
    override val tooltipStrings = listOf("Makes the kuarry retrieve the blocks as they are, instead of mining them")
}