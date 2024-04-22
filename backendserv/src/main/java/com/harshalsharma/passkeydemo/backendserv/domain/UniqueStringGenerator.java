package com.harshalsharma.passkeydemo.backendserv.domain;

import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.UUID;

public class UniqueStringGenerator {

    private static final String[] ADJECTIVES = {"happy", "blissful", "mysterious", "curious", "friendly", "sunny", "lively", "sparkling", "vibrant", "enigmatic"};
    private static final String[] NOUNS = {"dijkstra", "einstein", "tesla", "newton", "galileo", "darwin", "curie", "shannon", "turing", "boole",
            "banana", "potato", "penguin", "unicorn", "ninja", "cookie", "squirrel", "dragon", "robot", "pirate", "zombie", "toaster", "walrus",
            "noodle", "pickle", "sock", "hamster", "wizard", "shark", "llama"};
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
}
