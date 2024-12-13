/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.autoResolve.helper;

import org.apache.commons.lang3.stream.Streams;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Luana Coppio
 */
public class RandomUtils {

    private static final Collector<?, ?, ?> SHUFFLER = Collectors.collectingAndThen(
        Collectors.toCollection(ArrayList::new),
        list -> {
            Collections.shuffle(list);
            return list;
        }
    );

    /**
     * Returns a {@link Collector} that accumulates the input elements into a new {@code List} in a shuffled order.
     * Perfect to use as a termination operation in a stream pipeline.
     * @return a {@code Collector} which collects all the input elements into a {@code List}, then shuffles it.
     */
    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toShuffledList() {
        return (Collector<T, ?, List<T>>) SHUFFLER;
    }

    private static final Random random = new Random();

    /**
     * Returns an {@link Optional} describing some element of the array, or an empty {@code Optional} if the array is empty.
     * @param elements the array to sample from
     * @return an {@code Optional} describing some element of the array, or an empty {@code Optional} if the array is empty
     */
    @SafeVarargs
    public static <T> Optional<T> sample(T... elements) {
        return Streams.of(elements).collect(toShuffledList()).stream().findFirst();
    }

    /**
     * Returns an {@link Optional} describing some element of the list, or an empty {@code Optional} if the list is empty.
     * @param list the list to sample from
     * @return an {@code Optional} describing some element of the list, or an empty {@code Optional} if the list is empty
     */
    public static <T> Optional<T> fastSample(List<T> list) {
        var size = list.size();
        if (size == 0) {
            return Optional.empty();
        }
        return Optional.of(list.get(random.nextInt(size)));
    }

    /**
     * Returns an {@link Optional} describing some element of the list, or an empty {@code Optional} if the list is empty.
     * @param list the list to sample from
     * @return an {@code Optional} describing some element of the list, or an empty {@code Optional} if the list is empty
     */
    public static <T> Optional<T> sample(List<T> list) {
        return list.stream().collect(toShuffledList()).stream().findFirst();
    }

    /**
     * Returns a list of random elements from the input list.
     * @param list the list to sample from
     * @return a single element from the list, unboxed
     */
    public static <T> T sampleUnchecked(List<T> list) {
        return list.stream().collect(toShuffledList()).stream().findFirst().orElseThrow();
    }

    /**
     * Returns a list of random elements from the input list.
     * @param list the list to sample from
     * @param count the number of elements to sample, without repetition
     * @return a single element from the list, unboxed
     */
    public static <T> List<T> sampleUnchecked(List<T> list, int count) {
        return list.stream().collect(toShuffledList()).stream().limit(count).toList();
    }

    public static void main(String[] args) {
        List<String> list = List.of("a", "b", "c", "d", "e");
        System.out.println(sample(list).orElseThrow());
    }
}
