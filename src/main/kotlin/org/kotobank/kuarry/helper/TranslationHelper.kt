package org.kotobank.kuarry.helper

import net.minecraft.client.resources.I18n
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

@SideOnly(Side.CLIENT)
object TranslationHelper {
    internal val shiftForDetailsTooltip
        get() = I18n.format(
                "tooltips.hold_shift_for_ext_desc",
                "${TextFormatting.ITALIC}${TextFormatting.YELLOW}",
                "${TextFormatting.RESET}${TextFormatting.GRAY}"
        )
}