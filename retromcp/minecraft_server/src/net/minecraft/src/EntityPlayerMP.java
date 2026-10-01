package net.minecraft.src;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

public class EntityPlayerMP extends EntityPlayer {
	public NetServerHandler playerNetServerHandler;
	public MinecraftServer mcServer;
	public ItemInWorldManager theItemInWorldManager;
	public double managedPosX;
	public double managedPosZ;
	public List loadedChunks = new LinkedList();
	public Set loadChunks = new HashSet();
	public double managedPosY;
	public int lastHealth = -99999999;
	public boolean noclip = false;
	private long lastUpdateTime = -1L;

	public EntityPlayerMP(MinecraftServer var1, World var2, String var3, ItemInWorldManager var4) {
		super(var2);
		this.yOffset = 0.0F;
		this.moveToSpawn();
		this.mcServer = var1;
		this.stepHeight = 0.0F;
		var4.thisPlayer = this;
		this.username = var3;
		this.theItemInWorldManager = var4;
		this.yOffset = 0.0F;
	}

	public void onUpdate() {
	}

	public void moveToSpawn() {
		int var1 = this.worldObj.spawnX + this.rand.nextInt(20) - 10;
		int var2 = this.worldObj.spawnZ + this.rand.nextInt(20) - 10;
		int var3 = this.worldObj.getTopSolidOrLiquidBlock(var1, var2);
		this.setLocationAndAngles((double)var1 + 0.5D, (double)var3, (double)var2 + 0.5D, 0.0F, 0.0F);
	}

	public void resetForRespawn() {
		this.noclip = false;
		this.health = 20;
		this.prevHealth = 20;
		this.deathTime = 0;
		this.hurtTime = 0;
		this.heartsLife = 0;
		this.fire = -this.fireResistance;
		this.air = this.maxAir;
		this.fallDistance = 0.0F;
		this.dead = false;
		this.motionX = this.motionY = this.motionZ = 0.0D;
		this.inventory.mainInventory = new ItemStack[this.inventory.mainInventory.length];
		this.inventory.armorInventory = new ItemStack[this.inventory.armorInventory.length];
		this.inventory.craftingInventory = new ItemStack[this.inventory.craftingInventory.length];
		this.moveToSpawn();
	}

	public void onEntityUpdate() {
		if(this.health <= 0 && this.deathTime >= 20) {
			this.deathTime = 19;
		}

		super.onEntityUpdate();
	}

	public void onDeath(Entity var1) {
		this.dead = true;
		this.fire = -this.fireResistance;
	}

	protected void kill() {
		this.attackEntityFrom((Entity)null, 1000);
	}

	public boolean isEntityInsideOpaqueBlock() {
		return !this.noclip && super.isEntityInsideOpaqueBlock();
	}

	public boolean attackEntityFrom(Entity var1, int var2) {
		if(this.noclip && var1 == null) {
			// Noclip (op testing aid): no suffocation, fall, lava, fire or void damage.
			return false;
		} else if(var1 instanceof EntityPlayer && !this.mcServer.pvpOn) {
			return false;
		} else {
			return super.attackEntityFrom(var1, var2);
		}
	}

	public void knockBack(Entity var1, int var2, double var3, double var5) {
		this.motionX = this.motionY = this.motionZ = 0.0D;
		super.knockBack(var1, var2, var3, var5);
		this.playerNetServerHandler.sendPacket(new Packet28EntityVelocity(this));
	}

	public void onUpdateEntity() {
		// The client sends two movement packets per tick; advancing the player
		// on each one would double damage, drowning and burn speed.
		if(this.lastUpdateTime != this.worldObj.worldTime) {
			this.lastUpdateTime = this.worldObj.worldTime;
			super.onUpdate();
		}

		ChunkCoordIntPair var1 = null;
		double var2 = 0.0D;

		for(int var4 = 0; var4 < this.loadedChunks.size(); ++var4) {
			ChunkCoordIntPair var5 = (ChunkCoordIntPair)this.loadedChunks.get(var4);
			double var6 = var5.a(this);
			if(var4 == 0 || var6 < var2) {
				var1 = var5;
				var2 = var5.a(this);
			}
		}

		if(var1 != null) {
			boolean var8 = false;
			if(var2 < 1024.0D) {
				var8 = true;
			}

			if(this.playerNetServerHandler.getNumChunkDataPackets() < 2) {
				var8 = true;
			}

			if(var8) {
				this.loadedChunks.remove(var1);
				this.playerNetServerHandler.sendPacket(new Packet51MapChunk(var1.chunkXPos * 16, 0, var1.chunkZPos * 16, 16, 128, 16, this.mcServer.worldMngr));
				List var9 = this.mcServer.worldMngr.getTileEntityList(var1.chunkXPos * 16, 0, var1.chunkZPos * 16, var1.chunkXPos * 16 + 16, 128, var1.chunkZPos * 16 + 16);

				for(int var10 = 0; var10 < var9.size(); ++var10) {
					TileEntity var7 = (TileEntity)var9.get(var10);
					this.playerNetServerHandler.sendPacket(new Packet59ComplexEntity(var7.xCoord, var7.yCoord, var7.zCoord, var7));
				}
			}
		}

	}

	public void onLivingUpdate() {
		this.motionX = this.motionY = this.motionZ = 0.0D;
		this.isJumping = false;
		super.onLivingUpdate();
	}

	public void onItemPickup(Entity var1, int var2) {
		if(!var1.isDead && var1 instanceof EntityItem) {
			this.playerNetServerHandler.sendPacket(new Packet17AddToInventory(((EntityItem)var1).item, var2));
			this.mcServer.entityTracker.sendPacketToTrackedPlayers(var1, new Packet22Collect(var1.entityID, this.entityID));
		}

		if(!var1.isDead && var1 instanceof EntityArrow) {
			this.playerNetServerHandler.sendPacket(new Packet17AddToInventory(new ItemStack(Item.arrow, 1), 1));
			this.mcServer.entityTracker.sendPacketToTrackedPlayers(var1, new Packet22Collect(var1.entityID, this.entityID));
		}

		super.onItemPickup(var1, var2);
	}

	public void swingItem() {
		if(!this.isSwinging) {
			this.swingProgressInt = -1;
			this.isSwinging = true;
			this.mcServer.entityTracker.sendPacketToTrackedPlayers(this, new Packet18ArmAnimation(this, 1));
		}

	}

	protected float getEyeHeight() {
		return 1.62F;
	}
}
