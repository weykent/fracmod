package pictures.cutefox.fracmod.serialization

import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pictures.cutefox.fracmod.TileTank

trait NetDispatch[M] {
  def dispatch(message: M, ctx: MessageContext): Option[NetPacket]
}

object NetDispatch {
  def apply[M](message: M, ctx: MessageContext)(implicit obj: NetDispatch[M]): Option[NetPacket] =
    obj.dispatch(message, ctx)

  implicit def dispatchRequest: NetDispatch[NetPacket] = (req, ctx) => {
    import NetPacket.Kind
    req.kind match {
      case Kind.UpdateTank(u) => NetDispatch(u, ctx)
      case _ => None
    }
  }

  implicit def dispatchUpdateTank: NetDispatch[UpdateTank] = (update, ctx) => {
    val mc = Minecraft.getMinecraft
    mc.addScheduledTask(new Runnable {
      override def run(): Unit = {
        val pos = update.getPosition
        val blockPos = new BlockPos(pos.x, pos.y, pos.z)
        val world = mc.world
        if (!world.isBlockLoaded(blockPos)) {
          return
        }
        world.getTileEntity(blockPos) match {
          case tank: TileTank => {
            println(s"-!- loaded: $update $blockPos $tank")
            for (fluids <- NBT.unembed[List[FluidStack]](update.getNewContents.getFluids)) {
              tank.fluids = fluids
            }
          }
          case _ =>
        }
      }
    })
    None
  }
}
