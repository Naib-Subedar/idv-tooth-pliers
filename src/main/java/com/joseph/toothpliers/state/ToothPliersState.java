package com.joseph.toothpliers.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToothPliersState {
    private static final Map<UUID, SkillState> STATES = new HashMap<>();

    public static SkillState get(UUID player) {
        return STATES.computeIfAbsent(player, k -> new SkillState());
    }

    public static void remove(UUID player) {
        STATES.remove(player);
    }
}