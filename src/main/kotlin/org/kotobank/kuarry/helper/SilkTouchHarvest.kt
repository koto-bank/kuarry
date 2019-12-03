package net.minecraft.block

import net.minecraft.block.state.IBlockState
import net.minecraft.item.ItemStack

/** The class that resides in the [net.minecraft.block] package to gain access to the getSilkTouchDrop.
 *
 * For some reason this method is protected, but JVM allows access to protected methods
 * within the same package so this works.
 *
 * FIXME: find a better way to do this? This is obviously bad.
 */
internal object SilkTouchHarvest {
    fun getSilkTouchDrop(block: Block, blockState: IBlockState): ItemStack =
            block.getSilkTouchDrop(blockState)
}