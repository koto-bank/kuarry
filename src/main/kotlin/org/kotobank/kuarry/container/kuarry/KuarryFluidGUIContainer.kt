package org.kotobank.kuarry.container.kuarry

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fluids.Fluid
import org.kotobank.kuarry.KuarryMod
import org.kotobank.kuarry.KuarryModGUIHandler
import org.kotobank.kuarry.KuarryModIcons
import org.kotobank.kuarry.container.BaseGUIContainer
import org.kotobank.kuarry.tile_entity.kuarry_component.FluidInventoryComponent
import org.lwjgl.opengl.GL11
import java.text.DecimalFormat

class KuarryFluidGUIContainer(override val container: KuarryFluidContainer, val player: EntityPlayer) : BaseGUIContainer(container){
    companion object {
        private const val slotSize = 18

        private const val inventoryStartX = 8
        private const val inventoryStartY = 8

        private val decimalFormatter = DecimalFormat("#.#")
    }

    override val actualXSize = 175
    override val actualYSize = 131
    override val backgroundTexture = ResourceLocation(KuarryMod.MODID, "textures/gui/kuarry_fluid.png")

    override val buttons: List<Button> =
        listOf(
                BackButton(7, 29)
        )

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY)

        val fluidInventory = container.tileEntity.fluidInventoryComponent
        for (i in 0 until FluidInventoryComponent.size) {
            val fluidStack = fluidInventory.getFluidStackAt(i)
            if (fluidStack != null) {
                mc.renderEngine.bindTexture(KuarryModIcons.atlasResourceLocation)
                val sprite = mc.textureMapBlocks.getAtlasSprite(fluidStack.fluid.still.toString())

                drawTexturedModalRect(
                        guiLeft + inventoryStartX + (i * slotSize),
                        guiTop + inventoryStartY,
                        sprite,
                        sprite.iconWidth,
                        sprite.iconHeight
                )

                // Count the amount of fluid in buckets
                val bucketAmount = fluidStack.amount.toFloat() / Fluid.BUCKET_VOLUME
                val bucketStr = decimalFormatter.format(bucketAmount)

                // Down here the code handles scaling the text to make it smaller and position it properly,
                // depending on if the font is unicode or not. This has some specific numbers to make it
                // look as close as possible to the vanilla slot.


                val yAddition = mc.fontRenderer.FONT_HEIGHT.let { if (mc.isUnicode) it else it + 2 }

                val x = guiLeft + inventoryStartX + (i * slotSize) + 1
                val y = guiTop + inventoryStartY + yAddition


                GL11.glPushMatrix()
                // Translate the coordinates to the calculated positions, so that scaling doesn't affect them
                GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)

                val scale = if (mc.isUnicode) 1f else 0.7f
                // Scale the text to be a little smaller
                GL11.glScalef(scale, scale, 1f)

                drawString(
                        mc.fontRenderer,
                        bucketStr,
                        0,
                        0,
                        0xFFFFFF
                )

                GL11.glPopMatrix()
            }
        }
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY)

        val fluidInventory = container.tileEntity.fluidInventoryComponent
        for (i in 0 until FluidInventoryComponent.size) {
            val fluidStack = fluidInventory.getFluidStackAt(i) ?: continue

            val xStart = guiLeft + inventoryStartX + (i * slotSize)
            val yStart = guiTop + inventoryStartY
            if (inBounds(xStart, yStart, slotSize, slotSize, mouseX, mouseY)) {
                drawTooltip(
                        mouseX - guiLeft, mouseY - guiTop,
                        fluidStack.localizedName, "${TextFormatting.DARK_GRAY}${fluidStack.amount}"
                )
            }
        }
    }

    private inner class BackButton(x: Int, y: Int) : Button(x, y) {
        override val iconAndTooltipKey=
                Pair(KuarryModIcons.back, "tile.kuarry.gui.back")

        override fun onClick() {
            val pos = container.tileEntity.pos

            player.openGui(KuarryMod, KuarryModGUIHandler.KUARRY, player.world, pos.x, pos.y, pos.z)

            super.onClick()
        }
    }
}