package pictures.cutefox.fracmod.serialization

import collection.JavaConverters._
import net.minecraft.nbt.{JsonToNBT, NBTBase, NBTTagCompound, NBTTagList}
import net.minecraftforge.fluids.FluidStack

import scala.util.Try

trait NBT[T] {
  def encode(thing: T): NBTBase
  def decode(tag: NBTBase): Option[T]
}

object NBT {
  def embed[T](thing: T)(implicit obj: NBT[T]): EmbeddedNBT =
    EmbeddedNBT(obj.encode(thing).toString)

  def unembed[T](msg: EmbeddedNBT)(implicit obj: NBT[T]): Option[T] =
    Option(JsonToNBT.getTagFromJson(msg.data))
      .flatMap(obj.decode)

  def saveInto[T](name: String, thing: T, into: NBTTagCompound)(implicit obj: NBT[T]): NBTTagCompound = {
    into.setTag(name, obj.encode(thing))
    into
  }

  def loadFrom[T](name: String, from: NBTTagCompound)(implicit obj: NBT[T]): Option[T] =
    Option(from.getTag(name)).flatMap(obj.decode)

  implicit class NBTTagCompoundOps(val compound: NBTTagCompound) extends AnyVal {
    def withEncoded[T](name: String, thing: T)(implicit obj: NBT[T]): NBTTagCompound = {
      compound.setTag(name, obj.encode(thing))
      compound
    }

    def loadDecoded[T](name: String)(implicit obj: NBT[T]): Option[T] =
      Option(compound.getTag(name)).flatMap(obj.decode)
  }

  implicit def handleFluidStack: NBT[FluidStack] = new NBT[FluidStack] {
    override def encode(thing: FluidStack): NBTBase =
      thing.writeToNBT(new NBTTagCompound)
    override def decode(tag: NBTBase): Option[FluidStack] =
      Try(tag.asInstanceOf[NBTTagCompound])
        .toOption
        .flatMap(tag => Option(FluidStack.loadFluidStackFromNBT(tag)))
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
