package org.kotobank.kuarry.container

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Container
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fml.client.config.GuiUtils
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.ModIcons
import org.kotobank.kuarry.ModPackets
import org.kotobank.kuarry.packet.ChangeKuarryActivationMode
import org.kotobank.kuarry.tile_entity.KuarryTileEntity.ActivationMode

class KuarryGUIContainer(private val container: Container) : GuiContainer(container) {
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
        private const val buttonSize = 16
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

        if (container is KuarryContainer) {
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
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        if (container is KuarryContainer) {
            redstoneButtonPos.let {(x, y) ->
                KuarryRedstoneButton(x, y, mouseX - guiLeft, mouseY - guiTop, this, container.tileEntity.activationMode)
            }
        }

        if (inBounds(guiLeft + energyBarPlaceX, guiTop + energyBarTopY, energyBarWidth, energyBarHeight, mouseX, mouseY)) {
            if (container is KuarryContainer) {
                val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
                if (energyCapability != null) {
                    drawTooltip(mouseX - guiLeft, mouseY - guiTop, "${energyCapability.energyStored}/${energyCapability.maxEnergyStored}RF")
                }
            }
        }

        // Need to subtract guiLeft/Top here because mouse position is counted from the
        // beginning of the gui, not from the beginning of the screen
        renderHoveredToolTip(mouseX - guiLeft, mouseY - guiTop)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {

        if (container is KuarryContainer) {
            val te = container.tileEntity

            when {
                inBounds(guiLeft + redstoneButtonPos.first, guiTop + redstoneButtonPos.second, buttonSize, buttonSize, mouseX, mouseY) ->
                    ModPackets.networkChannel.sendToServer(ChangeKuarryActivationMode(te.pos))

                else -> super.mouseClicked(mouseX, mouseY, mouseButton)
            }
        }
    }


    private fun drawTooltip(x: Int, y: Int, vararg lines: String) {
        GlStateManager.disableLighting()
        // Again here, subtracting guiLeft/Top to translate the coordinates into screen coordinates
        GuiUtils.drawHoveringText(lines.toList(), x , y, mc.displayWidth, mc.displayHeight, -1, fontRenderer)
        GlStateManager.enableLighting()
    }


    inner class KuarryRedstoneButton(x: Int, y: Int, mouseX: Int, mouseY: Int, container: GuiContainer, activationMode: ActivationMode) {
        init {
            with(container) {
                val hovered = inBounds(x, y, buttonSize, buttonSize, mouseX, mouseY)

                if (hovered) {
                    drawTexturedModalRect(x, y, ModIcons.buttonHighlight, buttonSize, buttonSize)
                } else {
                    drawTexturedModalRect(x, y, ModIcons.button, buttonSize, buttonSize)
                }

                val (icon, text) =
                        when (activationMode) {
                            ActivationMode.AlwaysOn -> Pair(ModIcons.iconRSIgnore, "Always enabled")
                            ActivationMode.DisableWithRS -> Pair(ModIcons.iconRSWithout, "Disable with redstone signal")
                            ActivationMode.EnableWithRS -> Pair(ModIcons.iconRSWith, "Enable with redstone signal")
                            ActivationMode.AlwaysOff -> Pair(ModIcons.iconDisable, "Always disabled")
                        }
                drawTexturedModalRect(x, y, icon, buttonSize, buttonSize)

                if (hovered) {
                    drawTooltip(mouseX, mouseY, text)
                }
            }
        }
    }
}