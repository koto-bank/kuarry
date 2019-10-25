package org.kotobank.kuarry.tile_entity

import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.energy.*
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler

class KuarryTileEntity : TileEntity(), ITickable {

    companion object {
        internal const val upgradeInventoryWidth = 2
        internal const val upgradeInventoryHeight = 3

        internal const val packetEntityID = 0
    }

    private var lastEnergyStored = 0
    private val energyStorage = EnergyStorage(102400, 1024, 1024)

    internal val inventoryWidth = 9
    internal val inventoryHeight = 3

    /** A chest-sized inventory for inner item storage */
    private val inventory = ItemStackHandler(inventoryWidth * inventoryHeight)

    /** A small inventory for upgrades not exposed via getCapability */
    internal val upgradeInventory = ItemStackHandler(upgradeInventoryWidth * upgradeInventoryHeight)

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?) =
        when (capability) {
            CapabilityEnergy.ENERGY, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> true
            else -> super.hasCapability(capability, facing)
        }


    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        return when (capability) {
            CapabilityEnergy.ENERGY ->
                @Suppress("UNCHECKED_CAST")
                energyStorage as T
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ->
                @Suppress("UNCHECKED_CAST")
                inventory as T
            else ->
                super.getCapability(capability, facing)
        }
    }

    override fun readFromNBT(compound: NBTTagCompound) {
        with (compound) {
            CapabilityEnergy.ENERGY.readNBT(energyStorage, EnumFacing.NORTH, getTag("energy"))
            inventory.deserializeNBT(getCompoundTag("inventory"))
            upgradeInventory.deserializeNBT(getCompoundTag("upgrade_inventory"))
        }

        super.readFromNBT(compound)
    }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        with (compound) {
            setTag("energy", CapabilityEnergy.ENERGY.writeNBT(energyStorage, EnumFacing.NORTH)!!)
            setTag("inventory", inventory.serializeNBT())
            setTag("upgrade_inventory", upgradeInventory.serializeNBT())
        }

        return super.writeToNBT(compound)
    }

    private var updateCount = 0

    private lateinit var currentChunkPos: ChunkPos

    override fun update() {
        if (!world.isRemote) {
            updateCount = updateCount.inc()

            if (updateCount >= 5) {
                updateCount = 0

                // If the energy amount changed, send that new amount to the client
                if (lastEnergyStored != energyStorage.energyStored) {
                    lastEnergyStored = energyStorage.energyStored

                    val state = world.getBlockState(pos)
                    world.notifyBlockUpdate(pos, state, state, packetEntityID)
                    markDirty()
                }

                val chunk = world.getChunkFromBlockCoords(pos)

                if (!this::currentChunkPos.isInitialized || currentChunkPos != chunk.pos) {
                    currentChunkPos = chunk.pos
                }

                var x = currentChunkPos.xStart
                var z = currentChunkPos.zStart
                var y = pos.y - 1

                do {
                    val blockPos = BlockPos(x, y, z)
                    val blockState = chunk.getBlockState(blockPos)

                    if (blockState != Blocks.AIR.defaultState && blockState != Blocks.BEDROCK.defaultState) {
                        world.destroyBlock(blockPos, false)
                        break
                    }

                    when {
                        x < currentChunkPos.xEnd ->
                            x = x.inc()
                        z < currentChunkPos.zEnd -> {
                            x = currentChunkPos.xStart
                            z = z.inc()
                        }
                        else -> {
                            x = currentChunkPos.xStart
                            z = currentChunkPos.zStart
                            y = y.dec()
                        }
                    }
                } while (y >= 5)
                // The exit condition is to not go through bedrock
            }
        }
    }

    override fun getUpdateTag(): NBTTagCompound {
        return writeToNBT(NBTTagCompound())
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? {
        return SPacketUpdateTileEntity(pos, packetEntityID, updateTag)
    }

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        super.onDataPacket(net, pkt)
        handleUpdateTag(pkt.nbtCompound)
    }
}