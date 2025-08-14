package net.riches.biggerbarrels;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;

public class BarrelSerializer {

    public static void saveBarrel(Block barrel, Inventory inv) {
        writeSave(barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel", inv);
    }

    public static Inventory loadBarrel(Block barrel) {
        return loadSave(barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel");
    }

    public static HashMap<Integer, Map<String, Object>> serialize(final Inventory inventory) {
        final HashMap<Integer, Map<String, Object>> barrelContents = new HashMap<>();
        final ItemStack[] inventoryContents = inventory.getContents();

        for (int i = 0; i < 54; ++i) {
            if (inventoryContents[i] == null) continue;

            barrelContents.put(i, inventoryContents[i].serialize());
        }

        return barrelContents;
    }

    // save the barrel contents to a file
    public static void writeSave(final String path, final Inventory inventory) {
        final HashMap<Integer, Map<String, Object>> barrelContents = BarrelSerializer.serialize(inventory);

        try {
            new File(BiggerBarrels.getInstance().getDataFolder(), path).createNewFile();

            final FileOutputStream f = new FileOutputStream(new File(BiggerBarrels.getInstance().getDataFolder(), path));
            final BukkitObjectOutputStream o = new BukkitObjectOutputStream(f);

            o.writeObject(barrelContents);
            o.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // delete a barrels file
    public static void deleteBarrel(Block barrel) {
        new File(BiggerBarrels.getInstance().getDataFolder(), barrel.getX()+","+barrel.getY()+","+barrel.getZ() + ".barrel").delete();
    }

    // load the barrel contents from a file
    public static Inventory loadSave(final String path) {
        try {
            final FileInputStream fi = new FileInputStream(new File(BiggerBarrels.getInstance().getDataFolder(), path));
            final BukkitObjectInputStream oi = new BukkitObjectInputStream(fi);
            final HashMap<Integer, Map<String, Object>> barrelContents = (HashMap<Integer, Map<String, Object>>) oi.readObject();

            oi.close();
            fi.close();

            final Inventory inventory = Bukkit.createInventory(null, 54, "Barrel");

            for (final Map.Entry<Integer, Map<String, Object>> entry : barrelContents.entrySet()) {
                inventory.setItem(entry.getKey(), ItemStack.deserialize(entry.getValue()));
            }

            return inventory;
        } catch (Exception ignored) {}

        return Bukkit.createInventory(null, 54, "Barrel");
    }
}
