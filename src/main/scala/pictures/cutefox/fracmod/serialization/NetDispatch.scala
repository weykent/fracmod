package pictures.cutefox.fracmod.serialization

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

trait NetDispatch[M] {
  def dispatch(message: M, ctx: MessageContext): Option[NetworkResponse]
}

object NetDispatch {
  def apply[M](message: M, ctx: MessageContext)(implicit obj: NetDispatch[M]): Option[NetworkResponse] =
    obj.dispatch(message, ctx)

  implicit def dispatchRequest: NetDispatch[NetworkRequest] = (req, ctx) => {
    import NetworkRequest.Kind
    req.kind match {
      case Kind.UpdateTank(u) => NetDispatch(u, ctx)
      case _ => None
    }
  }

  implicit def dispatchUpdateTank: NetDispatch[UpdateTank] = (update, ctx) => {
    None
  }
}
