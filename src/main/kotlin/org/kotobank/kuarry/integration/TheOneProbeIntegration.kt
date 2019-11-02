package org.kotobank.kuarry.integration

import mcjty.theoneprobe.api.*
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.fml.common.event.FMLInterModComms
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.block.KuarryBlock
import org.kotobank.kuarry.tile_entity.KuarryTileEntity
import java.util.function.Function

class TheOneProbeIntegration {
    init {
        FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", GetTheOneProbe::class.java.name)
    }

    class GetTheOneProbe : Function<ITheOneProbe, Unit>  {
        override fun apply(iProbe: ITheOneProbe) {
            iProbe.registerProvider(
                    object : IProbeInfoProvider {
                        override fun getID() = "${KuarryMod.MODID}:kuarry_provider"
                        override fun addProbeInfo(mode: ProbeMode, probeInfo: IProbeInfo, player: EntityPlayer, world: World, blockState: IBlockState, data: IProbeHitData) {
                            if (mode == ProbeMode.EXTENDED && blockState.block is KuarryBlock) {
                                val te = world.getTileEntity(data.pos)
                                if (te is KuarryTileEntity && te.approxResourcesLeft != -1) {
                                    probeInfo.horizontal()
                                            .text("Approx. left: ${te.approxResourcesLeft}")
                                }
                            }
                        }
                    }
            )
        }
    }
}