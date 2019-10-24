package org.kotobank.kuarry

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder

import org.kotobank.kuarry.block.KuarryBlock

@Mod.EventBusSubscriber(modid = KuarryMod.MODID)
object ModBlocks {
    @ObjectHolder("${KuarryMod.MODID}:kuarry")
    lateinit var kuarry: Block

    @SubscribeEvent
    fun registerBlocks(event: RegistryEvent.Register<Block>) {
        val kuarryBlock = KuarryBlock(Material.IRON, "kuarry")

        event.registry.registerAll(kuarryBlock)

        GameRegistry.registerTileEntity(KuarryBlock.tileEntityClass, kuarryBlock.registryName)
    }
}