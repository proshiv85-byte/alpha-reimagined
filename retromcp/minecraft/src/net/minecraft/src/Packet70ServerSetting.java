package net.minecraft.src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** A server-wide setting the client should follow, e.g. "skins" = "off". */
public class Packet70ServerSetting extends Packet {
	public String key;
	public String value;

	public Packet70ServerSetting() {
	}

	public Packet70ServerSetting(String var1, String var2) {
		this.key = var1;
		this.value = var2;
	}

	public void readPacketData(DataInputStream var1) throws IOException {
		this.key = var1.readUTF();
		this.value = var1.readUTF();
	}

	public void writePacket(DataOutputStream var1) throws IOException {
		var1.writeUTF(this.key);
		var1.writeUTF(this.value);
	}

	public void processPacket(NetHandler var1) {
		var1.handleServerSetting(this);
	}

	public int getPacketSize() {
		return 4 + this.key.length() + this.value.length();
	}
}
