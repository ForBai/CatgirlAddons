package catgirlroutes.ui.misc.elements.impl

import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.elements.MiscElement
import catgirlroutes.utils.render.HUDRenderUtils
import java.awt.Color

class MiscElementButton(
    var name: String,
    x: Double,
    y: Double,
    width: Double = 80.0,
    height: Double = 20.0,
    var thickness: Double = 2.0,
    var action: () -> Unit
) : MiscElement(x, y, width, height) {

    override fun render(mouseX: Int, mouseY: Int, x: Double, y: Double) {
        HUDRenderUtils.drawRoundedBorderedRect(
            this.x, this.y, this.width, this.height, 5.0, thickness,
            Color(ColorUtil.elementColor), if (this.isHovered(mouseX, mouseY)) ColorUtil.clickGUIColor else Color(ColorUtil.outlineColor)
        )
        FontUtil.drawTotalCenteredString(name, x + width / 2.0, y + height / 2.0)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered(mouseX, mouseY)) {
            action()
            return true
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }
}