package org.kotobank.kuarry.container

import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.ResourceLocation
import org.kotobank.kuarry.KuarryMod

class KuarryGUIContainer(container: Container, private val inventoryPlayer: InventoryPlayer?) : GuiContainer(container) {
    companion object {
        private val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/kuarry.png")

        private const val actualYSize = 225
    }

    init {
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
    }
}