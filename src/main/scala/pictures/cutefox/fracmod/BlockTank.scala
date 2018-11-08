package pictures.cutefox.fracmod

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.world.World

class BlockTank extends Block(Material.GLASS) {
  setCreativeTab(CreativeTabs.MISC)

  override def hasTileEntity(state: IBlockState): Boolean = true
  override def hasTileEntity: Boolean = true

  override def createTileEntity(world: World, state: IBlockState): TileEntity = {
    new TileTank
  }

  override def isOpaqueCube(state: IBlockState): Boolean = false
  override def isFullCube(state: IBlockState): Boolean = false
  override def getBlockLayer: BlockRenderLayer = BlockRenderLayer.CUTOUT
}
