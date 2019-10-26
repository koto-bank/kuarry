package org.kotobank.kuarry.container

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.inventory.Container
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.fml.client.config.GuiUtils
import org.kotobank.kuarry.KuarryMod

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
        val x = (width - xSize) / 2
        val y = (height - ySize) / 2

        drawTexturedModalRect(x, y, 0, 0, xSize, ySize)

        if (container is KuarryContainer) {
            val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
            if (energyCapability != null) {
                val maxEnergy = energyCapability.maxEnergyStored
                val currentEnergy = energyCapability.energyStored

                // Scale the current amount of energy to the bar height
                val scaledHeight = ((currentEnergy.toFloat() / maxEnergy.toFloat()) * energyBarHeight).toInt()

                drawTexturedModalRect(
                        x + energyBarPlaceX, y + energyBarTopY + energyBarHeight - scaledHeight,
                        energyBarTextureX, energyBarTopY + energyBarHeight - scaledHeight,
                        energyBarWidth, scaledHeight
                        )
            }
        }
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        val x = (width - xSize) / 2
        val y = (height - ySize) / 2

        if (inBounds(x + energyBarPlaceX, y + energyBarTopY, energyBarWidth, energyBarHeight, mouseX, mouseY)) {
            if (container is KuarryContainer) {
                val energyCapability = container.tileEntity.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH)
                if (energyCapability != null) {
                    drawTooltip(mouseX, mouseY, "${energyCapability.energyStored}/${energyCapability.maxEnergyStored}RF")
                }
            }
        }

        // Need to subtract guiLeft/Top here because mouse position is counted from the
        // beginning of the gui, not from the beginning of the screen
        renderHoveredToolTip(mouseX - guiLeft, mouseY - guiTop)
    }


    private fun drawTooltip(x: Int, y: Int, vararg lines: String) {
        GlStateManager.disableLighting()
        // Again here, subtracting guiLeft/Top to translate the coordinates into screen coordinates
        GuiUtils.drawHoveringText(lines.toList(), x - guiLeft , y - guiTop, mc.displayWidth, mc.displayHeight, -1, fontRenderer)
        GlStateManager.enableLighting()
    }
}