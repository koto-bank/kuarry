package org.kotobank.kuarry.tile_entity

import net.minecraft.block.Block
import net.minecraft.block.SilkTouchHarvest
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityItem
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.*
import net.minecraft.util.math.*
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.energy.*
import net.minecraftforge.items.*
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fml.common.Loader
import org.kotobank.kuarry.*
import org.kotobank.kuarry.integration.autopushing.Autopusher
import org.kotobank.kuarry.item.*
import org.kotobank.kuarry.integration.MjReceiverImpl
import kotlin.reflect.KClass

class KuarryTileEntity : TileEntity(), ITickable {
    companion object {
        internal const val upgradeInventoryWidth = 2
        internal const val upgradeInventoryHeight = 3
        internal const val upgradeInventorySize = upgradeInventoryWidth * upgradeInventoryHeight

        internal const val packetEntityID = 0

        /** The blocks that cannot be un-blacklisted by an external filter */
        private val hardBlacklistedBlocks = listOf(
                Blocks.BEDROCK,
                Blocks.AIR,

                KuarryModBlocks.denatured_stone
        )

        /** The default blacklist of blocks */
        internal val defaultBlacklistedBlocks = listOf(
                Blocks.GRASS,
                Blocks.MYCELIUM,
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
                Blocks.SNOW_LAYER
        )

        internal const val baseRequiredEnergy = 1000

        internal const val inventoryWidth = 9
        internal const val inventoryHeight = 3
        internal const val inventorySize = inventoryWidth * inventoryHeight
    }

    private var lastEnergyStored = 0
    private val energyStorage = EnergyStorage(100000, 2000, 5000)

    /** IMjReceiver implementation for BuildCraft compatibility */
    private val mjEnergyStorage: MjReceiverImpl? by lazy {
        if (Loader.isModLoaded("buildcraftcore"))
            MjReceiverImpl(energyStorage)
        else
            null
    }

