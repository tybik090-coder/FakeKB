package dev.decmopl.fakekb.game;

public enum GamePhase {
    IDLE("Лобби"),
    STARTING("Создание мира"),
    CAGE("Кабинки"),
    PHASE_1("Фаза 1 • PvP"),
    PHASE_2("Фаза 2 • Майнинг"),
    PHASE_3("Фаза 3 • Барьер"),
    PHASE_4_CAGE("Финал • Кабинки"),
    PHASE_4("Фаза 4 • Финал"),
    ENDING("Завершение");

    private final String displayName;

    GamePhase(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean running() {
        return this != IDLE;
    }

    public boolean cagesLocked() {
        return this == CAGE || this == PHASE_4_CAGE;
    }

    public boolean pvp() {
        return this == PHASE_1 || this == PHASE_2 || this == PHASE_3 || this == PHASE_4;
    }
}
