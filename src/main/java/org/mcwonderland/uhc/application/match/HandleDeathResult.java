package org.mcwonderland.uhc.application.match;

public final class HandleDeathResult<T> {
    private final T winner;

    private HandleDeathResult(T winner) {
        this.winner = winner;
    }

    public static <T> HandleDeathResult<T> winner(T winner) {
        if (winner == null)
            throw new IllegalArgumentException("winner cannot be null.");

        return new HandleDeathResult<>(winner);
    }

    public static <T> HandleDeathResult<T> noWinner() {
        return new HandleDeathResult<>(null);
    }

    public boolean hasWinner() {
        return winner != null;
    }

    public T getWinner() {
        return winner;
    }
}
