package org.kotobank.kuarry

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.relauncher.Side
import org.kotobank.kuarry.packet.*

object KuarryModPackets {
    internal val networkChannel = NetworkRegistry.INSTANCE.newSimpleChannel(KuarryMod.MODID)

    internal fun register() {
        var id = 0;

        with (networkChannel) {
            registerMessage(SwitchKuarrySetting.Handler(), SwitchKuarrySetting::class.java, id++, Side.SERVER)
            registerMessage(SwitchCustomFilterSetting.Handler(), SwitchCustomFilterSetting::class.java, id++, Side.SERVER)
        }
    }
}