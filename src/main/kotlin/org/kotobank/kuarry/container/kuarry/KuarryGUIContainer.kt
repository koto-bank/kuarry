package org.kotobank.kuarry.container.kuarry

import net.minecraft.client.resources.I18n
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.energy.CapabilityEnergy
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModIcons
import org.kotobank.kuarry.KuarryModPackets
import org.kotobank.kuarry.packet.SwitchKuarrySetting
import org.kotobank.kuarry.tile_entity.KuarryTileEntity.ActivationMode
import org.kotobank.kuarry.container.BaseGUIContainer
import org.kotobank.kuarry.integration.autopushing.Autopusher


class KuarryGUIContainer(override val container: KuarryContainer) : BaseGUIContainer(container) {
    companion object {
        private const val energyBarTopY = 76
        private const val energyBarPlaceX = 194
        private const val energyBarTextureX = 240

        private const val energyBarHeight = 38
        private const val energyBarWidth = 14
    }

    override val actualXSize = 231
    override val actualYSize = 226

    override val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/kuarry.png")

    override val buttons: List<Button> =
            container.tileEntity.run {
                listOf(
                        ActivationModeButton(10, 10),
                        RenderBoundsButton(10, 30),
                        AutopushButton(10, 50)
                )
            }

    init {
        xSize = actualXSize
        ySize = actualYSize
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY)

        val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
        if (energyCapability != null) {
            val maxEnergy = energyCapability.maxEnergyStored
            val currentEnergy = energyCapability.energyStored

            // Scale the current amount of energy to the bar height
            val scaledHeight = ((currentEnergy.toFloat() / maxEnergy.toFloat()) * energyBarHeight).toInt()

            drawTexturedModalRect(
                    guiLeft + energyBarPlaceX, guiTop + energyBarTopY + energyBarHeight - scaledHeight,
                    energyBarTextureX, energyBarTopY + energyBarHeight - scaledHeight,
                    energyBarWidth, scaledHeight
            )
        }
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        val tileEntity = container.tileEntity

        if (tileEntity.approxResourcesLeft != -1) {
            drawString(
                    mc.fontRenderer,
                    "${I18n.format("tile.kuarry.gui.approx_left")}: ${tileEntity.approxResourcesLeft}",
                    30,
                    10,
                    0xFFFFFF
            )
        }

        super.drawGuiContainerForegroundLayer(mouseX, mouseY)

        // Draw the energy bar tooltip when the mouse is over it
        if (inBounds(guiLeft + energyBarPlaceX, guiTop + energyBarTopY, energyBarWidth, energyBarHeight, mouseX, mouseY)) {
            val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
            if (energyCapability != null) {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop, "${energyCapability.energyStored}/${energyCapability.maxEnergyStored}RF")
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val pressedButton = buttons.find { it.isOnButton(mouseX - guiLeft, mouseY - guiTop) }
        if (pressedButton != null) {
            pressedButton.onClick()
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    private inner class ActivationModeButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltipKey
            get() =
                when (container.tileEntity.activationMode) {
                    ActivationMode.AlwaysOn -> Pair(KuarryModIcons.alwaysEnable, "tile.kuarry.gui.always_enabled")
                    ActivationMode.DisableWithRS -> Pair(KuarryModIcons.RSWithout, "tile.kuarry.gui.disable_with_rs")
                    ActivationMode.EnableWithRS -> Pair(KuarryModIcons.RSWith, "tile.kuarry.gui.enable_with_rs")
                    ActivationMode.AlwaysOff -> Pair(KuarryModIcons.alwaysDisable, "tile.kuarry.gui.always_disabled")
                }

        override fun onClick() {
            KuarryModPackets.networkChannel.sendToServer(
                    SwitchKuarrySetting(container.tileEntity.pos, SwitchKuarrySetting.Setting.ActivationMode)
            )

            super.onClick()
        }
    }

    private inner class RenderBoundsButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltipKey
                get() =
                    if (container.tileEntity.renderBounds) {
                        Pair(KuarryModIcons.boundsEnable, "tile.kuarry.gui.render_bounds")
                    } else {
                        Pair(KuarryModIcons.boundsDisable, "tile.kuarry.gui.dont_render_bounds")
                    }

        override fun onClick() {
            KuarryModPackets.networkChannel.sendToServer(
                    SwitchKuarrySetting(container.tileEntity.pos, SwitchKuarrySetting.Setting.RenderBounds)
            )
            super.onClick()
        }
    }

    private inner class AutopushButton(x: Int, y: Int) : Button(x, y) {
        override val enabled: Boolean = Autopusher.isEnabled

        override val iconAndTooltipKey
            get() =
                if (container.tileEntity.autopush) {
                    Pair(KuarryModIcons.autopushEnable, "tile.kuarry.gui.autopush")
                } else {
                    Pair(KuarryModIcons.autopushDisable, "tile.kuarry.gui.dont_autopush")
                }

        override fun onClick() {
            KuarryModPackets.networkChannel.sendToServer(
                    SwitchKuarrySetting(container.tileEntity.pos, SwitchKuarrySetting.Setting.Autopush)
            )
            super.onClick()
        }
    }
}