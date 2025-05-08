package org.example.utils;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface QuadFunction<A,B,C,D,R> {

    R apply(A a, B b, C c, D d);

    /**
     * QuadFunction interface based on this StackOverflow answer:
     * https://stackoverflow.com/questions/18400210/java-8-where-is-trifunction-and-kin-in-java-util-function-or-what-is-the-alt
     */
    default <V> QuadFunction<A, B, C, D, V> andThen(
                                Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c, D d) -> after.apply(apply(a, b, c, d));
    }
}
