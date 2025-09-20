package org.baicaizhale.litemotto;

import java.util.LinkedList;
import java.util.List;

public class RecentMottoManager {
    private final int maxSize;
    private final LinkedList<String> recentMottos = new LinkedList<>();

    public RecentMottoManager(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized void addMotto(String motto) {
        if (motto == null || motto.isEmpty()) return;
        if (recentMottos.contains(motto)) return;
        recentMottos.addFirst(motto);
        if (recentMottos.size() > maxSize) {
            recentMottos.removeLast();
        }
    }

    public synchronized List<String> getRecentMottos() {
        return new LinkedList<>(recentMottos);
    }
}

