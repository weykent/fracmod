package pictures.cutefox.fracmod

import io.netty.buffer.{ByteBuf, ByteBufInputStream, ByteBufOutputStream}
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint
import net.minecraftforge.fml.common.network.simpleimpl.{IMessage, IMessageHandler, MessageContext}
import net.minecraftforge.fml.relauncher.Side
import pictures.cutefox.fracmod.serialization.{NetDispatch, NetPacket}

class NetMessage(private var messageOpt: Option[NetPacket] = None) extends IMessage {
  def this() = this(None)

  override def toBytes(buf: ByteBuf): Unit = {
    for (message <- messageOpt) {
      // XXX: I.. think this might be adding a bunch of zero padding on the
      // end. Not sure if that's important to fix, or how to tell where the end
      // of the serialized message is.
      message.writeDelimitedTo(new ByteBufOutputStream(buf))
    }
    println(s"bytes out: ${NetMessage.bytesHex(buf.array())}")
  }

  override def fromBytes(buf: ByteBuf): Unit = {
    println(s"bytes in: ${NetMessage.bytesHex(buf.array())}")
    messageOpt = NetPacket.parseDelimitedFrom(new ByteBufInputStream(buf))
  }
}

object NetMessage {
  private val networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(FracMod.MODID)
  for (side <- Side.values()) {
    networkWrapper.registerMessage(classOf[Handler], classOf[NetMessage], 0, side)
  }

  private def bytesHex(bytes: Array[Byte]): String =
    bytes.map("%02x" format _).mkString

  class Handler extends IMessageHandler[NetMessage, NetMessage] {
    override def onMessage(reqMessage: NetMessage, ctx: MessageContext): NetMessage = {
      println(s"-!- received: ${reqMessage.messageOpt}")
      var resp: Option[NetPacket] = None
      for (req <- reqMessage.messageOpt) {
        resp = NetDispatch(req, ctx)
      }
      resp.map(m => new NetMessage(Some(m))).orNull
    }
  }

  private def wrap(req: NetPacket) = new NetMessage(Some(req))

  def sendToClient(req: NetPacket): Unit = {
    println(s"-!- sending: $req")
    networkWrapper.sendToAll(wrap(req))
  }
  def sendToAllTracking(req: NetPacket, target: TargetPoint): Unit =
    networkWrapper.sendToAllTracking(wrap(req), target)
  def sendToServer(req: NetPacket): Unit =
    networkWrapper.sendToServer(wrap(req))
}
