package org.mcwonderland.uhc.util;

public final class TimePlaceholderFormatter {
    private static final String TIME_PLACEHOLDER = "{time}";
    private static final String FANCY_TIME_PLACEHOLDER = "{fancy-time}";
    private static final String SECONDS_PLACEHOLDER = "{seconds}";
    private static final String SECOND_PLACEHOLDER = "{second}";

    private static String secondSymbol = "second";
    private static String secondsSymbol = "seconds";
    private static String minuteSymbol = "minute";
    private static String minutesSymbol = "minutes ";

    private TimePlaceholderFormatter() {
    }

    public static void configureSymbols(String second, String seconds, String minute, String minutes) {
        secondSymbol = symbolOrDefault(second, "second");
        secondsSymbol = symbolOrDefault(seconds, "seconds");
        minuteSymbol = symbolOrDefault(minute, "minute");
        minutesSymbol = symbolOrDefault(minutes, "minutes ");
    }

    public static Object[] replacementPairs(Number seconds) {
        Number rawSeconds = seconds == null ? 0 : seconds;

        return new Object[]{
                TIME_PLACEHOLDER, clockTime(seconds),
                FANCY_TIME_PLACEHOLDER, fancyTime(seconds),
                SECONDS_PLACEHOLDER, rawSeconds,
                SECOND_PLACEHOLDER, rawSeconds
        };
    }

    public static String clockTime(Number seconds) {
        int duration = Math.max(0, toWholeSeconds(seconds));

        if (duration >= 3600)
            return formatHours(duration);

        return formatMinutes(duration);
    }

    public static String fancyTime(Number seconds) {
        int duration = toWholeSeconds(seconds);
        int minutes = duration / 60;
        int remainingSeconds = duration % 60;
        String secondWord = remainingSeconds > 1 ? secondsSymbol : secondSymbol;
        String minuteWord = minutes > 1 ? minutesSymbol : minuteSymbol;

        if (minutes == 0)
            return remainingSeconds + secondWord;
        if (remainingSeconds == 0)
            return minutes + minuteWord;

        return minutes + minuteWord + remainingSeconds + secondWord;
    }

    private static int toWholeSeconds(Number seconds) {
        if (seconds == null)
            return 0;

        return seconds.intValue();
    }

    private static String formatHours(int duration) {
        int hours = duration / 3600;
        int remainingSeconds = duration % 3600;

        return hours + ":" + formatMinutes(remainingSeconds);
    }

    private static String formatMinutes(int duration) {
        int minutes = duration / 60;
        int seconds = duration % 60;

        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private static String twoDigits(int number) {
        return (number < 10 ? "0" : "") + number;
    }

    private static String symbolOrDefault(String symbol, String defaultSymbol) {
        return symbol == null ? defaultSymbol : symbol;
    }
}
