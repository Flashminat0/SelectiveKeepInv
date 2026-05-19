package com.flashminat0.selectivekeepinv;

/**
 * Pool of chat message lines shown to players on respawn. Lives in its own
 * file so {@link EventHandler} stays focused on event logic.
 *
 * <p>All fields are {@code public static final String[]}, meant to be picked
 * from at random by the event handler. Each array can be edited / extended
 * independently without touching anything else.
 */
public final class DeathMessages {

    private DeathMessages() {}

    /** No items dropped (ALL mode). Light praise, slight ego boost. */
    public static final String[] ALL_LINES = {
            "The reaper went home empty-handed.",
            "You dropped nothing on your deathbed or whatever.",
            "Walked it off with everything intact. Show-off.",
            "Death is a minor inconvenience for you.",
            "Skill issue (the reaper's, not yours).",
            "Built different. The void respects that.",
            "Even death couldn't take your loot.",
            "Your kit survives where lesser players' don't.",
            "Insurance policy: maxed out.",
            "Game over? Hardly. You barely noticed.",
            "You died with style and nothing else.",
            "Death tried. Death failed. Carry on.",
            "The grave looked at your inventory and gave up.",
            "Untouchable. Even by mortality.",
            "Some people lose their stuff on death. Not you.",
            "The void will have to try harder.",
            "Respawn pristine. As is your right.",
            "Death respawns you, the gear respawns with you.",
            "Shivaxi himself would tip his hat.",
            "Even Shivaxi can't explain how you got out clean.",
            "NLBlackEagle didn't design Dregora for players this lucky.",
            // Hurtful: you still died, scrub.
            "You still died. Sit with that.",
            "Keeping loot doesn't undo the L.",
            "Your stuff is fine. Your skill is not.",
            "Congrats, you respawned a corpse with inventory.",
            "Easy mode engaged. Are you proud?",
            "Even with training wheels, you still fell over.",
            "The mod saved you. Nothing else could have.",
            "Survived death. Pride remains in critical condition.",
            "Your gear has more dignity than you do.",
            "Statistically you should not still be alive. The mod begs to differ."
    };

    /** ALL mode, deathLevel > 0. Extra flavor about the kept XP. */
    public static final String[] ALL_LINES_WITH_XP = {
            "Loot AND levels intact. Disgusting.",
            "Even your XP survived. The audacity.",
            "Kept your gear and your grind. The streamers cry."
    };

    /** ALL mode, deathLevel == 0. Mock for being level zero. */
    public static final String[] ALL_LINES_NO_XP = {
            "Nothing to drop, nothing to lose. Tragic.",
            "Died at level 0. The void found nothing of value.",
            "Bare minimum effort, bare minimum loss."
    };

    /** Same dimension as death. %s is the colored distance. Trolly. */
    public static final String[] SAME_DIM_LINES = {
            "Your funeral procession is %s blocks away.",
            "%s blocks to the scene of the crime.",
            "Your stuff sits %s blocks away, judging you.",
            "Trail of regrets leads %s blocks away.",
            "Hope you packed running shoes. %s blocks to go.",
            "Better start jogging. %s blocks won't run themselves.",
            "Skill issue. %s blocks of penance ahead.",
            "Get up. Your loot's %s blocks that way.",
            "%s blocks. Maybe try not dying next time.",
            "%s blocks of self-reflection coming up.",
            "That was embarrassing. %s blocks until you can pretend it didn't happen.",
            "Walk of shame: %s blocks.",
            "%s blocks. Reflect on your choices.",
            "%s blocks. Earn it back.",
            "Tragedy in three acts: you died, you respawned, you walked %s blocks.",
            "Your stuff is %s blocks away. It is not impressed.",
            "%s blocks. Hope it was worth dying for.",
            "Pack a lunch. %s blocks to your loot.",
            "%s blocks. Walk it off, champion.",
            "Loot's %s blocks that way. Bring tissues.",
            "%s blocks. The march of consequences begins.",
            "Speedrunning death, are we? %s blocks to recover.",
            "%s blocks of pure regret incoming.",
            "Ask Shivaxi nicely for your stuff back. Or walk %s blocks.",
            "Tweet Shivaxi a sad essay. Meanwhile, %s blocks of walking.",
            "Blame NLBlackEagle for the terrain. %s blocks of it ahead.",
            // Hurtful: designed to make a fragile player ragequit.
            "%s blocks. Walk it like a chump.",
            "Get to walking, scrub. %s blocks.",
            "%s blocks. Maybe uninstall this time.",
            "%s blocks of cope ahead. Touch grass.",
            "%s blocks. Try not to die again on the way back.",
            "%s blocks. Each step is your fault.",
            "%s blocks of regret. Earn every one.",
            "%s blocks. Future you is laughing at past you.",
            "%s blocks to the monument of your failure.",
            "%s blocks. Your friends saw the whole thing."
    };

