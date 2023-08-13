package io.github.stonley890.eyeofonyx.files;

public enum ChallengeType {

    UNKNOWN,

    /**
     * One teams has sixty seconds to find a spot to hide. The opposite team is given five minutes to find as many of them as possible. Repeat with opposite roles. The team to find the most people in the shortest amount of time wins.
     * @- Players per Party: 1-4
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: N/A
     */
    HIDE_AND_SEEK,

    /**
     * Players choose between prepared kits or their own gear. The team who defeats their opponent and remains the last one standing wins.
     * @- Players per Party: 1-4
     * @- Round Time Limit: 10:00
     * @- Items To Bring: (optional) Combat equipment
     * @- Modes: Best of 1, Best of 3, Best of 5
     */
    PVP,

    /**
     * A procedurally generated maze that the players must solve. The first player to reach the end of the maze wins.
     * @- Player per Party: 1
     * @- Round Time Limit: 10:00
     * @- Items To Bring: N/A
     * @- Modes: N/A
     */
    LABYRINTH_RACE,

    /**
     * Players compete in an archery contest. The player with the highest score or closest to the bullseye wins.
     * @- Players per Party: 1
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: Stationary, Moving targets
     */
    ARCHERY,

    /**
     * A procedurally generated parkour course with various obstacles and jumps. The player who completes the course in the shortest amount of time wins.
     * @- Players per Party: 1
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: N/A
     */
    PARKOUR,

    /**
     * Several items worth varying numbers of points are scattered around an enclosed area. The players must search and collect as many items as possible within a given time limit. The player with the most points wins.
     * @- Players per Party: 1-4
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: N/A
     */
    TREASURE_HUNT,

    /**
     * Players compete to break blocks from under each other. The last player remaining on solid ground wins the round, and the player with the most wins overall is the winner.
     * @- Players per Party: 1-10
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: One Layer, Three Layers
     */
    SPLEEF,

    /**
     * Monsters spawn randomly in an enclosed area and players have a set amount of time to eliminate as many mobs as possible. The player with the highest number of kills wins.
     * @- Players per Party: 1-4
     * @- Round Time Limit 5:00
     * @- Items To Bring: (Optional) Combat equipment
     * @- Modes: Kit, BYOG
     */
    MOB_HUNT,

    /**
     * Players must navigate boats over water and ice around obstacles and reach the finish line. The first player to complete the course wins.
     * @- Players per Party: 1
     * @- Round Time Limit: 5:00
     * @- Items To Bring: N/A
     * @- Modes: N/A
     */
    BOAT_RACE,

    /**
     * Players fly through a guided elytra course. The first player to reach the end wins.
     * @- Players per Party: 1
     * @- Round Time Limit: 5:00
     * @- Items To Bring: Elytra
     * @- Modes: N/A
     */
    ELYTRA_RACE;

}
