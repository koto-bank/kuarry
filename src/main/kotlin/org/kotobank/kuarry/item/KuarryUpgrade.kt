package org.kotobank.kuarry.item

import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

open class KuarryUpgrade : Item()

class KuarryXBoundariesUpgrade : KuarryUpgrade() {
    init {
        maxStackSize = 2
    }

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.add("Upgrades the kuarry to mine one more chunk along the X dimension.")
    }
}

class KuarryZBoundariesUpgrade : KuarryUpgrade() {
    init {
        maxStackSize = 2
    }

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.add("Upgrades the kuarry to mine one more chunk along the Z dimension.")
    }
}