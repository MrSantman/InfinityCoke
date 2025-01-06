package com.example.cocainegrinding;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

import java.util.ArrayList;
import java.util.List;

public class CocaineGrindingPlugin extends JavaPlugin implements Listener {

    private final List<Location> cocainePlantLocations = new ArrayList<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("CocaineGrindingPlugin is ingeschakeld!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CocaineGrindingPlugin is uitgeschakeld!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dit commando kan alleen door spelers worden gebruikt.");
            return true;
        }

        Player player = (Player) sender;

        if (label.equalsIgnoreCase("setcocainelocation")) {
            Location location = player.getLocation();
            cocainePlantLocations.add(location);
            player.sendMessage(ChatColor.GREEN + "Cocaïneplant locatie ingesteld op: " + locatieNaarString(location));
            return true;
        }

        if (label.equalsIgnoreCase("listcocainelocations")) {
            if (cocainePlantLocations.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Er zijn geen cocaïneplantlocaties ingesteld.");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Cocaïneplantlocaties:");
                for (Location loc : cocainePlantLocations) {
                    player.sendMessage(locatieNaarString(loc));
                }
            }
            return true;
        }

        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isInCokeRegion(event.getPlayer())) {
            event.getPlayer().sendMessage(ChatColor.RED + "Je bevindt je niet in een toegestane cocaïne locatie!");
            return;
        }

        if (cocainePlantLocations.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
            Player speler = event.getPlayer();

            // Drop cocaïnebladeren
            ItemStack cocaineLeaves = new ItemStack(Material.FERN, 1); // Representing cocaine leaves
            ItemMeta leavesMeta = cocaineLeaves.getItemMeta();
            if (leavesMeta != null) {
                leavesMeta.setDisplayName(ChatColor.WHITE + "Cocaïnebladeren");
                cocaineLeaves.setItemMeta(leavesMeta);
            }
            speler.getWorld().dropItemNaturally(event.getBlock().getLocation(), cocaineLeaves);

            // Geef feedback aan speler
            speler.sendMessage(ChatColor.GREEN + "Je hebt cocaïnebladeren geoogst!");

            // Verwijder de plant tijdelijk en laat deze later teruggroeien
            long growTime = config.getLong("grow-time", 600L) * 20; // Convert seconds to ticks
            event.getBlock().setType(Material.AIR);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                event.getBlock().setType(Material.SUGAR_CANE);
                speler.sendMessage(ChatColor.YELLOW + "Een cocaïneplant is opnieuw gegroeid.");
            }, growTime);
        }
    }

    public void convertCocaineLeaves(Player player) {
        if (!isInCokeRegion(player)) {
            player.sendMessage(ChatColor.RED + "Je moet in een cocaïne regio zijn om dit te doen!");
            return;
        }

        ItemStack leaves = new ItemStack(Material.FERN, 1);
        ItemMeta leavesMeta = leaves.getItemMeta();
        if (leavesMeta != null) {
            leavesMeta.setDisplayName(ChatColor.WHITE + "Cocaïnebladeren");
            leaves.setItemMeta(leavesMeta);
        }

        if (player.getInventory().containsAtLeast(leaves, 1)) {
            player.getInventory().removeItem(leaves);
            ItemStack cokePowder = new ItemStack(Material.SUGAR, 1);
            ItemMeta powderMeta = cokePowder.getItemMeta();
            if (powderMeta != null) {
                powderMeta.setDisplayName(ChatColor.GRAY + "Cocaine");
                cokePowder.setItemMeta(powderMeta);
            }
            player.getInventory().addItem(cokePowder);
            player.sendMessage(ChatColor.GREEN + "Je hebt cocaïnebladeren omgezet naar Cocaine!");
        } else {
            player.sendMessage(ChatColor.RED + "Je hebt geen cocaïnebladeren om te verwerken!");
        }
    }

    public void sellCokePowder(Player player) {
        if (!isInCokeRegion(player)) {
            player.sendMessage(ChatColor.RED + "Je moet in een cocaïne regio zijn om dit te doen!");
            return;
        }

        ItemStack cokePowder = new ItemStack(Material.SUGAR, 1);
        ItemMeta powderMeta = cokePowder.getItemMeta();
        if (powderMeta != null) {
            powderMeta.setDisplayName(ChatColor.GRAY + "Cocaine");
            cokePowder.setItemMeta(powderMeta);
        }

        if (player.getInventory().containsAtLeast(cokePowder, 1)) {
            player.getInventory().removeItem(cokePowder);
            int price = config.getInt("sell-price", 100);
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "eco give " + player.getName() + " " + price);
            player.sendMessage(ChatColor.GREEN + "Je hebt Cocaine verkocht voor " + price + " dollar!");
        } else {
            player.sendMessage(ChatColor.RED + "Je hebt geen Cocaine om te verkopen!");
        }
    }

    private String locatieNaarString(Location location) {
        return "Wereld: " + location.getWorld().getName() + ", X: " + location.getBlockX() + ", Y: " + location.getBlockY() + ", Z: " + location.getBlockZ();
    }

    public boolean isInCokeRegion(Player player) {
        com.sk89q.worldedit.util.Location wgLocation = BukkitAdapter.adapt(player.getLocation());
        RegionManager regionManager = WorldGuardPlugin.inst().getRegionManager(player.getWorld());
        if (regionManager != null) {
            ApplicableRegionSet regions = regionManager.getApplicableRegions(wgLocation);
            for (ProtectedRegion region : regions) {
                if (region.getId().equalsIgnoreCase("coke_location")) { // Vervang "coke_location" door jouw region-naam
                    return true;
                }
            }
        }
        return false;
    }

    public class CokeProcessingTrait extends Trait {
        public CokeProcessingTrait() {
            super("CokeProcessor");
        }

        @Override
        public void onRightClick(org.bukkit.entity.Entity entity, Player player) {
            convertCocaineLeaves(player);
        }
    }

    public class CokeDealerTrait extends Trait {
        public CokeDealerTrait() {
            super("CokeDealer");
        }

        @Override
        public void onRightClick(org.bukkit.entity.Entity entity, Player player) {
            sellCokePowder(player);
        }
    }

    public void setupNPC() {
        NPC processorNPC = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, "CokeProcessor");
        processorNPC.addTrait(CokeProcessingTrait.class);
        processorNPC.spawn(new Location(Bukkit.getWorld("world"), 0, 100, 0)); // Example location

        NPC dealerNPC = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, "CokeDealer");
        dealerNPC.addTrait(CokeDealerTrait.class);
        dealerNPC.spawn(new Location(Bukkit.getWorld("world"), 10, 100, 0)); // Example location
    }
}
