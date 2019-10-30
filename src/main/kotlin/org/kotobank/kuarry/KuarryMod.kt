package org.kotobank.kuarry

import net.minecraftforge.fml.common.event.*
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.network.NetworkRegistry
import org.apache.logging.log4j.Logger
import org.kotobank.kuarry.integration.TheOneProbeIntegration

@Mod(modid = KuarryMod.MODID, name = KuarryMod.NAME, version = KuarryMod.VERSION, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter",
        dependencies = "after:theoneprobe")
object KuarryMod {
    const val MODID = "kuarry"
    const val NAME = "Kuarry"
    const val VERSION = "0.1"

    internal lateinit var logger: Logger

    @EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger = event.modLog

        NetworkRegistry.INSTANCE.registerGuiHandler(this, KuarryModGUIHandler())

        KuarryModPackets.register()

        if (Loader.isModLoaded("theoneprobe")) {
            TheOneProbeIntegration()
        }
    }
}
