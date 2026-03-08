package me.dw1e.kbm.gui;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.KB;
import me.dw1e.kbm.gui.impl.KBListGui;
import me.dw1e.kbm.gui.impl.MainGui;
import me.dw1e.kbm.gui.impl.TypeGui;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiManager {

    private final Map<KB, Gui> typeGuis = new ConcurrentHashMap<>();
    private final Set<Gui> guis = new HashSet<>();

    private Gui mainGui, kbListGui;

    public void enable() {
        guis.add(mainGui = new MainGui());
        guis.add(kbListGui = new KBListGui());

        for (KB kb : KnockbackManager.getInstance().getKBLoader().getKbMap().values()) {
            typeGuis.put(kb, new TypeGui(kb));
        }

        guis.addAll(typeGuis.values());
    }

    public void disable() {
        guis.forEach(gui -> new ArrayList<>(gui.getInventory().getViewers()).forEach(HumanEntity::closeInventory));
        guis.clear();

        typeGuis.clear();

        mainGui = null;
    }

    public Gui getMainGui() {
        return mainGui;
    }

    public Gui getKbListGui() {
        return kbListGui;
    }

    public Gui getTypeGui(KB kb) {
        return typeGuis.get(kb);
    }
}