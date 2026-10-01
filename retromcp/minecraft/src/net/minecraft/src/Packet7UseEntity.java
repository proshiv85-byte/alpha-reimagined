package net.minecraft.src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet7UseEntity extends Packet {
	public int playerEntityId;
	public int targetEntity;
	public byte isLeftClick;

	public Packet7UseEntity() {
	}

	public Packet7UseEntity(int var1, int var2, int var3) {
		this.playerEntityId = var1;
		this.targetEntity = var2;
		this.isLeftClick = (byte)var3;
	}

	public void readPacketData(DataInputStream var1) throws IOException {
		this.playerEntityId = var1.readInt();
		this.targetEntity = var1.readInt();
		this.isLeftClick = var1.readByte();
	}

	public void writePacket(DataOutputStream var1) throws IOException {
		var1.writeInt(this.playerEntityId);
		var1.writeInt(this.targetEntity);
		var1.writeByte(this.isLeftClick);
	}

	public void processPacket(NetHandler var1) {
		var1.handleUseEntity(this);
	}

	public int getPacketSize() {
		return 9;
	}
}
