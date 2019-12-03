package org.kotobank.kuarry.container.custom_filter

import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModIcons
import org.kotobank.kuarry.KuarryModPackets
import org.kotobank.kuarry.container.BaseGUIContainer
import org.kotobank.kuarry.item.KuarryCustomFilter
import org.kotobank.kuarry.packet.SwitchCustomFilterSetting
import org.kotobank.kuarry.tile_entity.KuarryTileEntity

class CustomFilterGUIContainer(override val container: CustomFilterContainer) : BaseGUIContainer(container) {
    override val actualXSize = 175
    override val actualYSize = 171

    override val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/custom_filter.png")

    override val buttons = listOf(
            ModeButton(7, 69),
            BlacklistModeButton(7 + 16 + 4, 69)
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

    protected inner class BlacklistModeButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltip
            get() =
                when (KuarryCustomFilter.blacklistMode(filter)) {
                    KuarryCustomFilter.BlacklistMode.Only -> Pair(
                            KuarryModIcons.blacklistOnly,
                            "Only blacklist the specified blocks, overriding the default blacklist"
                    )
                    KuarryCustomFilter.BlacklistMode.Additional ->  Pair(
                            KuarryModIcons.blacklistAdditional,
                            "Blacklist the specified blocks, plus the blocks blacklisted by default"
                    )
                }

        override val additionalTooltipLines
            get() =
                when (KuarryCustomFilter.blacklistMode(filter)) {
                    KuarryCustomFilter.BlacklistMode.Only -> emptyArray()
                    KuarryCustomFilter.BlacklistMode.Additional -> {
                        // Join tne names of the blacklisted blocks together with commas, the game
                        // will make newlines by itself.

                        val defaultBlacklistStr =
                                KuarryTileEntity.defaultBlacklistedBlocks.joinToString { it.localizedName }

                        // Show the default blacklist after the help text, with a colored header
                        arrayOf(
                                "",
                                "${TextFormatting.BLUE}${TextFormatting.BOLD}Default blacklist",
                                defaultBlacklistStr
                        )
                    }
                }

        override val enabled
            get() = KuarryCustomFilter.mode(filter) == KuarryCustomFilter.Mode.Blacklist

        // The function below is overridden to only do something when filter mode = blacklist

        override fun onClick() {
            if (enabled) {
                KuarryModPackets.networkChannel.sendToServer(
                        SwitchCustomFilterSetting(SwitchCustomFilterSetting.Setting.BlacklistMode)
                )

                super.onClick()
            }
        }
    }
}