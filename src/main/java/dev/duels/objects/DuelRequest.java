    package dev.duels.objects;

    import java.util.UUID;

    public class DuelRequest {

        private final UUID sender;
        private final UUID target;
        private final String kitName;
        private final String arenaName;
        private final DuelSession.MatchMode matchMode;
        private final int matchValue;
        private final long timestamp;

        public DuelRequest(UUID sender, UUID target, String kitName, String arenaName, int matchValue) {
            this(sender, target, kitName, arenaName, DuelSession.MatchMode.FIRST_TO, matchValue);
        }

        public DuelRequest(UUID sender, UUID target, String kitName, String arenaName,
                           DuelSession.MatchMode matchMode, int matchValue) {
            this.sender = sender;
            this.target = target;
            this.kitName = kitName;
            this.arenaName = arenaName;
            this.matchMode = matchMode == null ? DuelSession.MatchMode.FIRST_TO : matchMode;
            this.matchValue = Math.max(1, matchValue);
            this.timestamp = System.currentTimeMillis();
        }




        public UUID getSender() { return sender; }
        public UUID getTarget() { return target; }
        public String getKitName() { return kitName; }
        public DuelSession.MatchMode getMatchMode() { return matchMode; }
        public int getMatchValue() { return matchValue; }
        public int getBestOf() { return matchValue; }
        public long getTimestamp() { return timestamp; }
        public String getArenaName() {return arenaName; }
        public String getMatchDescription() {
            return matchMode == DuelSession.MatchMode.BEST_OF ? "Best of " + matchValue : "First to " + matchValue;
        }

        public boolean isExpired(int timeoutSeconds) {
            return System.currentTimeMillis() - timestamp > (timeoutSeconds * 1000L);
        }
    }