    /** Died in a different dimension than the respawn point. Trolly++. */
    public static final String[] DIFF_DIM_LINES = {
            "Your stuff is in another dimension. Cry about it.",
            "Death came on vacation. Loot stayed home.",
            "Different dimension drop. Hope you packed a portal.",
            "Your stuff and you no longer share a reality. Awkward.",
            "Bold of you to die in another dimension. Loot's stranded.",
            "Different dimension, different problem. Your problem.",
            "Cross-dimensional loot retrieval. Good luck with that.",
            "Your loot's having a better time than you. In another world.",
            "Wrong dimension to die in. The loot says hi.",
            "Dimensional inconvenience: your loot, your problem.",
            "You died abroad. Your stuff stayed home. Best of luck.",
            "Pro tip: don't die in dimensions you can't easily return to.",
            "Your loot has filed for separation. Different reality and everything.",
            "Shivaxi promised difficulty, not delivery. Loot stays.",
            "Maybe Shivaxi will personally fetch it. He won't.",
            "NLBlackEagle made this dimensional mess. Your loot pays the price.",
            // Hurtful: extra brutal because dying abroad deserves it.
            "Loot's gone. Like your dignity. Different dimension.",
            "Imagine dying THERE. Couldn't be me.",
            "Different dimension, same skill issue.",
            "Loot evolved past you. Different reality and all.",
            "Stuff's in another dimension. So is your competence.",
            "You died abroad. Your reputation died first.",
            "Even the dimension didn't want you. The loot stays where it belongs.",
            "Cross-dimensional embarrassment unlocked. Loot left behind.",
            "Different dimension, same scrub. Loot stranded.",
            "Wrong portal, wrong life. Loot's in the right place. You're not."
    };

    /**
     * XP roll flavor: added as a fourth line for DEFAULT mode at level >= 100.
     * Three pools based on how the divisor landed. We never reveal the actual
     * ratio; the player can compare their pre-death vs post-respawn level if
     * they really want the math.
     */
    public static final String[] XP_ROLL_LUCKY = {
            "The XP gods smiled today.",
            "Lucky roll. Most of your XP survived.",
            "RNG was on your side. For once.",
            "The dice rolled your way on the XP.",
            "Generous return. Don't get used to it.",
            "Smooth landing on the XP front.",
            "Cosmic mercy was granted. Briefly.",
            "Your XP barely felt the dying."
    };

    public static final String[] XP_ROLL_MID = {
            "The XP roll was meh.",
            "Half-decent recovery.",
            "Mediocre XP day. Could be worse.",
            "Average roll, average results.",
            "The dice were lukewarm.",
            "Middling XP fortune.",
            "You got some back. Some.",
            "Compromise reached: half and half."
    };

    public static final String[] XP_ROLL_BRUTAL = {
            "Brutal XP roll. Ouch.",
            "The XP roll was tragic.",
            "Barely scraped any XP back.",
            "RNG hated your XP today.",
            "Tough XP luck. Sit with it.",
            "The dice spat in your face.",
            "Pittance returned. Earn the rest.",
            "The reaper kept most of your XP. Rude."
    };
}
