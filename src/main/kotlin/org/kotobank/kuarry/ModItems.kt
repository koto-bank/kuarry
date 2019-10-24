package org.kotobank.kuarry

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent.Register
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder

@Mod.EventBusSubscriber(modid = KuarryMod.MODID)
object ModItems {
    @ObjectHolder("${KuarryMod.MODID}:kuarry")
    lateinit var kuarry: Item

    @SubscribeEvent
    fun registerItems(event: Register<Item>) {
        val item = ItemBlock(ModBlocks.kuarry).setRegistryName(KuarryMod.MODID, "kuarry")
        event.registry.registerAll(item)

        ModelLoader.setCustomModelResourceLocation(item, 0, ModelResourceLocation(item.registryName, "inventory"))
    }
}