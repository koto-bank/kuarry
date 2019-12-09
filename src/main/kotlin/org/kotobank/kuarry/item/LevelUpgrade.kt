package org.kotobank.kuarry.item

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.item.Item
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

/** Data storage for various level-dependant things of the [KuarryTileEntity]. */
data class LevelProperties(val energy: Int, val upgradeSlotLines: Int)

/** Data for all levels. */
val LevelValues = arrayOf(
        // Base
        LevelProperties(100_000, 3),
        // Hardened
        LevelProperties(200_000, 4),
        // Reinforced
        LevelProperties(300_000, 5)
)

sealed class LevelUpgrade : Item() {
    abstract val level: Int

    /** Whether the upgrade should apply.
     *
     * Currently the only condition is that the machine is of the level directly below the upgrade level.
     */
    fun shouldUpgrade(te: KuarryTileEntity) = level - 1 == te.upgradeLevel

    override fun onItemUse(player: EntityPlayer, worldIn: World, pos: BlockPos, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): EnumActionResult {
        val itemstack = player.getHeldItem(hand)

        if (itemstack.item == this) {
            val te = worldIn.getTileEntity(pos)
            if (te is KuarryTileEntity) {
                if (shouldUpgrade(te)) {
                    // Set the level and shrink the amount of upgrades
                    te.upgradeLevel = level
                    itemstack.shrink(1)

                    // Play the upgrade sound, pass null as a player so it plays for everyone
                    worldIn.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1f, 1f)

                    return EnumActionResult.SUCCESS
                }
            }
        }

        return EnumActionResult.PASS
    }
}

class HardenedUpgrade : LevelUpgrade() {
    override val level = 1
}

class ReinforcedUpgrade : LevelUpgrade() {
    override val level = 2
}