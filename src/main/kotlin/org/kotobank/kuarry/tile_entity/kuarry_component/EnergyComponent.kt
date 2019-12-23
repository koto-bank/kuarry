package org.kotobank.kuarry.tile_entity.kuarry_component

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.fml.common.Loader
import org.kotobank.kuarry.integration.MjReceiverImpl
import org.kotobank.kuarry.item.KuarryUpgrade
import org.kotobank.kuarry.item.LevelValues
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class EnergyComponent(private val parent: KuarryTileEntity) {
    companion object {
        /** The base amount of energy per block mined. */
        internal const val baseRequiredEnergy = 1000
    }

    /** The amount of energy stored as of previous tick.
     *
     * Used to check if there's a need to notify the client about the new amount of energy.
     */
    private var lastEnergyStored = 0

    /** The apparent energy capacity based on the current level. */
    private val energyStorageCapacity
        get() = LevelValues[parent.upgradeLevel].energy

    /** The energy storage of the tile entity. */
    private val energyStorage = object : EnergyStorage(energyStorageCapacity, 2000, 5000) {
        /** The capacity of the storage that can also be changed. */
        var capacity
            get() = super.capacity
            set(value) { super.capacity = value }
    }

    /** Tries to extract energy from the energy storage.
     *
     * @return if there is enough energy, it is extracted wholly and true is returned;
     *         if there is not enough, then none is extracted and false is returned.
     */
    fun tryExtractingEnergy(requiredEnergy: Int): Boolean {
        // Not enough energy to mine the block, skip it
        if (energyStorage.energyStored < requiredEnergy) return false

        var requiredEnergyProcessing = requiredEnergy

        // Take out all the energy required, in multiple iterations if needed (by subtracting the
        // already extracted energy from the rest)
        while (requiredEnergyProcessing > 0) {
            requiredEnergyProcessing -= energyStorage.extractEnergy(requiredEnergyProcessing, false)
        }

        return true
    }

    /** IMjReceiver implementation for BuildCraft compatibility */
    private val mjEnergyStorage: MjReceiverImpl? by lazy {
        if (Loader.isModLoaded("buildcraftcore"))
            MjReceiverImpl(energyStorage)
        else
            null
    }

    /** Updates the energy storage capacity with the new level's value. */
    fun onUpgradeLevelChanged() {
        energyStorage.capacity = energyStorageCapacity
    }

    fun hasCapability(capability: Capability<*>): Boolean =
            (capability == CapabilityEnergy.ENERGY) ||
                    (Loader.isModLoaded("buildcraftcore") &&
                            (capability == MjReceiverImpl.capConnector || capability == MjReceiverImpl.capReceiver))

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getCapability(capability: Capability<T>): T? =
            when {
                capability == CapabilityEnergy.ENERGY -> energyStorage as T
                (Loader.isModLoaded("buildcraftcore") &&
                        (capability == MjReceiverImpl.capConnector || capability == MjReceiverImpl.capReceiver)) ->
                    mjEnergyStorage as T
                else -> null
            }

    fun writeToNBT(compound: NBTTagCompound) {
        compound.apply {
            setTag("energy", CapabilityEnergy.ENERGY.writeNBT(energyStorage, EnumFacing.NORTH)!!)
        }
    }

    fun readFromNBT(compound: NBTTagCompound) {
        compound.apply {
            CapabilityEnergy.ENERGY.readNBT(energyStorage, EnumFacing.NORTH, getTag("energy"))
        }
    }

    /** Calculates the energy needed with upgrades, starting from [currentEnergy] as a base. */
    private fun calculateRequiredEnergyForUpgrades(currentEnergy: Int): Int {
        var energy = currentEnergy

        // Go through the upgrade inventory and add all the energy required by the upgrades
        for (i in 0 until parent.upgradeInventoryComponent.upgradeInventorySize) {
            val stack = parent.upgradeInventoryComponent.upgradeInventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                val item = stack.item
                require(item is KuarryUpgrade)

                energy = item.energyUsageWithUpgrade(energy, stack)
            }
        }

        return energy
    }

    /** Calculates the energy required to mine the block. */
    fun calculateRequiredEnergyForBlock(block: Block, blockState: IBlockState): Int {
        // Harvesting something with pick/shovel should not cost more,
        // otherwise double the energy count
        val toolHarvestModifier = when (block.getHarvestTool(blockState)) {
            "pickaxe", "shovel", null -> 1
            else -> 2
        }
        // Don't want to have 0 or -1 as a modifier, as it would break the math later on, so make it 1
        val levelHarvestModifier = block.getHarvestLevel(blockState).let { if (it <= 0) 1 else it }


        // Get the starting energy from all the other modifiers

        return calculateRequiredEnergyForUpgrades(
                baseRequiredEnergy * toolHarvestModifier * levelHarvestModifier
        )
    }

    /** The required energy to collect fluids only depends on installed upgrades, since fluids
     * don't need a tool to be collected and don't have hardness.
     */
    fun calculateRequiredEnergyForFluid()= calculateRequiredEnergyForUpgrades(baseRequiredEnergy)

    /** Checks if the energy amount has changed since the last tick.
     *
     * Should run in the [KuarryTileEntity.update].
     */
    fun update() {
        // If the energy amount changed, send that new amount to the client
        if (lastEnergyStored != energyStorage.energyStored) {
            lastEnergyStored = energyStorage.energyStored

            parent.notifyClientAndMarkDirty()
        }
    }
}