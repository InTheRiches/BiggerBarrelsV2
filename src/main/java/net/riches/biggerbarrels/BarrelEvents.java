package net.riches.biggerbarrels;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.Utils.uBlock;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BarrelEvents implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        for (BlockState tileEntity : e.getChunk().getTileEntities()) {
            if (!(tileEntity instanceof Barrel barrel)) {
                return;
            }

            BiggerBarrels.getInstance().getBarrel(barrel.getBlock());
        }
    }

    @EventHandler
    public void onHopperInventorySearch(HopperInventorySearchEvent event) {
        Block searchBlock = event.getSearchBlock();

        // Only override when pulling from our custom barrel
        if (event.getContainerType() == HopperInventorySearchEvent.ContainerType.SOURCE
                && BiggerBarrels.getInstance().getBarrel(searchBlock) != null
                && searchBlock.getType() == Material.BARREL) {

            Inventory customInventory = BiggerBarrels.getInstance().getBarrel(searchBlock);
            event.setInventory(customInventory);
        }
        if (event.getContainerType() == HopperInventorySearchEvent.ContainerType.DESTINATION
                && BiggerBarrels.getInstance().getBarrel(searchBlock) != null
                && searchBlock.getType() == Material.BARREL) {

            Inventory customInventory = BiggerBarrels.getInstance().getBarrel(searchBlock);
            event.setInventory(customInventory);
        }
    }

    @EventHandler
    public void onHopperMinecartTick(VehicleUpdateEvent event) {
        if (event.getVehicle() instanceof HopperMinecart cart) {
            Block above = cart.getLocation().add(0, 1, 0).getBlock();

            if (above.getType() == Material.BARREL) {
                Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(above);
                if (barrelInv != null) {
                    List<ItemStack> contents = new ArrayList<>();
                    for (ItemStack item : barrelInv.getContents()) {
                        if (item != null) {
                            contents.add(item.clone());
                        }
                    }

                    if (!contents.isEmpty()) {
                        if (cart.getInventory().firstEmpty() == -1 && Objects.requireNonNull(cart.getInventory().getItem(4)).getAmount() == 64) {
                            return;
                        }
                        ItemStack itemToMove = contents.getFirst(); // Get the first item in the barrel
                        itemToMove.setAmount(1); // Move only one item
                        cart.getInventory().addItem(itemToMove);
                        barrelInv.removeItem(itemToMove);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBarrelOpened(PlayerInteractEvent e) {
        if (!e.hasBlock()) return;

        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
            assert e.getClickedBlock() != null;
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(e.getClickedBlock().getLocation(), true, null);

            if (claim != null && claim.getOwnerID() != null && !claim.getOwnerID().equals(e.getPlayer().getUniqueId()) && !claim.hasExplicitPermission(e.getPlayer(), ClaimPermission.Inventory)) {
                return;
            }
        }

        if (Bukkit.getPluginManager().isPluginEnabled("Lands")) {
            LandsIntegration lands = LandsIntegration.of(BiggerBarrels.getInstance());
            assert e.getClickedBlock() != null;
            Area area = lands.getArea(e.getClickedBlock().getLocation());

            if (area != null && !area.isTrusted(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp())
                return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("ChestShop")) {
            Sign connectedSign = uBlock.getConnectedSign(e.getClickedBlock());

            if (connectedSign != null && !ChestShopSign.canAccess(e.getPlayer(), connectedSign)) {
                return;
            }
        }

        assert e.getClickedBlock() != null;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK | e.getClickedBlock().getType() != Material.BARREL) return;

        if (e.getPlayer().isSneaking() && e.getItem() != null && (e.getItem().getType().isBlock() || e.getItem().getType() == Material.ITEM_FRAME)) return;

        e.getPlayer().playSound(e.getClickedBlock().getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0f, 1.0f);
        e.setCancelled(true);

        // load barrel from e.getClickedBlock()
        Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(e.getClickedBlock());
        e.getPlayer().openInventory(barrelInv);
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!event.getView().getTitle().contains("Barrel"))
            return;

        ((Player)event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.BLOCK_BARREL_CLOSE, 1.0f, 1.0f);
    }

    @EventHandler
    public void onBarrelBroke(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BARREL) return;

        Inventory barrelInv = BiggerBarrels.getInstance().getBarrel(e.getBlock());
        for (ItemStack item : barrelInv.getContents()) {
            if (item == null) continue;

            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
        }

        BiggerBarrels.getInstance().deleteBarrel(e.getBlock());
    }
}
