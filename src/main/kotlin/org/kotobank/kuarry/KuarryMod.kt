package org.kotobank.kuarry

import net.minecraftforge.fml.common.event.*
import net.minecraftforge.fml.common.Mod
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.apache.logging.log4j.Logger

@Mod(modid = KuarryMod.MODID, name = KuarryMod.NAME, version = KuarryMod.VERSION, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter")
object KuarryMod {
    const val MODID = "kuarry"
    const val NAME = "Kuarry"
    const val VERSION = "0.1"

    internal lateinit var logger: Logger

    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        KuarryMod.logger = event.modLog

        NetworkRegistry.INSTANCE.registerGuiHandler(this, ModGUIHandler())
    }
}
