package com.harshalsharma.passkeydemo.backendserv.domain;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

public class UniqueStringGenerator {

    private static final String[] ADJECTIVES = {"happy", "blissful", "mysterious", "curious", "friendly", "sunny", "lively", "sparkling", "vibrant", "funny"};
    private static final String[] NOUNS = {"dijkstra", "einstein", "tesla", "newton", "galileo", "darwin", "curie", "turing",
            "banana", "potato", "penguin", "unicorn", "ninja", "cookie", "squirrel", "dragon", "robot", "pirate",
            "noodle", "pickle", "hamster", "wizard"};
    private static final Random RANDOM = new Random();

    public static synchronized String generateRandomName() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];

        return adjective + "_" + noun + "_" + RANDOM.nextInt(99);
    }

    @NotNull
    public static synchronized String generateUUIDString() {
        return UUID.randomUUID().toString();
    }

    @NotNull
    public static String generateChallenge() {
        return Base64.encodeBase64String(generateUUIDString().getBytes());
    }
}
