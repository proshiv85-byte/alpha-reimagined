package net.minecraft.src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Packet9Respawn extends Packet {
	public void readPacketData(DataInputStream var1) throws IOException {
	}

	public void writePacket(DataOutputStream var1) throws IOException {
	}

	public void processPacket(NetHandler var1) {
		var1.handleRespawn(this);
	}

	public int getPacketSize() {
		return 0;
	}
}
