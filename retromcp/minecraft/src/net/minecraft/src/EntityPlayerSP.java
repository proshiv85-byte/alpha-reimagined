package net.minecraft.src;

import net.minecraft.client.Minecraft;

public class EntityPlayerSP extends EntityPlayer {
	private boolean healthInitialized = false;
	public MovementInput movementInput;
	private Minecraft mc;

	public EntityPlayerSP(Minecraft var1, World var2, Session var3) {
		super(var2);
		this.mc = var1;
		if(var3 != null && var3.username != null && var3.username.length() > 0) {
			this.skinUrl = "http://www.minecraft.net/skin/" + var3.username + ".png";
			System.out.println("Loading texture " + this.skinUrl);
		}

		this.username = var3.username;
	}

	public void updateEntityActionState() {
		super.updateEntityActionState();
		this.moveStrafing = this.movementInput.moveStrafe;
		this.moveForward = this.movementInput.moveForward;
		this.isJumping = this.movementInput.jump;
	}

	public void toggleNoclip() {
		this.noClip = !this.noClip;
		this.motionX = this.motionY = this.motionZ = 0.0D;
		this.fallDistance = 0.0F;
		this.mc.ingameGUI.addChatMessage("\u00a77Noclip " + (this.noClip ? "on (Space up, Shift down)" : "off"));
		if(this.worldObj.multiplayerWorld) {
			this.sendChatMessage("/noclip " + (this.noClip ? "on" : "off"));
		}

	}

	public void onLivingUpdate() {
		if(this.noClip) {
			// Test-build noclip: fly through blocks, no gravity.
			this.movementInput.updatePlayerMoveState(this);
			float var1 = this.movementInput.moveForward;
			float var2 = this.movementInput.moveStrafe;
			if(this.movementInput.sneak) {
				var1 /= 0.3F;
				var2 /= 0.3F;
			}

			double var3 = 0.6D;
			float var5 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
			float var6 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
			this.motionX = (double)(var2 * var6 - var1 * var5) * var3;
			this.motionZ = (double)(var1 * var6 + var2 * var5) * var3;
			this.motionY = this.movementInput.jump ? var3 : (this.movementInput.sneak ? -var3 : 0.0D);
			this.moveEntity(this.motionX, this.motionY, this.motionZ);
			this.fallDistance = 0.0F;
			this.ySize = 0.0F;
			return;
		}

		this.movementInput.updatePlayerMoveState(this);
		if(this.movementInput.sneak && this.ySize < 0.2F) {
			this.ySize = 0.2F;
		}

		super.onLivingUpdate();
	}

	public void resetPlayerKeyState() {
		this.movementInput.resetKeyState();
	}

	public void handleKeyPress(int var1, boolean var2) {
		this.movementInput.checkKeyForMovementInput(var1, var2);
	}

	public void writeEntityToNBT(NBTTagCompound var1) {
		super.writeEntityToNBT(var1);
		var1.setInteger("Score", this.score);
	}

	public void readEntityFromNBT(NBTTagCompound var1) {
		super.readEntityFromNBT(var1);
		this.score = var1.getInteger("Score");
	}

	public void displayGUIChest(IInventory var1) {
		this.mc.displayGuiScreen(new GuiChest(this.inventory, var1));
	}

	public void displayGUIEditSign(TileEntitySign var1) {
		this.mc.displayGuiScreen(new GuiEditSign(var1));
	}

	public void displayWorkbenchGUI() {
		this.mc.displayGuiScreen(new GuiCrafting(this.inventory));
	}

	public void displayGUIFurnace(TileEntityFurnace var1) {
		this.mc.displayGuiScreen(new GuiFurnace(this.inventory, var1));
	}

	public void attackEntity(Entity var1) {
		int var2 = this.inventory.getDamageVsEntity(var1);
		if(var2 > 0) {
			var1.attackEntityFrom(this, var2);
			ItemStack var3 = this.getCurrentEquippedItem();
			if(var3 != null && var1 instanceof EntityLiving) {
				var3.hitEntity((EntityLiving)var1);
				if(var3.stackSize <= 0) {
					var3.onItemDestroyedByUse(this);
					this.destroyCurrentEquippedItem();
				}
			}
		}

	}

	public void onItemPickup(Entity var1, int var2) {
		this.mc.effectRenderer.addEffect(new EntityPickupFX(this.mc.theWorld, var1, this, -0.5F));
	}

	public int getPlayerArmorValue() {
		return this.inventory.getTotalArmorValue();
	}

	public void interactWithEntity(Entity var1) {
		if(!var1.interact(this)) {
			ItemStack var2 = this.getCurrentEquippedItem();
			if(var2 != null && var1 instanceof EntityLiving) {
				var2.useItemOnEntity((EntityLiving)var1);
				if(var2.stackSize <= 0) {
					var2.onItemDestroyedByUse(this);
					this.destroyCurrentEquippedItem();
				}
			}

		}
	}

	public void sendChatMessage(String var1) {
	}

	public void onPlayerUpdate() {
	}

	public boolean isSneaking() {
		return this.movementInput.sneak;
	}

	public void setHealth(int var1) {
		int var2 = this.health - var1;
		if(!this.healthInitialized) {
			this.healthInitialized = true;
			this.health = var1;
		} else if(var2 <= 0) {
			this.health = var1;
			if(var2 < 0) {
				this.heartsLife = this.heartsHalvesLife / 2;
			}
		} else {
			this.inventory.damageArmor(var2);
			this.prevHealth = this.health;
			this.heartsLife = this.heartsHalvesLife;
			this.health = var1;
			this.hurtTime = this.maxHurtTime = 10;
			this.attackedAtYaw = 0.0F;
			if(this.health <= 0) {
				this.worldObj.playSoundAtEntity(this, this.getDeathSound(), this.getSoundVolume(), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
				this.onDeath((Entity)null);
			} else {
				this.worldObj.playSoundAtEntity(this, this.getHurtSound(), this.getSoundVolume(), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
			}
		}

	}
}
