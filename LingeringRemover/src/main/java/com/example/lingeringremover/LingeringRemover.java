package com.example.lingeringremover;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class LingeringRemover extends JavaPlugin implements Listener, CommandExecutor {

    private final String GUI_TITLE = ChatColor.DARK_AQUA + "Potion Filter Settings";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("potionremove").setExecutor(this);
        getLogger().info("Inventory Potion Remover with GUI enabled!");
    }

    private void clearTargetPotions(Player player) {
        boolean allowDrink = getConfig().getBoolean("allow-drinkable-potions", true);
        boolean allowSplash = getConfig().getBoolean("allow-splash-potions", true);
        boolean allowLingering = getConfig().getBoolean("allow-lingering-potions", false);

        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;

            Material type = item.getType();

            if (!allowLingering && type == Material.LINGERING_POTION) {
                player.getInventory().setItem(i, null);
            } else if (!allowSplash && type == Material.SPLASH_POTION) {
                player.getInventory().setItem(i, null);
            } else if (!allowDrink && type == Material.POTION) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    // --- GUI CREATION ---

    public void openSettingsGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, GUI_TITLE);

        gui.setItem(2, createGuiItem(Material.POTION, ChatColor.BLUE + "Standard Potions", 
                getConfig().getBoolean("allow-drinkable-potions", true)));
        
        gui.setItem(4, createGuiItem(Material.SPLASH_POTION, ChatColor.LIGHT_PURPLE + "Splash Potions", 
                getConfig().getBoolean("allow-splash-potions", true)));
        
        gui.setItem(6, createGuiItem(Material.LINGERING_POTION, ChatColor.DARK_PURPLE + "Lingering Potions", 
                getConfig().getBoolean("allow-lingering-potions", false)));

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name, boolean isAllowed) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Status: " + (isAllowed ? ChatColor.GREEN + "ALLOWED" : ChatColor.RED + "BANNED"),
                ChatColor.YELLOW + "Click to toggle status!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- COMMAND HANDLING ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (!player.hasPermission("lingeringremover.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            openSettingsGUI(player);
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /potionremove gui");
        return true;
    }

    // --- EVENTS ---

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true); // Stop players from taking items out of the GUI

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        Material type = clicked.getType();

        if (type == Material.POTION) {
            boolean current = getConfig().getBoolean("allow-drinkable-potions", true);
            getConfig().set("allow-drinkable-potions", !current);
        } else if (type == Material.SPLASH_POTION) {
            boolean current = getConfig().getBoolean("allow-splash-potions", true);
            getConfig().set("allow-splash-potions", !current);
        } else if (type == Material.LINGERING_POTION) {
            boolean current = getConfig().getBoolean("allow-lingering-potions", false);
            getConfig().set("allow-lingering-potions", !current);
        }

        saveConfig(); // Persist changes to config.yml
        openSettingsGUI(player); // Refresh inventory screen to update lore indicators
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        clearTargetPotions(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(GUI_TITLE)) return;
        if (event.getWhoClicked() instanceof Player) {
            getServer().getScheduler().runTask(this, () -> {
                clearTargetPotions((Player) event.getWhoClicked());
            });
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Material pickedType = event.getItem().getItemStack().getType();

            boolean allowDrink = getConfig().getBoolean("allow-drinkable-potions", true);
            boolean allowSplash = getConfig().getBoolean("allow-splash-potions", true);
            boolean allowLingering = getConfig().getBoolean("allow-lingering-potions", false);

            if ((!allowLingering && pickedType == Material.LINGERING_POTION) ||
                (!allowSplash && pickedType == Material.SPLASH_POTION) ||
                (!allowDrink && pickedType == Material.POTION)) {
                
                event.setCancelled(true);
                event.getItem().remove();
            }
        }
    }
}