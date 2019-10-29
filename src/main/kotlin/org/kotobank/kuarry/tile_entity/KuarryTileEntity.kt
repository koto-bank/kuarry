package org.kotobank.kuarry.tile_entity

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ITickable
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.energy.*
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.KuarryMod

class KuarryTileEntity : TileEntity(), ITickable {
    companion object {
        internal const val upgradeInventoryWidth = 2
        internal const val upgradeInventoryHeight = 3

        internal const val packetEntityID = 0

        /** The blocks that cannot be un-blacklisted by an external filter */
        private val hardBlacklistedBlocks = listOf(
                Blocks.BEDROCK,
                Blocks.AIR
        )

        /** The default blacklist of blocks */
        private val defaultBlacklistedBlocks = listOf(
                Blocks.GRASS,
                Blocks.GRASS_PATH,
                Blocks.DIRT,
                Blocks.STONE,
                Blocks.MONSTER_EGG,
                Blocks.GRAVEL,
                Blocks.SAND,
                Blocks.SANDSTONE,
                Blocks.END_STONE,
                Blocks.NETHERRACK,

                Blocks.TALLGRASS,
                Blocks.SNOW_LAYER,

                Blocks.WATER,
                Blocks.FLOWING_WATER,
                Blocks.LAVA,
                Blocks.FLOWING_LAVA
        )

        internal const val baseRequiredEnergy = 1000
    }

    private var lastEnergyStored = 0
    private val energyStorage = EnergyStorage(100000, 2000)

    internal val inventoryWidth = 9
    internal val inventoryHeight = 3
    internal val inventorySize = inventoryWidth * inventoryHeight

    /** A chest-sized inventory for inner item storage */
    private val inventory = ItemStackHandler(inventorySize)

    /** A small inventory for upgrades not exposed via getCapability */
    internal val upgradeInventory = ItemStackHandler(upgradeInventoryWidth * upgradeInventoryHeight)

    enum class ActivationMode {
        AlwaysOn, EnableWithRS, DisableWithRS, AlwaysOff
    }
    internal var activationMode = ActivationMode.AlwaysOn

    /** Switches the [activationMode] circularly and notifies the client about the change.
     *
     * In order, [ActivationMode.AlwaysOn] -> [ActivationMode.EnableWithRS] -> [ActivationMode.DisableWithRS] -> [ActivationMode.AlwaysOff], and then back
     */
    internal fun switchActivationMode() {
        activationMode = when (activationMode) {
            ActivationMode.AlwaysOn -> ActivationMode.EnableWithRS
            ActivationMode.EnableWithRS -> ActivationMode.DisableWithRS
            ActivationMode.DisableWithRS -> ActivationMode.AlwaysOff
            ActivationMode.AlwaysOff -> ActivationMode.AlwaysOn
        }

        notifyClientAndMarkDirty()
    }

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

    override fun readFromNBT(compound: NBTTagCompound): Unit =
            super.readFromNBT(
                    compound.apply {
                        CapabilityEnergy.ENERGY.readNBT(energyStorage, EnumFacing.NORTH, getTag("energy"))
                        inventory.deserializeNBT(getCompoundTag("inventory"))
                        upgradeInventory.deserializeNBT(getCompoundTag("upgrade_inventory"))

                        activationMode = ActivationMode.valueOf(getString("activation_mode"))
                    }
            )

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound =
            super.writeToNBT(
                    compound.apply {
                        setTag("energy", CapabilityEnergy.ENERGY.writeNBT(energyStorage, EnumFacing.NORTH)!!)
                        setTag("inventory", inventory.serializeNBT())
                        setTag("upgrade_inventory", upgradeInventory.serializeNBT())

                        setString("activation_mode", activationMode.name)
                    }
            )

    override fun getUpdateTag(): NBTTagCompound =
            writeToNBT(NBTTagCompound()).apply {
                // These are sent to client but are ephemeral and reset on world reload

                setInteger("approx_res_count", approxResourceCount)
                setInteger("approx_res_mined", approxResourcesMined)
            }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? =
            SPacketUpdateTileEntity(pos, packetEntityID, updateTag)

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        super.onDataPacket(net, pkt)

        // Handle the usual non-ephemeral properties
        handleUpdateTag(pkt.nbtCompound)

        // Handle the properties that are not saved and therefore are not in readFromNBT

