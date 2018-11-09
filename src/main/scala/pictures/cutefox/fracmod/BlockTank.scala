package pictures.cutefox.fracmod

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class BlockTank extends Block(Material.GLASS) {
  setCreativeTab(CreativeTabs.MISC)

  override def hasTileEntity(state: IBlockState): Boolean = true
  override def hasTileEntity: Boolean = true

  override def createTileEntity(world: World, state: IBlockState): TileEntity =
    new TileTank

  override def onBlockPlacedBy(worldIn: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, stack: ItemStack): Unit = {
    super.onBlockPlacedBy(worldIn, pos, state, placer, stack)
    worldIn.getTileEntity(pos) match {
      case tank: TileTank => {
        tank.setSomeFluid()
      }
    }
  }

  override def isOpaqueCube(state: IBlockState): Boolean = false
  override def isFullCube(state: IBlockState): Boolean = false
  override def getBlockLayer: BlockRenderLayer = BlockRenderLayer.CUTOUT
}
