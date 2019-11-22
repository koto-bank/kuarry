package org.kotobank.kuarry.block

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
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

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, player: World?, tooltip: MutableList<String>, advanced: ITooltipFlag) {
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(
                    "Denatured stone is proven to be a very useful material: it can be a cheap papier mache, " +
                            "it can be smashed to pieces, or it can be the subject of a ten year long \"How can we use denatured stone?\" research with an immense budget. " +
                            "Most importantly, however, is that it can sometimes drop gravel when broken."
            )
        } else {
            tooltip.add("[Hold ${TextFormatting.YELLOW}${TextFormatting.ITALIC}Shift${TextFormatting.RESET} for a detailed description]")
        }
    }
}