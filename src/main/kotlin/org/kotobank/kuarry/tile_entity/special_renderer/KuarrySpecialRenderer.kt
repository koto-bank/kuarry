package org.kotobank.kuarry.tile_entity.special_renderer

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import org.kotobank.kuarry.tile_entity.KuarryTileEntity
import org.lwjgl.opengl.GL11.*
import net.minecraft.client.renderer.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats

class KuarrySpecialRenderer : TileEntitySpecialRenderer<KuarryTileEntity>() {

    override fun render(te: KuarryTileEntity, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, alpha: Float) {
        if (!te.renderBounds) return

        val chunk = world.getChunkFromBlockCoords(te.pos)
        val chunkPos = chunk.pos

        // Find the difference between the block and the beginning of the first chunk
        val xDiff = (chunkPos.xStart - te.pos.x).toDouble()
        val zDiff = (chunkPos.zStart - te.pos.z).toDouble()

        GlStateManager.disableTexture2D()

        // Disable the standard lighting and make the lines max lighted
        GlStateManager.disableLighting()
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f)

        val tesselator = Tessellator.getInstance()
        val bb = tesselator.buffer

        // Use the lines that loop back to the first one
        bb.begin(GL_LINE_LOOP, DefaultVertexFormats.POSITION)

        // Set the color to red
        GlStateManager.color(0.7f, 0f, 0f)
        GlStateManager.glLineWidth(5f)

        // Translate the drawing to the coordinates of the block
        bb.setTranslation(x, y, z)

        // Get the amount of chunks to expand the zone to.
        // This is added to the base chunk and multiplied to get the whole zone covered
        val (xExpansion, zExpansion) = te.xzChunkExpansion()

        // Draw the lines around the chunks borders
        bb.pos(xDiff, 2.0, zDiff).endVertex()
        bb.pos(xDiff + (16.0 * (1 + xExpansion)), 2.0, zDiff).endVertex()
        bb.pos(xDiff + (16.0 * (1 + xExpansion)), 2.0, (zDiff + (16.0 * (1 + zExpansion)))).endVertex()
        bb.pos(xDiff, 2.0, zDiff + (16.0 * (1 + zExpansion))).endVertex()

        // Translate the coordinates back to the base ones
        bb.setTranslation(0.0, 0.0, 0.0)
        tesselator.draw()

        // Enable lighting and textures back
        GlStateManager.enableLighting()
        GlStateManager.enableTexture2D()
    }

    override fun isGlobalRenderer(te: KuarryTileEntity) = true
}