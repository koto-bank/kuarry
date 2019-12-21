package org.kotobank.kuarry.tile_entity.kuarry_component

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack

class XPCollectorComponent {

    /** Fluids viable for generation. */
    private val resultFluids by lazy {
        listOfNotNull(
                // Essence of knowledge from ThermalExpansion
                FluidRegistry.getFluid("experience"),
                // Liquid XP from EnderIO
                FluidRegistry.getFluid("xpjuice")
        )
    }

    val enabled
        get() = resultFluids.isNotEmpty()

    fun xpFromBlock(block: Block, state: IBlockState, world: IBlockAccess, pos: BlockPos, fortuneLevel: Int): FluidStack? {
        return resultFluids.firstOrNull()?.let {
            val expDrop = block.getExpDrop(state, world, pos, fortuneLevel)

            FluidStack(it, expDrop * 20)
        }
    }
}