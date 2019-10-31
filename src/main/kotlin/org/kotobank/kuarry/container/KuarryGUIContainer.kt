package org.kotobank.kuarry.container

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fml.client.config.GuiUtils
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModIcons
import org.kotobank.kuarry.KuarryModPackets
import org.kotobank.kuarry.packet.SwitchKuarrySetting
import org.kotobank.kuarry.tile_entity.KuarryTileEntity.ActivationMode
import net.minecraft.init.SoundEvents
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.renderer.texture.TextureAtlasSprite


class KuarryGUIContainer(private val container: KuarryContainer) : GuiContainer(container) {
    companion object {
        private val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/kuarry.png")

        private const val actualXSize = 231
        private const val actualYSize = 226

        private const val energyBarTopY = 76
        private const val energyBarPlaceX = 194
        private const val energyBarTextureX = 240

        private const val energyBarHeight = 38
        private const val energyBarWidth = 14

        fun inBounds(x: Int, y: Int, w: Int, h: Int, ox: Int, oy: Int) =
                ox >= x && ox <= x + w && oy >= y && oy <= y + h

        private val redstoneButtonPos = Pair(10, 10)
        private val boundsButtonPos = Pair(10, 30)
        private const val buttonSize = 16
    }

    private val buttons: List<Button> = run {
        container.tileEntity.run {
            listOf(
                    ActivationModeButton(10, 10),
                    RenderBoundsButton(10, 30)
            )
        }
    }

    init {
        xSize = actualXSize
        ySize = actualYSize
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.color(1f, 1f, 1f, 1f)

        // Draw the default dark background. This HAS to happen before bindTexture,
        // otherwise it's all garbled
        drawDefaultBackground();

        mc.textureManager.bindTexture(backgroundTexture)

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)

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

        if (tileEntity.approxResourceCount != -1) {
            drawString(
                    mc.fontRenderer,
                    "Approx. mined: ${tileEntity.approxResourcesMined}/${tileEntity.approxResourceCount}",
                    30,
                    10,
                    0xFFFFFF
            )
        }

        // Draw the energy bar tooltip when the mouse is over it
        if (inBounds(guiLeft + energyBarPlaceX, guiTop + energyBarTopY, energyBarWidth, energyBarHeight, mouseX, mouseY)) {
            val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
            if (energyCapability != null) {
                drawTooltip(mouseX - guiLeft, mouseY - guiTop, "${energyCapability.energyStored}/${energyCapability.maxEnergyStored}RF")
            }
        }

        buttons.forEach { it.draw(mouseX - guiLeft, mouseY - guiTop) }

        // Need to subtract guiLeft/Top here because mouse position is counted from the
        // beginning of the gui, not from the beginning of the screen
        renderHoveredToolTip(mouseX - guiLeft, mouseY - guiTop)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        val pressedButton = buttons.find { it.isOnButton(mouseX - guiLeft, mouseY - guiTop) }
        if (pressedButton != null) {
            pressedButton.onClick()
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }


    private fun drawTooltip(x: Int, y: Int, vararg lines: String) {
        GlStateManager.disableLighting()
        // Again here, subtracting guiLeft/Top to translate the coordinates into screen coordinates
        GuiUtils.drawHoveringText(lines.toList(), x , y, mc.displayWidth, mc.displayHeight, -1, fontRenderer)
        GlStateManager.enableLighting()
    }

    private abstract inner class Button(private val x: Int, private val y: Int) {
        fun isOnButton(mouseX: Int, mouseY: Int) = inBounds(x, y, buttonSize, buttonSize, mouseX, mouseY)

        abstract val iconAndTooltip: Pair<TextureAtlasSprite, String>

        open fun onClick() {
            mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.UI_BUTTON_CLICK, 1f, 0.3f))
        }

        fun draw(mouseX: Int, mouseY: Int) {
            // This is REQUIRED for drawing textures from the atlas. It WILL obscurely fail to draw
            // in specific situations otherwise.
            mc.renderEngine.bindTexture(KuarryModIcons.atlasResourceLocation)

            val hovered = isOnButton(mouseX, mouseY)

            val buttonTex = if (hovered) KuarryModIcons.buttonHighlight else KuarryModIcons.button
            drawTexturedModalRect(x, y, buttonTex, buttonSize, buttonSize)

            val (icon, tooltipText) = iconAndTooltip

            drawTexturedModalRect(x, y, icon, buttonSize, buttonSize)

            if (hovered) {
                drawTooltip(mouseX, mouseY, tooltipText)
            }
        }
    }

    private inner class ActivationModeButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltip
            get() =
                when (container.tileEntity.activationMode) {
                    ActivationMode.AlwaysOn -> Pair(KuarryModIcons.iconRSIgnore, "Always enabled")
                    ActivationMode.DisableWithRS -> Pair(KuarryModIcons.iconRSWithout, "Disable with redstone signal")
                    ActivationMode.EnableWithRS -> Pair(KuarryModIcons.iconRSWith, "Enable with redstone signal")
                    ActivationMode.AlwaysOff -> Pair(KuarryModIcons.iconDisable, "Always disabled")
                }

        override fun onClick(): Unit =
            KuarryModPackets.networkChannel.sendToServer(
                    SwitchKuarrySetting(container.tileEntity.pos, SwitchKuarrySetting.Setting.ActivationMode)
            ).also { super.onClick() }
    }

    private inner class RenderBoundsButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltip: Pair<TextureAtlasSprite, String>
            get() = if (container.tileEntity.renderBounds) {
                Pair(KuarryModIcons.boundsEnable, "Show mining area boundaries")
            } else {
                Pair(KuarryModIcons.boundsDisable, "Do not show mining area boundaries")
            }

        override fun onClick() {
                KuarryModPackets.networkChannel.sendToServer(
                        SwitchKuarrySetting(container.tileEntity.pos, SwitchKuarrySetting.Setting.RenderBounds)
                ).also { super.onClick() }
        }
    }
}