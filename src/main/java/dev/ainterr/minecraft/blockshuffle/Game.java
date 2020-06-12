package dev.ainterr.minecraft.blockshuffle;

import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collections;
import java.util.Random;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;


public final class Game extends JavaPlugin {
    public static BlockBlacklist BLACKLIST = new BlockBlacklist();
    public PlayerList players = new PlayerList();

    private boolean running = false;

    public void startRound() {
        for(Player player: Bukkit.getServer().getOnlinePlayers()) {
            this.players.addPlayer(player);

            this.players.newBlock(player);
            String block = this.players.getBlock(player);

            player.sendMessage(
                ChatColor.GREEN
                + player.getName() + " you must find " + block
            );
        }

        getServer().getPluginManager().registerEvents(
            new MovementListener(this), this
        );

        new Countdown(this).runTaskTimer(this, 30*20, 20);

        this.running = true;
    }

    public void endRound() {
        for(Player player: this.players.getPlayers()) {
            if(this.players.getStatus(player) == PlayerList.STATUS_FAILURE) {
                Bukkit.broadcastMessage(
                    ChatColor.RED
                    + player.getName() + " failed to find "
                    + this.players.getBlock(player)
                );
            }
        }

        PlayerMoveEvent.getHandlerList().unregister(this);
        Bukkit.getScheduler().cancelTasks(this);

        this.running = false;
    }

    private void startGame() {
        Bukkit.broadcastMessage("welcome to BlockShuffle");

        this.startRound();
    }

    private void stopGame() {
        this.endRound();

        Bukkit.broadcastMessage("BlockShuffle game over");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("block-shuffle-start")) {
            if(this.running) {
                sender.sendMessage("BlockShuffle has already started");
                return true;
            }

            this.startGame();

            return true;
        }
        else if(command.getName().equalsIgnoreCase("block-shuffle-stop")) {
            if(!this.running) {
                sender.sendMessage("you're not currently playing BlockShuffle");
                return true;
            }

            this.stopGame();

            return true;
        }

        return false;
    }
}

class MovementListener implements Listener {
    private Game plugin;

    public MovementListener(Game plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(final PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if(
            from.getBlockX() == to.getBlockX() 
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()
        ) {
            return;
        }

        Player player = event.getPlayer();

        int status = this.plugin.players.getStatus(player);
        boolean found = this.plugin.players.isBlockFound(player);

        if(found && status != PlayerList.STATUS_SUCCESS) {
            Bukkit.broadcastMessage(
                ChatColor.GOLD
                + player.getName() + " found " + this.plugin.players.getBlock(player)
            );

            if(this.plugin.players.getAllStatus() == PlayerList.STATUS_SUCCESS) {
                this.plugin.endRound();
                this.plugin.startRound();
            }
        }
    }
}


class Countdown extends BukkitRunnable {
    private Game plugin;
    private int countdown;

    public Countdown(Game plugin) {
        this.plugin = plugin;
        this.countdown = 10;
    }

    @Override
    public void run() {
        if(this.countdown > 0) {
            Bukkit.broadcastMessage(
                "BlockShuffle round over in " + countdown-- + " ..."
            );
        }
        else {
            new Shuffle(this.plugin).runTaskLater(this.plugin, 1*20);
            this.cancel();
        }
    }
}

class Shuffle extends BukkitRunnable {
    private Game plugin;

    public Shuffle(Game plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        this.plugin.endRound();
        this.plugin.startRound();
    }
}