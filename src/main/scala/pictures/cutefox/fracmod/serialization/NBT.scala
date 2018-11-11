package pictures.cutefox.fracmod.serialization

import net.minecraft.nbt._
import net.minecraftforge.fluids.FluidStack
import pictures.cutefox.fracmod.TileFractionatingColumn.FractionatingFluidStack

import scala.collection.JavaConverters._
import scala.util.Try

trait NBT[T] {
  def encode(thing: T): NBTBase
  def decode(tag: NBTBase): Option[T]
}

object NBT {
  def embed[T](thing: T)(implicit obj: NBT[T]): EmbeddedNBT =
    EmbeddedNBT(new NBTTagCompound().withEncoded("_", thing).toString)

  def unembed[T](msg: EmbeddedNBT)(implicit obj: NBT[T]): Option[T] =
    Option(JsonToNBT.getTagFromJson(msg.data))
      .flatMap(tag => tag.loadDecoded[T]("_"))

  def saveInto[T](name: String, thing: T, into: NBTTagCompound)(implicit obj: NBT[T]): NBTTagCompound = {
    into.setTag(name, obj.encode(thing))
    into
  }

  def loadFrom[T](name: String, from: NBTTagCompound)(implicit obj: NBT[T]): Option[T] =
    Option(from.getTag(name)).flatMap(obj.decode)

  implicit class NBTTagCompoundOps(val compound: NBTTagCompound) extends AnyVal {
    def withEncoded[T](name: String, thing: T)(implicit obj: NBT[T]): NBTTagCompound =
      NBT.saveInto(name, thing, compound)

    def loadDecoded[T](name: String)(implicit obj: NBT[T]): Option[T] =
      NBT.loadFrom[T](name, compound)
  }

  implicit def handleFluidStack: NBT[FluidStack] = new NBT[FluidStack] {
    override def encode(thing: FluidStack): NBTBase =
      thing.writeToNBT(new NBTTagCompound)
    override def decode(tag: NBTBase): Option[FluidStack] =
      Try(tag.asInstanceOf[NBTTagCompound])
        .toOption
        .flatMap(tag => Option(FluidStack.loadFluidStackFromNBT(tag)))
  }

  implicit def handleFractionatingFluidStack: NBT[FractionatingFluidStack] = new NBT[FractionatingFluidStack] {
    override def encode(thing: FractionatingFluidStack): NBTBase =
      new NBTTagCompound()
        .withEncoded("fs", thing.fs)
        .withEncoded("m", thing.moving)

    override def decode(tag: NBTBase): Option[FractionatingFluidStack] =
      Try(tag.asInstanceOf[NBTTagCompound])
        .toOption
        .flatMap { tag =>
          (tag.loadDecoded[FluidStack]("fs"), tag.loadDecoded[Boolean]("m")) match {
            case (Some(fs), Some(moving)) => Some(FractionatingFluidStack(fs, moving))
            case _ => None
          }
        }
  }

  implicit def handleBoolean: NBT[Boolean] = new NBT[Boolean] {
    override def encode(thing: Boolean): NBTBase =
      new NBTTagByte(if (thing) 1 else 0)

    override def decode(tag: NBTBase): Option[Boolean] =
      Try(tag.asInstanceOf[NBTTagByte])
        .toOption
        .map { b => b.getByte != 0 }
  }

  implicit def handleList[T](implicit obj: NBT[T]): NBT[List[T]] = new NBT[List[T]] {
    override def encode(thing: List[T]): NBTBase = {
      val ret = new NBTTagList
      for (x <- thing) {
        ret.appendTag(obj.encode(x))
      }
      ret
    }

    override def decode(listTag: NBTBase): Option[List[T]] = {
      Try(listTag.asInstanceOf[NBTTagList])
        .toOption
        .map(_.iterator().asScala.flatMap(tag => obj.decode(tag)).toList)
    }
  }
}
