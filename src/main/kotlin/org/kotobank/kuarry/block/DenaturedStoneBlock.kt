package org.kotobank.kuarry.block

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.helper.TranslationHelper
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

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, player: World?, tooltip: MutableList<String>, advanced: ITooltipFlag) {
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(I18n.format("tile.denatured_stone.description"))
        } else {
            tooltip.add(TranslationHelper.shiftForDetailsTooltip)
        }
    }
}