    /** A chest-sized inventory for inner item storage */
    internal val inventory = object : ItemStackHandler(inventorySize) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            markDirty()
        }
    }

    /** A small inventory for upgrades not exposed via getCapability */
    internal val upgradeInventory = object : ItemStackHandler(upgradeInventoryWidth * upgradeInventoryHeight) {
        override fun onContentsChanged(slot: Int) {
            super.onContentsChanged(slot)
            markDirty()
        }
    }

    private val autopusher by lazy {
        if (Autopusher.isEnabled)
            Autopusher(this, inventory, inventoryWidth, inventoryHeight)
        else
            null
    }

    /** Activation mode of the block.
     *
     * [ActivationMode.AlwaysOn] - Always enabled
     * [ActivationMode.AlwaysOff] - Always disabled
     * [ActivationMode.EnableWithRS] - Only enabled with redstone
     * [ActivationMode.DisableWithRS] - Only enabled when there without redstone
     */
    enum class ActivationMode {
        AlwaysOn, EnableWithRS, DisableWithRS, AlwaysOff
    }
    /** The kuarry [ActivationMode].
     *
     * Changing this notifies the client about the change.
     */
    internal var activationMode = ActivationMode.AlwaysOn
        set(value) {
            field = value

            notifyClientAndMarkDirty()
        }

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
    }

    /** Whether the bounds of the mined region should be rendered.
     *
     * Ephemeral, but sent to the client. There's no reason to save this, since it's essentially
     * just for the people to see the where the digging would happen
     */
    internal var renderBounds = false

    /** Toggles the [renderBounds] on or off */
    internal fun toggleRenderBounds() {
        renderBounds = !renderBounds

        notifyClient()
    }

    /** Whether to automatically push item to pipes. */
    internal var autopush = false
        get() =
            Autopusher.isEnabled && field
        set(value) {
            field = value

            notifyClientAndMarkDirty()
        }

    /** Toggles [autopush] on or off. */
    internal fun toggleAutopush() {
        autopush = !autopush
    }

    /** Whether the resource count should be updated in the next [update] run. */
    private var shouldUpdateResourcesLeft = true

    /** Recalculates [approxResourcesLeft]. */
    private fun updateResourcesLeft() {
        approxResourcesLeft = countAllMinable(calculateMinedChunks())

        // Mark the block dirty to make comparator see it and
        // notify the client about the amount of resources
        notifyClientAndMarkDirty()
    }

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        return when (capability) {
            CapabilityEnergy.ENERGY, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY -> true
            else -> when {
                Loader.isModLoaded("buildcraftcore") &&
                        (capability == MjReceiverImpl.capConnector || capability == MjReceiverImpl.capReceiver) -> true
                else -> super.hasCapability(capability, facing)
            }

        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        return when (capability) {
            CapabilityEnergy.ENERGY ->
                energyStorage as T
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ->
                inventory as T
            else -> when {
                Loader.isModLoaded("buildcraftcore") &&
                        (capability == MjReceiverImpl.capConnector || capability == MjReceiverImpl.capReceiver) ->
                    mjEnergyStorage as T
                else -> super.getCapability(capability, facing)
            }
        }
    }

    /** Write NBT data that is mod's and not game's. */
    internal fun writeModNBT(compound: NBTTagCompound): NBTTagCompound =
            compound.apply {
                setTag("energy", CapabilityEnergy.ENERGY.writeNBT(energyStorage, EnumFacing.NORTH)!!)
                setTag("inventory", inventory.serializeNBT())
                setTag("upgrade_inventory", upgradeInventory.serializeNBT())

                setString("activation_mode", activationMode.name)
                setBoolean("autopush", autopush)
            }

    /** Read mod's NBT data, but not game's NBT data. */
    internal fun readModNBT(compound: NBTTagCompound): Unit {
        compound.apply {
            CapabilityEnergy.ENERGY.readNBT(energyStorage, EnumFacing.NORTH, getTag("energy"))
            inventory.deserializeNBT(getCompoundTag("inventory"))
            upgradeInventory.deserializeNBT(getCompoundTag("upgrade_inventory"))

            activationMode = ActivationMode.valueOf(getString("activation_mode"))
            autopush = getBoolean("autopush")
        }
    }

    override fun readFromNBT(compound: NBTTagCompound): Unit =
            super.readFromNBT(compound).also { readModNBT(compound) }

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound =
            super.writeToNBT(compound).let(::writeModNBT)

    override fun getUpdateTag(): NBTTagCompound =
            writeToNBT(NBTTagCompound()).apply {
                // These are sent to client but are ephemeral and reset on world reload

                setInteger("approx_res_left", approxResourcesLeft)
                setBoolean("render_bounds", renderBounds)
            }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? =
            SPacketUpdateTileEntity(pos, packetEntityID, updateTag)

    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        super.onDataPacket(net, pkt)

        // Handle the usual non-ephemeral properties
        handleUpdateTag(pkt.nbtCompound)

        // Handle the properties that are not saved and therefore are not in readFromNBT

        with(pkt.nbtCompound) {
            approxResourcesLeft = getInteger("approx_res_left")
            renderBounds = getBoolean("render_bounds")
        }
    }

    /** Notify the client about the change in the block data. */
    private fun notifyClient() {
        // When is is called, the "world" might be null if it has not been initialized yet
        // (e.g. when the world is loaded and the data is set from NBT)
        world?.getBlockState(pos)?.let { state ->
            world.notifyBlockUpdate(pos, state, state, packetEntityID)
        }
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
    internal var approxResourcesLeft = -1

    /** Tick count for general updating stuff, to not do them 20 times a second. */
    private var updateCount = 0

    /** Tick count for general updates, to not run them too much */
    private var mineUpdateCount = 0

    /** Tick count for the resource-count-in-the-chunk updates */
    private var resourcesLeftUpdateCount = 0

    /** Finds an upgrade in the inventory, or null if there was none.
     *
     * The inventory _should_ not contain more than one [ItemStack] of any upgrade,
     * therefore this function returns the first upgrade found.
     */
    internal fun upgradeInInventory(upgradeC: KClass<out Item>): ItemStack? {
        for (i in 0 until upgradeInventorySize) {
            val slotItemStack = upgradeInventory.getStackInSlot(i)
            if (!slotItemStack.isEmpty && slotItemStack.item::class == upgradeC) {
                return slotItemStack
            }
        }

        return null
    }

    /** Counts the amount of upgrades of specified type in the upgrade inventory. */
    internal fun upgradeCountInInventory(upgradeC: KClass<out Item>): Int =
            upgradeInInventory(upgradeC)?.count ?: 0

    /** A convenience function with a generic parameter that calls the [KClass]-receiving version. */
    internal inline fun <reified T : KuarryUpgrade> upgradeCountInInventory() = upgradeCountInInventory(T::class)

    /** Count the amount of chunks added to X and Z by upgrades.
     *
     * The amount is equal to the amount of these upgrades, this function is
     * a convenient way to get both values.
     */
    fun xzChunkExpansion(): Pair<Int, Int> = Pair(
            upgradeCountInInventory<KuarryXBoundariesUpgrade>(),
            upgradeCountInInventory<KuarryZBoundariesUpgrade>()
    )

    /** Find and put in a list the chunks to be mined.
     *
     * The first chunk has the smallest coordinates and is used as the beginning
     * when calculating the mining zone. The last chunk has the biggest coordinates
     * and is used as the ending of the mining zone. These two might be the same chunk, if
     * there are no upgrades.
     */
    private fun calculateMinedChunks(): List<Chunk> {
        val (additionalXChunks, additionalZChunks) = xzChunkExpansion()

        val kuarryChunk = world.getChunkFromBlockCoords(pos)
        var chunks = mutableListOf<Chunk>()
        for (x in 0..additionalXChunks) {
            for (z in 0..additionalZChunks) {
                chunks.add(world.getChunkFromChunkCoords(kuarryChunk.x + x, kuarryChunk.z + z))
            }
        }
        // Supposedly, when chunks are added like this, they are always sorted, so the last one
        // in the list should have the biggest coordinates (the end coordinates basically)

        return chunks
    }

    override fun update() {
        if (!world.isRemote) {
            updateCount++
            mineUpdateCount++
            resourcesLeftUpdateCount++

            // Check everything ~4 times a second
            if (updateCount < 5) return
            updateCount = 0

            // If the energy amount changed, send that new amount to the client
            if (lastEnergyStored != energyStorage.energyStored) {
                lastEnergyStored = energyStorage.energyStored

                notifyClientAndMarkDirty()
            }

            // 6000 ~= 5 minutes
            // 50 is to wait a little until the world loads on start
            if (resourcesLeftUpdateCount >= 6000) {
                shouldUpdateResourcesLeft = true
                resourcesLeftUpdateCount = 0
            }

            // Update resource count if anything requested it
            if (shouldUpdateResourcesLeft) updateResourcesLeft()

            // Try pushing resources to something that can handle autopushing
            if (autopush) autopusher?.tryPushing()

            // Calculate the amount of ticks subtracted with speed upgrades
            val ticksSubtracted = run {
                val speedUpgradeCount = upgradeCountInInventory<KuarrySpeedUpgrade>()

                // Should max out at 50, therefore speeding it up to a block per second
                10 * speedUpgradeCount
            }

            // Exit if it's not time to mine yet
            if (mineUpdateCount < 70 - ticksSubtracted) return
            mineUpdateCount = 0

            when (activationMode) {
                ActivationMode.AlwaysOn -> {}
                ActivationMode.AlwaysOff -> return
                ActivationMode.DisableWithRS -> if (world.isBlockPowered(pos)) return
                ActivationMode.EnableWithRS -> if (!world.isBlockPowered(pos)) return
            }

            val minedChunks = calculateMinedChunks()

            // Process all blocks in the chunks
            doWithAllBlocksInChunks(minedChunks, ::processBlock)
        }
    }

    /** Counts the all the minable resources in the chunk for user-faced stats. */
    private fun countAllMinable(minedChunks: List<Chunk>): Int {
        var amountOfBlocks = 0

        doWithAllBlocksInChunks(minedChunks) { _, blockState ->
            if (isBlockAllowed(blockState.block)) {
                amountOfBlocks++
            }

            // Continue until there are no more blocks in the chunk
            false
        }

        return amountOfBlocks
    }

    /** Iterate over all the blocks in all the chunks, calling [func] on these blocks.
     *
     * If [func] returns true, the iteration is stopped and this function returns.
     */
    private fun doWithAllBlocksInChunks(chunks: List<Chunk>, func: (blockPos: BlockPos, blockState: IBlockState) -> Boolean) {
        val firstChunkPos = chunks.first().pos
        val lastChunkPos = chunks.last().pos

        val firstX = firstChunkPos.xStart
        val firstZ = firstChunkPos.zStart

        val lastX = lastChunkPos.xEnd
        val lastZ = lastChunkPos.zEnd

        var x = firstX
        var z = firstZ
        var y = pos.y - 1

        do {
            val blockPos = BlockPos(x, y, z)
            val blockState = world.getBlockState(blockPos)

            // Exit whenever the function returns true
            if (func(blockPos, blockState)) break

            when {
                x < lastX ->
                    x++
                z < lastZ -> {
                    x = firstX
                    z++
                }
                else -> {
                    x = firstX
                    z = firstZ
                    y--
                }
            }
        } while (y >= 0)
        // The exit condition is to not go through bedrock
    }

    /** Checks if the block is allowed by black/whitelisting and should be mined. */
    private fun isBlockAllowed(block: Block): Boolean {
        // Blacklist all fluids
        // TODO: allow fluids somehow?
        if (block is IFluidBlock) return false

        val customFilter = upgradeInInventory(KuarryCustomFilter::class)

        // If there's a custom filter, get its mode, otherwise default to blacklist
        val mode = customFilter?.let(KuarryCustomFilter.Companion::mode) ?: KuarryCustomFilter.Mode.Blacklist

        return if (customFilter != null) {
            val cap = customFilter.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)!!

            // A list of blocks gotten from the ItemStacks in the inventory
            val filterBlocks =
                    List(cap.slots) { i ->
                        // Take the itemstack in a slot and if it has a block, return that block. Otherwise return null.
                        cap.getStackInSlot(i)
                                .takeUnless { it.isEmpty }
                                ?.let { Block.getBlockFromItem(it.item) }
                                ?.takeUnless { it == Blocks.AIR }
                    }.filterNotNull()

            val blacklistMode = KuarryCustomFilter.blacklistMode(customFilter)

            when (mode) {
                KuarryCustomFilter.Mode.Blacklist ->
                    // Check if the block is not hard-blacklisted or user-blacklisted
                    when (blacklistMode) {
                        // Add the default blacklist to the provided blacklist
                        KuarryCustomFilter.BlacklistMode.Additional ->
                            block !in (hardBlacklistedBlocks + defaultBlacklistedBlocks + filterBlocks)
                        // Only use the provided blacklist
                        KuarryCustomFilter.BlacklistMode.Only ->
                            block !in (hardBlacklistedBlocks + filterBlocks)
                    }
                KuarryCustomFilter.Mode.Whitelist ->
                    // If the block is hard-blacklisted, don't allow it even with a whitelist.
                    // Otherwise, allow only the blocks in the whitelist.
                    block !in hardBlacklistedBlocks && block in filterBlocks
            }
        } else {
            // Disallow blocks that are hard or user blacklisted
            block !in (hardBlacklistedBlocks + defaultBlacklistedBlocks)
        }
    }

    /** Processes a single block.
     *
     * @return Whether the block was mined or not
     */
    private fun processBlock(blockPos: BlockPos, blockState: IBlockState): Boolean {
        val block = blockState.block

        // Skip the block if it's blacklisted or is a fluid
        if (!isBlockAllowed(block)) return false

        // Harvesting something with pick/shovel should not cost more,
        // otherwise double the energy count
        val toolHarvestModifier = when (block.getHarvestTool(blockState)) {
            "pickaxe", "shovel", null -> 1
            else -> 2
        }
        // Don't want to have 0 or -1 as a modifier, as it would break the math later on, so make it 1
        val levelHarvestModifier = block.getHarvestLevel(blockState).let { if (it <= 0) 1 else it }


        var requiredEnergy = run {
            // Get the starting energy from all the other modifiers
            var energy = baseRequiredEnergy * toolHarvestModifier * levelHarvestModifier

            // Go through the upgrade inventory and add all the energy required by the upgrades
            for (i in 0 until upgradeInventorySize) {
                val stack = upgradeInventory.getStackInSlot(i)
                if (!stack.isEmpty) {
                    val item = stack.item
                    require(item is KuarryUpgrade)

                    energy = item.energyUsageWithUpgrade(energy, stack)
                }
            }

            energy
        }

        // Not enough energy to mine the block, skip it
        if (energyStorage.energyStored < requiredEnergy) return false

        // Take out all the energy required, in multiple iterations if needed (by subtracting the
        // already extracted energy from the rest)
        while (requiredEnergy > 0) {
            requiredEnergy -= energyStorage.extractEnergy(requiredEnergy, false)
        }

        var drops = NonNullList.create<ItemStack>()
        if (upgradeCountInInventory<KuarrySilkTouchUpgrade>() < 1) {
            // Get the fortune level, depending on the upgrades.
            // The levels are mapped to metadata + 1 and invalid number
            // is treated as level 1
            val fortune =
                    upgradeInInventory(KuarryLuckUpgrade::class)?.let {
                        when (it.metadata) {
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            else -> 1
                        }
                    } ?: 0

            // If there's no silk touch upgrade, process the block as if it was mined
            block.getDrops(drops, world, pos, blockState, fortune)
        } else {
            // If there is silk touch, collect it as if with silk touch
            drops.add(SilkTouchHarvest.getSilkTouchDrop(block, blockState))
        }

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
        world.setBlockState(blockPos, KuarryModBlocks.denatured_stone.defaultState)

        approxResourcesLeft.dec().let {
            when {
                it > 0 -> approxResourcesLeft--
                it == 0 -> {
                    // If there are no more resources, mark block as dirty
                    // so the comparator would see the signal
                    approxResourcesLeft--
                    markDirty()
                }
                it < 0 -> {
                    // If there are less then zero resources, the resource count is probably wrong,
                    // so mark the resource count for recalculation on the next update cycle
                    shouldUpdateResourcesLeft = true
                }
            }

            // Notify the client about the new amount of resources left
            notifyClient()
        }

        return true
    }

    override fun getRenderBoundingBox(): AxisAlignedBB {
        val originalBB = super.getRenderBoundingBox()

        if (renderBounds) {
            val (additionalXChunks, additionalZChunks) = xzChunkExpansion()

            // The box should be grown depending on the amount of upgrades
            return originalBB.grow(
                    16.0 * (1 + additionalXChunks),
                    5.0,
                    16.0 * (1 + additionalZChunks)
            )
        }

        return originalBB
    }
}