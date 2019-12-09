package org.kotobank.kuarry.item

import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
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

    /** The tooltip about what upgrade does. */
    protected val levelUpgradeInfoKey: String = "tooltips.about_level_upgrade"

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
                } else {
                    player.sendMessage(
                            TextComponentTranslation(
                                    "tooltips.level_uprgade_wrong_level",
                                    level, // This number is basically the number the user wants to see, since those are + 1 of actual numbers
                                    te.upgradeLevel + 1 // Add 1 to this to stay consistent with the previous one
                            )
                    )
                }
            }
        }

        return EnumActionResult.PASS
    }

    @get:SideOnly(Side.CLIENT)
    protected val tooltipStrings: List<String>
        get() {
            val levelValues = LevelValues[level]

            return listOf(I18n.format(levelUpgradeInfoKey, levelValues.energy, levelValues.upgradeSlotLines * 2))
        }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.addAll(tooltipStrings)
    }
}

class HardenedUpgrade : LevelUpgrade() {
    override val level = 1
}

class ReinforcedUpgrade : LevelUpgrade() {
    override val level = 2
}