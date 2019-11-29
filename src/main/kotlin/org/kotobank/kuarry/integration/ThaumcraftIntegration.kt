package org.kotobank.kuarry.integration

import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.kotobank.kuarry.KuarryModBlocks
import org.kotobank.kuarry.KuarryModItems
import thaumcraft.api.aspects.Aspect
import thaumcraft.api.aspects.AspectHelper
import thaumcraft.api.aspects.AspectList
import thaumcraft.api.aspects.AspectRegistryEvent

object ThaumcraftIntegration {
    fun registerForTCEvents() = MinecraftForge.EVENT_BUS.register(this::class.java)

    @SubscribeEvent
    @JvmStatic
    fun registerAspects(event: AspectRegistryEvent) {
        with (event.register) {
            registerObjectTag(ItemStack(KuarryModBlocks.denatured_stone), AspectList().add(Aspect.VOID, 1))

            registerObjectTag(
                    ItemStack(KuarryModItems.kuarry_casing),
                    AspectList()
                            .add(Aspect.MECHANISM, 6)
                            .add(Aspect.METAL, 6)
            )
            registerObjectTag(
                    ItemStack(KuarryModItems.z_boundaries_upgrade),
                    AspectHelper.getObjectAspects(ItemStack(KuarryModItems.x_boundaries_upgrade))
            )
        }
    }
}