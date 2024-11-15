package catgirlroutes.module.impl.dungeons

import Hclip.hclip
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.commands.Ring
import catgirlroutes.commands.RingManager.loadRings
import catgirlroutes.commands.RingManager.rings
import catgirlroutes.commands.editmode
import catgirlroutes.commands.ringsActive
import catgirlroutes.events.ReceivePacketEvent
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.impl.misc.LavaClip.lavaClipToggle
import catgirlroutes.module.settings.impl.StringSetting
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.jump
import catgirlroutes.utils.MovementUtils.setKey
import catgirlroutes.utils.MovementUtils.stopMovement
import catgirlroutes.utils.MovementUtils.stopVelo
import catgirlroutes.utils.ServerRotateUtils.resetRotations
import catgirlroutes.utils.ServerRotateUtils.set
import catgirlroutes.utils.Utils.airClick
import catgirlroutes.utils.Utils.getYawAndPitch
import catgirlroutes.utils.Utils.leftClick
import catgirlroutes.utils.Utils.snapTo
import catgirlroutes.utils.Utils.swapFromName
import catgirlroutes.utils.render.WorldRenderUtils.drawP3box
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color.black
import java.awt.Color.white
import kotlin.math.abs
import kotlin.math.floor


object AutoP3 : Module(
    "Auto P3",
    category = Category.DUNGEON,
    description = "A module that allows you to place down rings that execute various actions."
){
    val selectedRoute = StringSetting("Selected route", "1", description = "Name of the selected route for auto p3.")
    var termFound = false
    var termListener = false
    val termTitles: Array<String> = arrayOf("Click in order!", "Select all the", "What starts with:", "Change all to the same color!", "Correct all the panes!", "Click the button on time!")

    init {
        this.addSettings(
            selectedRoute
        )
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        loadRings()
    }

    private val cooldownMap = mutableMapOf<String, Boolean>()

    @OptIn(DelicateCoroutinesApi::class)
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!ringsActive || !this.enabled || editmode) return
        rings.forEach { ring ->
            val key = "${ring.x},${ring.y},${ring.z},${ring.type}"
            val cooldown: Boolean = cooldownMap[key] == true
            if(inRing(ring)) {
                if (ring.arguments!!.contains("term") && !termFound) {
                    termListener = true
                    return
                } else  scheduleTask (1) { termFound = false }
                if (cooldown) return@forEach
                cooldownMap[key] = true
                GlobalScope.launch {
                    executeAction(ring)
                }
            } else if (cooldown) {
                cooldownMap[key] = false
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!ringsActive || !this.enabled ) return
        rings.forEach { ring ->
            val key = "${ring.x},${ring.y},${ring.z},${ring.type}"
            val cooldown: Boolean = cooldownMap[key] == true
            val color = if (cooldown) white else black
            drawP3box(ring.x - ring.width / 2, ring.y, ring.z - ring.width / 2, ring.width.toDouble(), ring.height.toDouble(), ring.width.toDouble(), color, 4F, false)
        }
    }

    @SubscribeEvent
    fun onTerm(event: ReceivePacketEvent) {
        if (!termListener) return
        if (event.packet !is S2DPacketOpenWindow) return
        if (event.packet.windowTitle?.unformattedText in termTitles) {
            modMessage("Term found")
            termFound = true
            termListener = false
        }
    }

    private fun inRing(ring: Ring): Boolean {
        val distanceX = abs(mc.renderManager.viewerPosX - ring.x)
        val distanceY = abs(mc.renderManager.viewerPosY - ring.y)
        val distanceZ = abs(mc.renderManager.viewerPosZ - ring.z)

        return distanceX < (ring.width / 2) && distanceY < ring.height && distanceY >= -0.5 && distanceZ < (ring.width / 2);
    }

    private suspend fun executeAction(ring: Ring) {
        val actiondelay: Int = if (ring.delay == null) 0 else ring.delay!!
        delay(actiondelay.toLong())
        if (ring.arguments != null) {
            if (ring.arguments!!.contains("stop")) stopVelo()
            if (ring.arguments!!.contains("walk")) setKey("w", true)
            if (ring.arguments!!.contains("look")) snapTo(ring.yaw, ring.pitch)
        }
        when(ring.type) {
            "walk" -> {
                modMessage("Walking!")
                setKey("w", true)
            }
            "jump" -> {
                modMessage("Jumping!")
                jump()
            }
            "stop" -> {
                modMessage("Stopping!")
                stopMovement()
                stopVelo()
            }
            "boom" -> {
                modMessage("Bomb denmark!")
                swapFromName("infinityboom tnt")
                scheduleTask(1) {leftClick()}
            }
            "hclip" -> {
                modMessage("Hclipping!")
                hclip(ring.yaw)
            }
            "vclip" -> {
                modMessage("Vclipping!")
                lavaClipToggle(ring.depth!!.toDouble(), true)
            }
            "bonzo" -> {
                modMessage("Bonzoing!")
                swapFromName("bonzo's staff")
                set(ring.yaw, ring.pitch)
                scheduleTask(1) {
                    airClick()
                    resetRotations()
                }
            }
            "look" -> {
                modMessage("Looking!")
                snapTo(ring.yaw, ring.pitch)
            }
            "align" -> {
                modMessage("Aligning!")
                mc.thePlayer.setPosition(floor(mc.thePlayer.posX) + 0.5, mc.thePlayer.posY, floor(mc.thePlayer.posZ) + 0.5)
            }
            "block" -> {
                val(yaw, pitch) = getYawAndPitch(ring.lookBlock!!.xCoord, ring.lookBlock!!.yCoord, ring.lookBlock!!.zCoord)
                snapTo(yaw, pitch)
            }
        }
    }
}