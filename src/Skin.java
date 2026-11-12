/** Goat coats, unlocked by reaching a best altitude. */
public enum Skin {
    CLASSIC("CLASSIC", 0),
    SCARF("RED SCARF", 100),
    SHADOW("SHADOW", 250),
    GOLDEN("GOLDEN", 500);

    public final String label;
    public final int unlockAltitude;

    Skin(String label, int unlockAltitude) {
        this.label = label;
        this.unlockAltitude = unlockAltitude;
    }

    public boolean unlocked() {
        return Save.bestAltitude() >= unlockAltitude;
    }

    public Skin next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Skin previous() {
        return values()[(ordinal() + values().length - 1) % values().length];
    }

    /** ARGB colour for a goat sprite code, or 0 for transparent. */
    public int color(char code) {
        return switch (this) {
            case SHADOW -> switch (code) {
                case 'M' -> 0xff6a7182;
                case 'W' -> 0xff444a58;
                case 'L' -> 0xff323743;
                case 'G' -> 0xff272a33;
                case 'K' -> 0xff0c0d11;
                case 'H' -> 0xffe2dac2; // pale horns
                case 'E' -> 0xffffd23f; // glowing eye
                case 'N' -> 0xff101014;
                case 'B' -> 0xff08080a;
                default -> 0;
            };
            case GOLDEN -> switch (code) {
                case 'M' -> 0xfffff6c2;
                case 'W' -> 0xffffd65a;
                case 'L' -> 0xffe5a836;
                case 'G' -> 0xffbd8424;
                case 'K' -> 0xff6b4210;
                case 'H' -> 0xff8a5a12;
                case 'E' -> 0xff2a1a06;
                case 'N' -> 0xff4a2e0a;
                case 'B' -> 0xff5a380c;
                default -> 0;
            };
            default -> switch (code) {
                case 'M' -> 0xffffffff; // sunlit fur on the hump
                case 'W' -> 0xfff4f3e8; // shaggy cream-white coat
                case 'L' -> 0xffd3d2c4; // coat shade
                case 'G' -> 0xffa8a99f; // leg shade
                case 'K' -> 0xff3a3d45; // outline
                case 'H' -> 0xff1e1b1a; // horns
                case 'E' -> 0xff0e0e12; // eye
                case 'N' -> 0xff26262b; // nose
                case 'B' -> 0xff141417; // hooves
                default -> 0;
            };
        };
    }
}
