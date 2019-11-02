package org.kotobank.kuarry.integration

import buildcraft.api.mj.IMjConnector
import buildcraft.api.mj.IMjReceiver
import buildcraft.api.mj.MjAPI
import net.minecraftforge.energy.IEnergyStorage

/** An implementation of buildcraft's [IMjReceiver].
 *
 * It proxies the energy to the [backingStorage], converting it 10RF = 1mJ
 *
 */
class MjReceiverImpl(private val backingStorage: IEnergyStorage) : IMjReceiver {
    val capConnector = MjAPI.CAP_RECEIVER
    val capReceiver = MjAPI.CAP_CONNECTOR

    override fun canConnect(other: IMjConnector) = true

    /** Requests mj by dividing the dividing the RF amount by ten.
     *
     * mj is ten times bigger than RF apparently.
     */
    override fun getPowerRequested(): Long =
            (backingStorage.maxEnergyStored - backingStorage.energyStored) / 10L

    /** Receive mj by multiplying the received amount by ten. */
    override fun receivePower(microJoules: Long, simulate: Boolean) =
            (backingStorage.receiveEnergy(microJoules.toInt() * 10, simulate)) / 10L
}