        with(pkt.nbtCompound) {
            approxResourceCount = getInteger("approx_res_count")
            approxResourcesMined = getInteger("approx_res_mined")
        }
    }

    /** Notify the client about the change in the block data. */
    private fun notifyClient() {
        val state = world.getBlockState(pos)
        world.notifyBlockUpdate(pos, state, state, packetEntityID)
    }

    /** Both notify the client about the change in block data and mark the block dirty for the game to save it. */
    private fun notifyClientAndMarkDirty() {
        notifyClient()
        markDirty()
    }

    /** An approximate total number of resources.
     *
     * Ephemeral, resets on world reload.
     */
    internal var approxResourceCount = -1

    /** An approximate number of resources mined since world loading.
     *
     * The value is ephemeral and is reset on world reload.
     */
    internal var approxResourcesMined = 0

    private var updateCount = 0

    /** Tick count for the resource-count-in-the-chunk updates */
    private var resourceCountUpdateCount = 0

    override fun update() {
        if (!world.isRemote) {
            updateCount = updateCount.inc()

            if (updateCount >= 50) {
                updateCount = 0

                // If the energy amount changed, send that new amount to the client
                if (lastEnergyStored != energyStorage.energyStored) {
                    lastEnergyStored = energyStorage.energyStored

                    notifyClientAndMarkDirty()
                }

                when (activationMode) {
                    ActivationMode.AlwaysOn -> {}
                    ActivationMode.AlwaysOff -> return
                    ActivationMode.DisableWithRS -> if (world.isBlockPowered(pos)) return
                    ActivationMode.EnableWithRS -> if (!world.isBlockPowered(pos)) return
                }

                val chunk = world.getChunkFromBlockCoords(pos)

                // Process all blocks in the chunk
                doWithAllBlocksInChunk(chunk, ::processBlock)
            }

            // 6000 ~= 5 minutes
            if (resourceCountUpdateCount >= 6000 || approxResourceCount == -1) {
                resourceCountUpdateCount++

                val chunk = world.getChunkFromBlockCoords(pos)

                approxResourceCount = countAllMinable(chunk)

                notifyClient()
            }
        }
    }

    /** Counts the all the minable resources in the chunk for user-faced stats. */
    private fun countAllMinable(chunk: Chunk): Int {
        var amountOfBlocks = 0

        doWithAllBlocksInChunk(chunk) { _, blockState ->
            if (!isBlockBlacklisted(blockState.block)) {
                amountOfBlocks++
            }

            // Continue until there are no more blocks in the chunk
            false
        }

        return amountOfBlocks
    }

    /** Iterate over all (until y level 5) blocks calling the function on these blocks for side effects.
     *
     * If the functions returns true, the iteration is stopped and the function returns.
     */
    private fun doWithAllBlocksInChunk(chunk: Chunk, func: (blockPos: BlockPos, blockState: IBlockState) -> Boolean) {
        val chunkPos = chunk.pos

        var x = chunkPos.xStart
        var z = chunkPos.zStart
        var y = pos.y - 1

        do {
            val blockPos = BlockPos(x, y, z)
            val blockState = chunk.getBlockState(blockPos)

            // Exit whenever the function returns true
            if (func(blockPos, blockState)) break

            when {
                x < chunkPos.xEnd ->
                    x++
                z < chunkPos.zEnd -> {
                    x = chunkPos.xStart
                    z++
                }
                else -> {
                    x = chunkPos.xStart
                    z = chunkPos.zStart
                    y--
                }
            }
        } while (y >= 5)
        // The exit condition is to not go through bedrock
    }

    /** Checks if the block is blacklisted by some filter and should not be mined. */
    private fun isBlockBlacklisted(block: Block): Boolean {
        val fullBlacklist = hardBlacklistedBlocks + defaultBlacklistedBlocks

        return (block in fullBlacklist || block is IFluidBlock)
    }

    /** Processes a single block.
     *
     * @return Whether the block was mined or not
     */
    private fun processBlock(blockPos: BlockPos, blockState: IBlockState): Boolean {
        val block = blockState.block

        // Skip the block if it's blacklisted or is a fluid
        if (isBlockBlacklisted(block)) return false

        // Harvesting something with pick/shovel should not cost more,
        // otherwise double the energy count
        val toolHarvestModifier = when (block.getHarvestTool(blockState)) {
            "pickaxe", "shovel", null -> 1
            else -> 2
        }
        // Don't want to have 0 as a modifier, as it would break the math later on, so make it 1
        val levelHarvestModifier = block.getHarvestLevel(blockState).let { if (it == 0) 1 else it }


        var requiredEnergy = baseRequiredEnergy * toolHarvestModifier * levelHarvestModifier

        // Not enough energy to mine the block, skip it
        if (energyStorage.energyStored < requiredEnergy) return false

        KuarryMod.logger.info(blockState.block.localizedName)

        // Take out all the energy required, in multiple iterations if needed (by subtracting the
        // already extracted energy from the rest)
        while (requiredEnergy > 0) {
            requiredEnergy -= energyStorage.extractEnergy(requiredEnergy, false)
        }

        var drops = NonNullList.create<ItemStack>()
        block.getDrops(drops, world, pos, blockState, 0)

        // Some blocks don't drop anything, so check if there are any drops at all
        if (drops.isNotEmpty()) {
            var allPut = false
            slotLoop@ for (i in 0 until inventoryHeight) {
                // The width counter, reset every step.
                // Not advanced UNLESS the item cannot be put into the slot.
                // This ensures that if there are multiple ItemStacks that can be put into the
                // same slot, they will be.
                var j = 0
                while (j < inventoryWidth) {
                    val positionInInventory = (j * inventoryHeight) + i

                    if (inventory.insertItem(positionInInventory, drops.first(), false) == ItemStack.EMPTY) {
                        // If the item has successfully been put into the inventory, remove it from the drops list
                        drops.removeAt(0)

                        // Exit when there are no more items
                        if (drops.isEmpty()) {
                            allPut = true
                            break@slotLoop
                        }
                    } else {
                        // The item could not have been put, advance the width slot
                        j++
                    }
                }
            }
            if (!allPut) {
                for (remainingDrop in drops) {
                    // If there is no space inside the kuarry, spawn all dropped items
                    // as entities in the world on top of the kuarry block
                    world.spawnEntity(
                            EntityItem(world, pos.x.toDouble(), (pos.y + 1).toDouble(), pos.z.toDouble(), remainingDrop)
                    )
                }
            }
        }

        // TODO: something smart here
        world.setBlockState(blockPos, Blocks.STONE.defaultState)

        // Increment the mined resource amount and notify the client about it
        approxResourcesMined++
        notifyClient()

        return true
    }
}