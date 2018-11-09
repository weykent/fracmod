package pictures.cutefox.fracmod

import io.netty.buffer.{ByteBuf, ByteBufOutputStream}
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import net.minecraftforge.fml.relauncher.Side
import pictures.cutefox.fracmod.serialization.{NetDispatch, NetworkRequest, NetworkResponse}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

case class PbMessage[M <: GeneratedMessage with Message[M]](private var messageOpt: Option[M] = None)(implicit companion: GeneratedMessageCompanion[M]) extends IMessage {
  override def toBytes(buf: ByteBuf): Unit = {
    for (message <- messageOpt) {
      message.writeTo(new ByteBufOutputStream(buf))
    }
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    messageOpt = Some(companion.parseFrom(buf.array()))
  }
}

object PbMessage {
  private val networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(FracMod.MODID)
  for (side <- Side.values()) {
    networkWrapper.registerMessage(classOf[Handler], classOf[PbMessage[NetworkRequest]], 0, side)
  }

  class Handler extends IMessageHandler[PbMessage[NetworkRequest], PbMessage[NetworkResponse]] {
    override def onMessage(reqMessage: PbMessage[NetworkRequest], ctx: MessageContext): PbMessage[NetworkResponse] = {
      println(s"-!- received: ${reqMessage.messageOpt}")
      var resp: Option[NetworkResponse] = None
      for (req <- reqMessage.messageOpt) {
        resp = NetDispatch(req, ctx)
      }
      PbMessage(resp)
    }
  }

  private def wrap(req: NetworkRequest) = PbMessage(Some(req))

  def sendToClient(req: NetworkRequest): Unit = {
    println(s"-!- sending: $req")
    networkWrapper.sendToAll(wrap(req))
  }
  def sendToAllTracking(req: NetworkRequest, target: TargetPoint): Unit =
    networkWrapper.sendToAllTracking(wrap(req), target)
  def sendToServer(req: NetworkRequest): Unit =
    networkWrapper.sendToServer(wrap(req))
}
