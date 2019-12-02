package org.kotobank.kuarry.integration

import li.cil.oc.api.Driver
import li.cil.oc.api.Network
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

object OpenComputersIntegration {
    fun register() {
        Driver.add(OpenComputersDriver)
    }
}

object OpenComputersDriver : DriverSidedTileEntity() {
    override fun getTileEntityClass() = KuarryTileEntity::class.java

    override fun createEnvironment(world: World, blockPos: BlockPos, facing: EnumFacing?) =
            Environment(world.getTileEntity(blockPos) as KuarryTileEntity)

    @Suppress("UNUSED_PARAMETER")
    class Environment(private val tileEntity: KuarryTileEntity) : AbstractManagedEnvironment(), NamedBlock {
        init {
            setNode(
                    Network.newNode(this, Visibility.Network)
                            .withComponent("kuarry", Visibility.Network)
                            .create()
            )
        }

        override fun preferredName() = "kuarry"
        override fun priority() = 1

        @Callback(doc = "function():number -- The approximate number of remaining resources to mine")
        fun getApproxResourcesLeft(context: Context, args: Arguments): Array<Any> =
                arrayOf(tileEntity.approxResourcesLeft)

        @Callback(doc = "function():string -- The activation mode of the kuarry. Can be AlwaysOn, EnableWithRS, DisableWithRS or AlwaysOff")
        fun getActivationMode(context: Context, args: Arguments): Array<Any> =
                arrayOf(tileEntity.activationMode)

        @Callback(doc = "function(string) -- Sets the activation mode of the kuarry. The values are the same as in getActivationMode")
        fun setActivationMode(context: Context, args: Arguments): Array<Any>? {
            tileEntity.activationMode = KuarryTileEntity.ActivationMode.valueOf(args.checkString(0))

            return null
        }

        @Callback(doc = "function():table -- Returns the upgrades installed in the kuarry. Because the inventory isn't directly exposed, this has to be used.")
        fun getInstalledUpgrades(context: Context, args: Arguments): Array<Any> {
            val result = mutableListOf<ItemStack>()

            val inventory = tileEntity.upgradeInventory
            for (i in 0 until inventory.slots) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty) result.add(stack)
            }

            return arrayOf(result)
        }

        // TODO: Figure out a way to allow putting/getting upgrades to/from the upgrade inventory,
        // TODO: maybe unify them into a single inventory
    }
}