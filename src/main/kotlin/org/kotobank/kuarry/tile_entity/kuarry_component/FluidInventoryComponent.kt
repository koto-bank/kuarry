package org.kotobank.kuarry.tile_entity.kuarry_component

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class FluidInventoryComponent(private val parent: KuarryTileEntity): IFluidHandler {
    companion object {
        const val size = 9

        /** Maximum amount of fluid in one [FluidStack] in millibuckets. */
        const val maxAmount = 64000

        /** Checks if the [stack] has enough fluid to subtract the [amount] fully. */
        private fun hasEnoughFluid(stack: FluidStack, amount: Int) = stack.amount >= amount

        /** Checks if the [stack] has enough place (controlled by [maxAmount]) to add [amount] to it. */
        private fun canHoldMoreFluid(stack: FluidStack, amount: Int) = stack.amount + amount <= maxAmount

        /** Subtracts the specified [amount] of fluid from the [stack].
         *
         * @return the updated stack, from which the amount has been subtracted,
         *         and a new stack with the amount of fluid subtracted
         */
        fun subtractFluidStack(stack: FluidStack, amount: Int): Pair<FluidStack, FluidStack> {
            val updatedStack = stack.copy()
            val resultingStack = stack.copy()

            if (hasEnoughFluid(stack, amount)) {
                // If there is enough fluid, subtract the amount from the updated stack and return a full
                // resulting stack
                updatedStack.amount -= amount
                resultingStack.amount = amount
            } else {
                // If there's not enough fluid, empty the resulting stack and make the resulting stack have
                // the amount that was taken out
                updatedStack.amount = 0
                resultingStack.amount = stack.amount
            }

            return Pair(updatedStack, resultingStack)
        }

        /** Add the specified [amount] of fluid to the [stack], maxing out at [maxAmount].
         *
         * @return the updated stack filled with the fluid and the amount of fluid that's left
         */
        fun addFluidToStack(stack: FluidStack, amount: Int): Pair<FluidStack, FluidStack> {
            val remainder = stack.copy()
            val resulting = stack.copy()

            if (canHoldMoreFluid(resulting, amount)) {
                // If the stack can still hold all the fluid, add it and return an empty remainder
                remainder.amount = 0
                resulting.amount += amount
            } else {
                // If the total amount is more that can be held, set the amount to the max amount,
                // and return the subtracted remainder
                remainder.amount -= amount
                resulting.amount = maxAmount
            }

            return Pair(resulting, remainder)
        }
    }

    /** The "inventory" that holds the [FluidStack]s.
     *
     * NOTE: In 1.14 this should be FluidStack.EMPTY
     */
    private var fluidStacks = arrayOfNulls<FluidStack>(size)

    fun getFluidStackAt(i: Int) = fluidStacks[i]?.copy()

    fun setFluidStackAt(i: Int, stack: FluidStack?) {
        if (stack != null && stack.amount > 0)
            fluidStacks[i] = stack.copy()
        else
            fluidStacks[i] = null

        fluidTankProperties[i] = object : IFluidTankProperties {
            override fun canDrain() = true
            override fun canFill() = false
            override fun canDrainFluidType(fluidStack: FluidStack?) = true
            override fun canFillFluidType(fluidStack: FluidStack?) = false
            override fun getCapacity() = maxAmount
            override fun getContents(): FluidStack? = getFluidStackAt(i)
        }

        // The fluids have changed, so update the parent
        parent.notifyClientAndMarkDirty()
    }

    /** An array of fluid tank properties that should be updated when the fluids are updated. */
    private var fluidTankProperties = arrayOfNulls<IFluidTankProperties>(size)

    /** Puts a copy of the specified [FluidStack] into the inventory.
     *
     * @return the fluid that was not put in due to the lack of space.
     */
    internal fun putFluid(stackToPut: FluidStack): FluidStack {
        val foundFluidIndex = fluidStacks.indexOfFirst { it == stackToPut }

        if (foundFluidIndex == -1) {
            // If there is no such fluid in the container yet, try adding it to an empty slot

            val firstEmptySlotIndex = fluidStacks.indexOf(null)
            // If there were no empty slots, return the FluidStack as is, with everything remaining
            if (firstEmptySlotIndex == -1) {
                return stackToPut
            } else {
                // If there's an empty slot, fill it

                val (copyToPut, copyToReturn) = addFluidToStack(
                        // Pass in an empty copy as a container
                        stackToPut.copy().apply { amount = 0 },
                        stackToPut.amount
                )

                // Put the full stack into storage
                setFluidStackAt(firstEmptySlotIndex, copyToPut)

                return copyToReturn
            }
        } else {
            val foundFluidStack = getFluidStackAt(foundFluidIndex)!!
            val (copyToPut, copyToReturn) = addFluidToStack(foundFluidStack, stackToPut.amount)

            // Put the full stack into storage
            setFluidStackAt(foundFluidIndex, copyToPut)

            return copyToReturn
        }
    }

    fun extractAt(max: Int, at: Int, doDrain: Boolean = true): FluidStack? {
        val slot = getFluidStackAt(at) ?: return null

        val (copyToPut, copyToReturn) = subtractFluidStack(slot, max)

        if (doDrain)
            setFluidStackAt(at, copyToPut)

        return copyToReturn
    }

    /** Extracts and returns a copy of the first available [FluidStack] or null if none exist.
     *
     * @param max maximum amount to extract
     * @param doDrain if false, drain will be simulated
     */
    fun extractFirstAvailable(max: Int, doDrain: Boolean = true): FluidStack? {
        val indexOfFirst = fluidStacks.indexOfFirst { it != null }
        if (indexOfFirst == -1) return null

        return extractAt(max, indexOfFirst, doDrain)
    }

    override fun getTankProperties() = fluidTankProperties
    override fun fill(resource: FluidStack?, doFill: Boolean): Int = 0
    override fun drain(maxDrain: Int, doDrain: Boolean): FluidStack? =
        extractFirstAvailable(maxDrain, doDrain)

    override fun drain(resource: FluidStack, doDrain: Boolean): FluidStack? {
        val indexOfFirst = fluidStacks.indexOfFirst { it == resource }
        if (indexOfFirst == -1) return null

        return extractAt(
                resource.amount,
                indexOfFirst,
                doDrain
        )
    }

    fun hasCapability(capability: Capability<*>): Boolean =
            capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getCapability(capability: Capability<T>): T? =
            when (capability) {
                CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY -> this as T
                else -> null
            }

    fun writeToNBT(compound: NBTTagCompound) {
        compound.apply {
            val list = NBTTagList()
            for (i in 0 until size) {
                val fluidStack = getFluidStackAt(i)

                if (fluidStack != null) {
                    list.appendTag(NBTTagCompound().apply {
                        setInteger("index", i)
                        setTag("fluid_stack", fluidStack.writeToNBT(NBTTagCompound()))
                    })
                }
            }

            setTag("fluid_inventory", list)
        }
    }

    fun readFromNBT(compound: NBTTagCompound) {
        compound.apply {
            val list = getTagList("fluid_inventory", NBTTagCompound().id.toInt())

            fluidStacks = arrayOfNulls(size)
            fluidTankProperties = arrayOfNulls(size)

            for (i in 0 until list.tagCount()) {
                val listElem = list.getCompoundTagAt(i)

                val inventoryIndex = listElem.getInteger("index")
                val fluidStack = FluidStack.loadFluidStackFromNBT(listElem.getCompoundTag("fluid_stack"))

                setFluidStackAt(inventoryIndex, fluidStack)
            }
        }
    }
}