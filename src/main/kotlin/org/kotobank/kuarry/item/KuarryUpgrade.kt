package org.kotobank.kuarry.item

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
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

    @get:SideOnly(Side.CLIENT)
    protected open val energyUsageTooltip: String
        get() = I18n.format(
                "tooltips.about_energy_multiplication",
                "${TextFormatting.BOLD}$energyUsageMultiplier${TextFormatting.RESET}${TextFormatting.GRAY}"
        )

    @get:SideOnly(Side.CLIENT)
    protected open val tooltipKey by lazy {
        "${unlocalizedName}.description"
    }

    @get:SideOnly(Side.CLIENT)
    protected open val incompatWithKey by lazy {
        "${unlocalizedName}.description.incompat"
    }

    @get:SideOnly(Side.CLIENT)
    protected val tooltipStrings: List<String>
    get() {
        val tooltipList = mutableListOf(I18n.format(tooltipKey))

        if (incompatibleWith != null)
            tooltipList.add(I18n.format(incompatWithKey))

        if (energyUsageMultiplier > 1)
            tooltipList.addAll(listOf("", energyUsageTooltip))

        return tooltipList
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

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        tooltip.addAll(tooltipStrings)
    }
}

class KuarryXBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val energyUsageMultiplier = 1.1f
}

class KuarryZBoundariesUpgrade : KuarryUpgrade() {
    override val stackSize = 2
    override val energyUsageMultiplier = 1.1f
}

class KuarrySilkTouchUpgrade : KuarryUpgrade() {
    override val energyUsageMultiplier = 1.1f

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

    @get:SideOnly(Side.CLIENT)
    override val energyUsageTooltip: String
        get() = I18n.format(
                "item.luck_upgrade.description.about_energy_multiplication",
                "${TextFormatting.BOLD}$energyUsageMultiplier${TextFormatting.RESET}${TextFormatting.GRAY}"
        )

    override fun getHasSubtypes() = true

    override fun getSubItems(tab: CreativeTabs, items: NonNullList<ItemStack>) {
        if (tab == CreativeTabs.SEARCH) {
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
}
