package org.kotobank.kuarry.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.KClass

abstract class KuarryUpgrade : Item() {
    /** Since items don't have an initialization function and using an open member
     *  in the constructor doesn't work properly, this has to be set by the caller with [setMaxStackSize]
     */
    open val stackSize = 1

    open val incompatibleWith: KClass<out KuarryUpgrade>? = null

    /** Energy usage multiplier used with [energyUsageWithUpgrade]. */
    open val energyUsageMultiplier = 1f

    open val tooltipStrings by lazy {
        if (energyUsageMultiplier > 1)
            listOf("", "Multiplies energy usage by ${TextFormatting.BOLD}$energyUsageMultiplier")
        else
            emptyList()
    }

    /** Calculates energy usage for the upgrades, probably based on the [energyUsageMultiplier].
     *
     * The default implementation raises the power usage exponentionally with the amount of upgrades,
     * based on the [energyUsageMultiplier].
     *
     * @param currentEnergyUsage current energy usage to add to
     * @param upgrades the [ItemStack] of the upgrade
     *
     * @return new energy usage
     */
    open fun energyUsageWithUpgrade(currentEnergyUsage: Int, upgrades: ItemStack): Int =
            // For some reason there's no integer power function, so have to convert to float and back
            (currentEnergyUsage * (energyUsageMultiplier.pow(upgrades.count))).roundToInt()

    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.addAll(tooltipStrings)
    }
}

class KuarryXBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val energyUsageMultiplier = 1.1f
    override val tooltipStrings =
            listOf("Upgrades the kuarry to mine one more chunk along the X dimension") + super.tooltipStrings
}

class KuarryZBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val energyUsageMultiplier = 1.1f
    override val tooltipStrings =
            listOf("Upgrades the kuarry to mine one more chunk along the Z dimension") + super.tooltipStrings
}

class KuarrySilkTouchUpgrade : KuarryUpgrade() {
    override val energyUsageMultiplier = 1.1f
    override val tooltipStrings = listOf(
            "Makes the kuarry retrieve the blocks as they are, instead of mining them",
            "Not compatible with the luck upgrade"
    ) + super.tooltipStrings

    override val incompatibleWith = KuarryLuckUpgrade::class
}

class KuarryLuckUpgrade : KuarryUpgrade() {
    override val energyUsageMultiplier = 1.2f
    override fun energyUsageWithUpgrade(currentEnergyUsage: Int, upgrades: ItemStack): Int {
        val level = run {
            val v = upgrades.metadata + 1
            if (v > 3) 3 else v
        }

        // Scale the energy usage from the luck upgrade level instead of upgrade count
        return (currentEnergyUsage * (energyUsageMultiplier.pow(level))).roundToInt()
    }

    override val tooltipStrings = listOf(
            "Makes the kuarry mine more resources from some ores",
            "Not compatible with the silk touch upgrade"
    ) + super.tooltipStrings

    override fun getHasSubtypes() = true

    override fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (tab == creativeTab) {
            for (level in 0..2) {
                items.add(
                        ItemStack(this, 1, level)
                )
            }
        }
    }

    override fun getUnlocalizedName(stack: ItemStack): String = "${super.getUnlocalizedName()}_${getMetadata(stack)}"

    override val incompatibleWith = KuarrySilkTouchUpgrade::class

    fun registerSubitemModels() {
        for (level in 0..2) {
            ModelLoader.setCustomModelResourceLocation(
                    this,
                    level,
                    ModelResourceLocation("${registryName}_$level", "inventory")
            )
        }
    }
}

class KuarrySpeedUpgrade : KuarryUpgrade() {
    override val stackSize = 5
    override val energyUsageMultiplier = 1.2f
    override val tooltipStrings =
            listOf(
                    "${TextFormatting.RED}${TextFormatting.BOLD}The red one goes faster"
            ) + super.tooltipStrings
}
