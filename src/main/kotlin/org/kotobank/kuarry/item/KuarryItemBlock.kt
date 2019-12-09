package org.kotobank.kuarry.item

import net.minecraft.block.Block
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack

class KuarryItemBlock(block: Block) : ItemBlock(block) {
    override fun getUnlocalizedName(stack: ItemStack): String {
        val blockName = super.getUnlocalizedName(stack)

        // Get the name based on the level
        return when (val level = stack.tagCompound?.getInteger("upgrade_level") ?: 0) {
            1, 2 ->
                "${blockName}.level_${level + 1}"
            else -> super.getUnlocalizedName(stack)
        }
    }
}