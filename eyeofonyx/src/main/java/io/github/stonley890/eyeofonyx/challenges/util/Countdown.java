package io.github.stonley890.eyeofonyx.challenges.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Countdown {

    public static List<Countdown> countdowns = new ArrayList<>();


    public int time;
    public boolean running;
    public boolean shown;
    public List<Player> players;

    /**
     * Create a new bossbar countdown
     * @param startTime The number of seconds the countdown will last.
     * @param startNow Whether to start the countdown immediately.
     * @param show Whether to show the bossbar.
     * @param playersToShow The players to show the countdown to.
     */
    public Countdown(int startTime, boolean startNow, boolean show, List<Player> playersToShow) {
        time = startTime;
        running = startNow;
        shown = show;
        players = playersToShow;

        Runnable count = new Runnable() {
            @Override
            public void run() {
                time -= 1;
                try {
                    wait(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void create() {
        countdowns.add(this);
    }

}
