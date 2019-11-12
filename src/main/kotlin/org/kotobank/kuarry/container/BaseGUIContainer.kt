package org.kotobank.kuarry.container

import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.init.SoundEvents
import net.minecraft.inventory.Container
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.config.GuiUtils
import org.kotobank.kuarry.KuarryModIcons

abstract class BaseGUIContainer(protected open val container: Container) : GuiContainer(container) {
    companion object {
        fun inBounds(x: Int, y: Int, w: Int, h: Int, ox: Int, oy: Int) =
                ox >= x && ox <= x + w && oy >= y && oy <= y + h

        val buttonSize = 16
    }

    /** Actual X size of the container, [xSize] is set from this value. */
    abstract val actualXSize: Int
    /** Actual Y size of the container. [ySize] is set from this value. */
    abstract val actualYSize: Int

    abstract val backgroundTexture: ResourceLocation

    /** A list of buttons in the container.
     *
     * The buttons will be drawn by [drawGuiContainerForegroundLayer], if the subclass
     * calls this implementation. The buttons will be interactable if the subclass calls
     * the [mouseClicked] implementation of this class (or doesn't override it, since it _should_
     * be enough already).
     */
    protected open val buttons: List<Button> = listOf()

    override fun initGui() {
        xSize = actualXSize
        ySize = actualYSize

        super.initGui()
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GlStateManager.color(1f, 1f, 1f, 1f)

        // Draw the default dark background. This HAS to happen before bindTexture,
        // otherwise it's all garbled
        drawDefaultBackground();

        mc.textureManager.bindTexture(backgroundTexture)

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        buttons.forEach { it.draw(mouseX - guiLeft, mouseY - guiTop) }
        buttons.forEach { it.drawTooltip(mouseX - guiLeft, mouseY - guiTop) }

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

    protected fun drawTooltip(x: Int, y: Int, vararg lines: String) {
        GlStateManager.disableLighting()
        // Again here, subtracting guiLeft/Top to translate the coordinates into screen coordinates
        GuiUtils.drawHoveringText(lines.toList(), x , y, mc.displayWidth, mc.displayHeight, -1, fontRenderer)
        GlStateManager.enableLighting()
    }

    /** A base class for GUI buttons.
     *
     * These buttons should be added to the [buttons] list, which has no relation to
     * standard minecraft buttons. They will then be processed as explained in the [buttons] documentation string.
     */
    protected abstract inner class Button(private val x: Int, private val y: Int) {
        /** Whether the specified [mouseX] and [mouseY] are floating over the button. */
        fun isOnButton(mouseX: Int, mouseY: Int) = inBounds(x, y, buttonSize, buttonSize, mouseX, mouseY)

        /** Returns, depending on some custom circumstances, a pair consisting of an icon and a tooltip to use for the button */
        abstract val iconAndTooltip: Pair<TextureAtlasSprite, String>

        /** Additional lines to add to the tooltip besides the button name. */
        open val additionalTooltipLines = arrayOf<String>()

        /** An action to perform when the button is clicked.
         *
         * Plays a click sound by default. Implementations should call super to play a click sound.
         */
        open fun onClick() {
            mc.soundHandler.playSound(PositionedSoundRecord.getRecord(SoundEvents.UI_BUTTON_CLICK, 1f, 0.3f))
        }

        /** Draws the button and the icon from [iconAndTooltip]. */
        fun draw(mouseX: Int, mouseY: Int) {
            // This is REQUIRED for drawing textures from the atlas. It WILL obscurely fail to draw
            // in specific situations otherwise.
            mc.renderEngine.bindTexture(KuarryModIcons.atlasResourceLocation)

            val hovered = isOnButton(mouseX, mouseY)

            val buttonTex = if (hovered) KuarryModIcons.buttonHighlight else KuarryModIcons.button
            drawTexturedModalRect(x, y, buttonTex, buttonSize, buttonSize)

            val (icon, _) = iconAndTooltip

            drawTexturedModalRect(x, y, icon, buttonSize, buttonSize)
        }

        /** Draws the tooltip from [iconAndTooltip] if the mouse is floating over the button. */
        fun drawTooltip(mouseX: Int, mouseY: Int) {
            if (isOnButton(mouseX, mouseY)) {
                val (_, tooltip) = iconAndTooltip

                drawTooltip(mouseX, mouseY, tooltip, *additionalTooltipLines)
            }
        }
    }
}