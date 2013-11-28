package info.gomeow.mctag;

import info.gomeow.mctag.util.Equip;
import info.gomeow.mctag.util.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class Match {

    String name;
    ConfigurationSection config;
    Map<String, Integer> players = new HashMap<String, Integer>(); // Name, tags
    State state = State.LOBBY;

    BukkitRunnable startRun;
    boolean starting = false;

    BukkitRunnable endRun;

    int minSize = 2;

    String it = "";
    String lastIt = "";

    Random rand = new Random();

    Location itspawn;
    Location regularspawn;

    boolean safe = false; // TODO Make configurable

    public Match(String n, ConfigurationSection section) {
        name = n;
        config = section;
        itspawn = Manager.getLocation(config.getString("itspawn"));
        regularspawn = Manager.getLocation(config.getString("regularspawn"));
    }

    public String getName() {
        return name;
    }

    public void addPlayer(Player player) {
        players.put(player.getName(), 0);
        broadcast(ChatColor.GOLD + player.getName() + " has joined the match! (" + players.size() + " players in match)");
        if (players.size() >= 2) { // TODO
            countdown();
        }
    }

    public void removePlayer(Player player) {
        int temp = (state == State.INGAME) ? 0 : 1;
        players.remove(player.getName());
        if (players.size() <= minSize) {
            if (startRun != null) {
                startRun.cancel();
            }
            if ((state == State.LOBBY && starting) || (state == State.INGAME)) {
                broadcast(ChatColor.DARK_RED + "Not enough players to continue!");
            }
            starting = false;
        }
        if (temp == 0) {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            removePotionEffects(player);
            player.teleport(MCTag.instance.getManager().getLobby());
            Manager.loadInventory(player);
        }
    }

    public Set<Player> getPlayers() {
        Set<Player> plys = new HashSet<Player>();
        for (String name : players.keySet()) {
            plys.add(Bukkit.getPlayerExact(name));
        }
        return plys;
    }

    public String getIT() {
        return it;
    }

    public void tag(Player tagger, Player tagged) {
        int tags = players.get(tagger.getName());
        tags++;
        players.put(tagger.getName(), tags);
        Equip.equipIt(tagged);
        Equip.equipOther(tagger);
        it = tagged.getName();
        lastIt = tagger.getName();
    }

    public int givePoint(Player player) {
        int current = players.get(player.getName()) + 1;
        players.put(player.getName(), current);
        return current;
    }

    public void broadcast(String message) {
        for (Player player : getPlayers()) {
            player.sendMessage(message);
        }
    }

    public int getSize() {
        return players.size();
    }

    public boolean containsPlayer(Player player) {
        return players.containsKey(player.getName());
    }

    public State getState() {
        return state;
    }

    public void countdown() {
        if (!starting) {
            starting = true;
            startRun = new BukkitRunnable() {

                int time = 21;

                public void run() {
                    if (starting) {
                        time--;
                        if (time <= 0) {
                            startGame();
                            cancel();
                        } else if ((time % 5 == 0) || (time <= 5)) {
                            broadcast(ChatColor.AQUA + "Match starting in " + time + " seconds.");
                        }
                    } else {
                        cancel();
                    }
                }
            };
            startRun.runTaskTimer(MCTag.instance, 0L, 20L);
        }
    }

    public void startGame() {
        state = State.INGAME;
        int item = rand.nextInt(players.size());
        int i = 0;
        for (String name : players.keySet()) {
            if (i == item) {
                it = name;
            }
            i++;
        }
        for (Player player : getPlayers()) {
            Manager.saveInventory(player);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            removePotionEffects(player);
            if (player.getName().equalsIgnoreCase(it)) {
                Equip.equipIt(player);
            } else {
                Equip.equipOther(player);
            }
            player.setHealth(player.getMaxHealth());
        }
        safe = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                safe = false;
            }
        }.runTaskLater(MCTag.instance, 400L); // TODO Make configurable

        endRun = new BukkitRunnable() {

            public void run() {
                endGame();
            }
        };
    }

    public void endGame() {
        // TODO Send ending message
    }

    public void reset() {
        starting = false;
        if (state == State.INGAME) {
            state = State.LOBBY;
            for (Player player : getPlayers()) {
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                removePotionEffects(player);
                player.teleport(MCTag.instance.getManager().getLobby());
                Manager.loadInventory(player);
            }
            players.clear();
            it = null;
        }
    }

    public void removePotionEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

}
