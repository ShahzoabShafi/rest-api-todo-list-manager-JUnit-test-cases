package api;

import java.util.Random;

final class RandomTodoData {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz ";
    private static final long SEED = 429L;
    private static final Random RANDOM = new Random(SEED);

    private RandomTodoData() {
    }

    static String title() {
        return "t" + text(4, 24);
    }

    static String description() {
        return text(0, 60);
    }

    static boolean doneStatus() {
        return RANDOM.nextBoolean();
    }

    static String todoPayload() {
        return Payloads.todo(title(), doneStatus(), description());
    }

    private static String text(int minLength, int maxLength) {
        int length = minLength + RANDOM.nextInt(maxLength - minLength + 1);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return text.toString();
    }
}
