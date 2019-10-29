package org.kotobank.kuarry.block

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import org.kotobank.kuarry.KuarryMod
import java.util.Random

class DenaturedStoneBlock(material: Material, registryName: String) : Block(material) {
    companion object {
        private val random = Random()
    }

    init {
        setUnlocalizedName(registryName)

        setRegistryName(KuarryMod.MODID, registryName)

        setHardness(0.2f)
        setHarvestLevel("pickaxe", 0)
    }

    override fun getDrops(drops: NonNullList<ItemStack>, world: IBlockAccess, pos: BlockPos, state: IBlockState, fortune: Int) {
        if (random.nextInt(5) == 0) {
            drops.add(ItemStack(Blocks.GRAVEL))
        }
    }
}