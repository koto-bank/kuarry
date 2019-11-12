package org.kotobank.kuarry.container.custom_filter

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModIcons
import org.kotobank.kuarry.KuarryModPackets
import org.kotobank.kuarry.container.BaseGUIContainer
import org.kotobank.kuarry.item.KuarryCustomFilter
import org.kotobank.kuarry.packet.SwitchCustomFilterSetting

class CustomFilterGUIContainer(override val container: CustomFilterContainer) : BaseGUIContainer(container) {
    override val actualXSize = 175
    override val actualYSize = 171

    override val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/custom_filter.png")

    override val buttons = listOf(
            ModeButton(8, 69)
    )

    private val filter
        get() = run {
            val filter = container.player.heldItemMainhand
            if (filter.item is KuarryCustomFilter) {
                filter
            } else {
                KuarryMod.logger.warn("Item wasn't a custom filter when handling custom filter GUI")

                filter
            }

        }

    protected inner class ModeButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltip
            get() =
                when (KuarryCustomFilter.mode(filter)) {
                    KuarryCustomFilter.Mode.Blacklist -> Pair(KuarryModIcons.blacklist, "Blacklist")
                    KuarryCustomFilter.Mode.Whitelist -> Pair(KuarryModIcons.whitelist, "Whitelist")
                }

        override val additionalTooltipLines
            get() = arrayOf(
                    when (KuarryCustomFilter.mode(filter)) {
                        KuarryCustomFilter.Mode.Blacklist -> "Skip these items when mining"
                        KuarryCustomFilter.Mode.Whitelist -> "Only mine these items"
                    }
            )

        override fun onClick() {
            KuarryModPackets.networkChannel.sendToServer(
                    SwitchCustomFilterSetting(SwitchCustomFilterSetting.Setting.Mode)
            )

            super.onClick()
        }
    }
}