package org.kotobank.kuarry.integration.autopushing

import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.items.ItemStackHandler
import org.kotobank.kuarry.helper.InventoryHelper as KInventoryHelper

abstract class BaseAutopusher(private val world: World, private val pos: BlockPos, private val inventory: ItemStackHandler,
                              private val width: Int, private val height: Int) {

    /** Try to push [itemStack] to the [toTileEntity] from the [fromSide] side.
     *
     * @return Items that remained after pushing and should be returned to the inventory.
     */
    protected abstract fun tryPushingToSide(toTileEntity: TileEntity?, fromSide: EnumFacing, itemStack: ItemStack): ItemStack

    /** Try to push to the autopushing receiver.
     *
     * @return Whether the push was successful and other receiver should not be tried next.
     */
    open fun tryPushing(): Boolean {
        var pushed = false

        KInventoryHelper.forEachPositionInInventory(width, height) { invPos, _, _ ->
            val stackFromSlot = inventory.getStackInSlot(invPos)

            if (!stackFromSlot.isEmpty) {
                for (side in EnumFacing.values()) {
                    val adjInv = world.getTileEntity(pos.offset(side))
                    val remaining = tryPushingToSide(adjInv, side, stackFromSlot)

                        // If the item count has changed, then the items have been pushed successfully
                        // and the iteration can be stopped
                        if (remaining.count != stackFromSlot.count) {
                            inventory.setStackInSlot(invPos, remaining)
                            pushed = true
                            break
                        }
                }
            }

            pushed
        }

        return pushed
    }
}