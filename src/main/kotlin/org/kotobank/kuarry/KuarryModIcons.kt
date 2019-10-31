package org.kotobank.kuarry

import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = KuarryMod.MODID)
object KuarryModIcons {

    @SubscribeEvent
    fun textureStitchPre(event: TextureStitchEvent.Pre) =
            registerIcons(event.map)

    private fun TextureMap.registerIcon(icon: String): TextureAtlasSprite =
        registerSprite(ResourceLocation(KuarryMod.MODID, "gui/icons/$icon"))

    /** This is the atlas resource location. The texture HAS to be bound to this location,
     *  or things will fail to draw in certain specific situations. */
    val atlasResourceLocation = ResourceLocation("textures/atlas/blocks.png")

    lateinit var button: TextureAtlasSprite
    lateinit var buttonHighlight: TextureAtlasSprite

    lateinit var iconRSIgnore: TextureAtlasSprite
    lateinit var iconRSWithout: TextureAtlasSprite
    lateinit var iconRSWith: TextureAtlasSprite
    lateinit var iconDisable: TextureAtlasSprite

    lateinit var boundsDisable: TextureAtlasSprite
    lateinit var boundsEnable: TextureAtlasSprite

    private fun registerIcons(map: TextureMap) {
        with (map) {
            button = registerIcon("button")
            buttonHighlight = registerIcon("button_highlight")
            iconRSIgnore = registerIcon("activation_always_enable")
            iconRSWithout = registerIcon("activation_rs_without")
            iconRSWith = registerIcon("activation_rs_with")
            iconDisable = registerIcon("activation_always_disable")

            boundsDisable = registerIcon("bounds_disable")
            boundsEnable = registerIcon("bounds_enable")
        }
    }
}