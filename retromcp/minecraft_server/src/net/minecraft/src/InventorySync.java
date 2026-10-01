package net.minecraft.src;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps container contents (chests, furnaces) consistent between the server and
 * clients that edit them locally.
 *
 * Every state the server sends out is tagged with a revision number, and
 * clients echo the revision their edit was based on. The server then applies
 * only the slots the client actually changed relative to that revision, so an
 * edit that crosses a server-side change (smelting, another player, an echo of
 * the client's own previous edit) no longer overwrites it.
 */
public class InventorySync {
	private static final int HISTORY = 64;
	private int revision = 0;
	private final Map history = new LinkedHashMap();
	private final Map lastEdit = new java.util.HashMap();

	/** Revision of the given contents, recording a new one if they changed. */
	public int currentRevision(ItemStack[] var1) {
		ItemStack[] var2 = (ItemStack[])this.history.get(Integer.valueOf(this.revision));
		if(var2 == null || !sameContents(var2, var1)) {
			++this.revision;
			this.history.put(Integer.valueOf(this.revision), copy(var1));
			if(this.history.size() > HISTORY) {
				Iterator var3 = this.history.keySet().iterator();
				var3.next();
				var3.remove();
			}
		}

		return this.revision;
	}

	/**
	 * Merges a client's edited contents into the server's current contents.
	 * Without a known base revision the client's differing slots win, which is
	 * the old behaviour.
	 *
	 * A client can send several edits based on the same revision (a single slot
	 * click sends the container twice). Each of those already contains the
	 * previous one, so only the difference since that player's previous edit is
	 * applied; otherwise the same stack would be added again.
	 */
	public ItemStack[] merge(int var1, ItemStack[] var2, ItemStack[] var3, String var9) {
		Object[] var10 = (Object[])this.lastEdit.get(var9);
		ItemStack[] var4;
		if(var10 != null && ((Integer)var10[0]).intValue() == var1) {
			var4 = (ItemStack[])var10[1];
		} else {
			var4 = (ItemStack[])this.history.get(Integer.valueOf(var1));
		}

		if(var4 == null) {
			var4 = var3;
		}

		this.lastEdit.put(var9, new Object[]{Integer.valueOf(var1), copy(var2)});

		ItemStack[] var5 = new ItemStack[var3.length];

		for(int var6 = 0; var6 < var3.length; ++var6) {
			ItemStack var7 = var6 < var4.length ? var4[var6] : null;
			ItemStack var8 = var6 < var2.length ? var2[var6] : null;
			var5[var6] = mergeSlot(var7, var8, var3[var6]);
		}

		return var5;
	}

	private static ItemStack mergeSlot(ItemStack var0, ItemStack var1, ItemStack var2) {
		if(sameStack(var0, var1)) {
			return var2;
		}

		ItemStack var3 = var1 != null ? var1 : var0;
		if(var2 != null && var3 != null && sameItem(var2, var3) && (var0 == null || sameItem(var0, var3)) && (var1 == null || sameItem(var1, var3))) {
			int var4 = var2.stackSize + (var1 == null ? 0 : var1.stackSize) - (var0 == null ? 0 : var0.stackSize);
			if(var4 <= 0) {
				return null;
			}

			return new ItemStack(var2.itemID, Math.min(var4, var2.getMaxStackSize()), var2.itemDmg);
		}

		return var1 == null ? null : var1.copy();
	}

	private static boolean sameItem(ItemStack var0, ItemStack var1) {
		return var0.itemID == var1.itemID && var0.itemDmg == var1.itemDmg;
	}

	private static boolean sameStack(ItemStack var0, ItemStack var1) {
		return var0 == null ? var1 == null : var1 != null && sameItem(var0, var1) && var0.stackSize == var1.stackSize;
	}

	private static boolean sameContents(ItemStack[] var0, ItemStack[] var1) {
		if(var0.length != var1.length) {
			return false;
		}

		for(int var2 = 0; var2 < var0.length; ++var2) {
			if(!sameStack(var0[var2], var1[var2])) {
				return false;
			}
		}

		return true;
	}

	private static ItemStack[] copy(ItemStack[] var0) {
		ItemStack[] var1 = new ItemStack[var0.length];

		for(int var2 = 0; var2 < var0.length; ++var2) {
			var1[var2] = var0[var2] == null ? null : var0[var2].copy();
		}

		return var1;
	}
}
