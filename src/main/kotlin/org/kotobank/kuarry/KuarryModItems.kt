package org.kotobank.kuarry

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent.Register
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder
import org.kotobank.kuarry.item.*

@Mod.EventBusSubscriber(modid = KuarryMod.MODID)
object KuarryModItems {
    @ObjectHolder("${KuarryMod.MODID}:kuarry")
    lateinit var kuarry: Item

    @SubscribeEvent
    fun registerItems(event: Register<Item>) {
        val upgrades =
                listOf(
                        Pair("x_boundaries_upgrade", KuarryXBoundariesUpgrade()),
                        Pair("z_boundaries_upgrade", KuarryZBoundariesUpgrade()),
                        Pair("silk_touch_upgrade", KuarrySilkTouchUpgrade()),
                        Pair("custom_filter", KuarryCustomFilter()),
                        Pair("speed_upgrade", KuarrySpeedUpgrade())
                )
        // Set each upgrade's max stack size
        upgrades.forEach { (_, item) -> item.setMaxStackSize(item.stackSize) }

        val items =
                listOf(
                        Pair("kuarry", ItemBlock(KuarryModBlocks.kuarry)),
                        Pair("denatured_stone", ItemBlock(KuarryModBlocks.denatured_stone)),

                        Pair("kuarry_casing", Item())
                )

        (items + upgrades).forEach { (name, item) ->
            item.apply {
                setRegistryName(KuarryMod.MODID, name)
                setUnlocalizedName(name)

                event.registry.register(this)

                ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!, "inventory"))
            }
        }

        // The luck upgrade is a special case: it has subitems and the models have to be registered
        // for their names and not for the base name
        with(KuarryLuckUpgrade()) {
            setMaxStackSize(stackSize)
            setRegistryName(KuarryMod.MODID, "luck_upgrade")
            setUnlocalizedName("luck_upgrade")
            event.registry.register(this)
            registerSubitemModels()
        }
    }
}