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
object KuarryModItems {
    @ObjectHolder("${KuarryMod.MODID}:kuarry")
    lateinit var kuarry: Item

    @ObjectHolder("${KuarryMod.MODID}:denatured_stone")
    lateinit var denatured_stone: Item

    @SubscribeEvent
    fun registerItems(event: Register<Item>) {

        listOf(
                Pair("kuarry", ItemBlock(KuarryModBlocks.kuarry)),
                Pair("denatured_stone", ItemBlock(KuarryModBlocks.denatured_stone)),

                Pair("kuarry_casing", Item())
        ).forEach { (name, item) ->
            item.apply {
                setRegistryName(KuarryMod.MODID, name)
                setUnlocalizedName(name)

                event.registry.register(this)

                ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!, "inventory"))
            }
        }
    }
}