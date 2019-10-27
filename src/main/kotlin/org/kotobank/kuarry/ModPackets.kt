package org.kotobank.kuarry

import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.relauncher.Side
import org.kotobank.kuarry.packet.*

object ModPackets {
    internal val networkChannel = NetworkRegistry.INSTANCE.newSimpleChannel(KuarryMod.MODID)

    internal fun register() {
        var id = 0;
        networkChannel.registerMessage(ChangeKuarryActivationMode.Handler(), ChangeKuarryActivationMode::class.java, id++, Side.SERVER)
    }
}