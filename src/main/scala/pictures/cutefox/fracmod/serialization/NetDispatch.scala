package pictures.cutefox.fracmod.serialization

import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import pictures.cutefox.fracmod.TileFractionatingColumn
import pictures.cutefox.fracmod.TileFractionatingColumn.FractionatingFluidStack

trait NetDispatch[M] {
  def dispatch(message: M, ctx: MessageContext): Option[NetPacket]
}

object NetDispatch {
  def apply[M](message: M, ctx: MessageContext)(implicit obj: NetDispatch[M]): Option[NetPacket] =
    obj.dispatch(message, ctx)

  implicit def dispatchRequest: NetDispatch[NetPacket] = (req, ctx) => {
    import NetPacket.Kind
    req.kind match {
      case Kind.UpdateFluids(u) => NetDispatch(u, ctx)
      case _ => None
    }
  }

  // XXX: I'm not super happy with the organization of this dispatching. Should
  // I make these in separate files? How does that work with typeclass implicit
  // objects?
  implicit def dispatchUpdateFluids: NetDispatch[UpdateFluids] = (update, ctx) => {
    val mc = Minecraft.getMinecraft
    // XXX: There's a better way to write anonymous Runnable blocks, right?
    mc.addScheduledTask(new Runnable {
      override def run(): Unit = {
        val pos = update.getPosition
        val blockPos = new BlockPos(pos.x, pos.y, pos.z)
        val world = mc.world
        if (!world.isBlockLoaded(blockPos)) {
          return
        }
        world.getTileEntity(blockPos) match {
          case tile: TileFractionatingColumn => {
            for (fluids <- NBT.unembed[List[FractionatingFluidStack]](update.getFluids)) {
              tile.fluids = fluids
            }
          }
          case _ =>
        }
      }
    })
    None
  }
}
