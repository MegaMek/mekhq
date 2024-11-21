package mekhq.campaign.autoResolve.helper;

import org.apache.commons.lang3.stream.Streams;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RandomUtils {

    private static final Collector<?, ?, ?> SHUFFLER = Collectors.collectingAndThen(
        Collectors.toCollection(ArrayList::new),
        list -> {
            Collections.shuffle(list);
            return list;
        }
    );

    @SuppressWarnings("unchecked")
    public static <T> Collector<T, ?, List<T>> toShuffledList() {
        return (Collector<T, ?, List<T>>) SHUFFLER;
    }


    @SafeVarargs
    public static <T> Optional<T> sample(T... elements) {
        return Streams.of(elements).collect(toShuffledList()).stream().findFirst();
    }

    public static <T> Optional<T> sample(List<T> list) {
        return list.stream().collect(toShuffledList()).stream().findFirst();
    }

    public static class WeightedList<T> {
        private final List<WeightedEntry<T>> entries = new ArrayList<>();
        private double totalWeight = 0.0;
        private static final Random random = new Random();


        public WeightedList() {}

        WeightedList(Object... input) {
            if ((input.length & 1) != 0) { // implicit nullcheck of input
                throw new InternalError("length is odd");
            }
            for (int i = 0; i < input.length; i += 2) {
                @SuppressWarnings("unchecked")
                T k = Objects.requireNonNull((T)input[i]);
                double v = (double) input[i+1];
                var alreadyPresent = entries.stream().anyMatch(entry -> entry.value().equals(k));
                if (alreadyPresent) {
                    throw new IllegalArgumentException("duplicate value: " + k);
                }
                this.addEntry(k, v);
            }
        }

        public void addEntry(T value, double weight) {
            entries.add(new WeightedEntry<>(value, weight));
            totalWeight += weight;
        }

        public Optional<T> sample() {
            double randomValue = random.nextDouble() * totalWeight;
            double cumulativeWeight = 0.0;

            for (WeightedEntry<T> entry : entries) {
                cumulativeWeight += entry.weight();
                if (randomValue <= cumulativeWeight) {
                    return Optional.of(entry.value());
                }
            }

            return Optional.empty();
        }

        public List<T> values() {
            return entries.stream().map(WeightedEntry::value).toList();
        }

        public double getWeight(T value) {
            return entries.stream()
                .filter(entry -> entry.value().equals(value))
                .map(WeightedEntry::weight)
                .findFirst()
                .orElse(0.0);
        }

        public double getPercentage(T value) {
            return (getWeight(value) / totalWeight) * 100;
        }

        private record WeightedEntry<T>(T value, double weight) { }

        public static <T> WeightedList<T> of(T k1, double v1) {
            return new WeightedList<>(k1, v1);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2) {
            return new WeightedList<>(k1, v1, k2, v2);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5, T k6, double v6) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5, T k6, double v6, T k7, double v7) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5, T k6, double v6, T k7, double v7, T k8, double v8) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5, T k6, double v6, T k7, double v7, T k8, double v8, T k9, double v9) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
        }

        public static <T> WeightedList<T> of(T k1, double v1, T k2, double v2, T k3, double v3, T k4, double v4, T k5, double v5, T k6, double v6, T k7, double v7, T k8, double v8, T k9, double v9, T k10, double v10) {
            return new WeightedList<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
        }
    }

    public static void main(String[] args) {
        List<String> list = List.of("a", "b", "c", "d", "e");
        System.out.println(sample(list).orElseThrow());

        var sampled = new WeightedList<String>();
        sampled.addEntry("a", 0.1);
        sampled.addEntry("b", 0.2);
        sampled.addEntry("c", 0.3);
        sampled.addEntry("d", 0.4);
        sampled.addEntry("e", 0.5);
        for (int i=0; i < 10; i++) {
            var ret = sampled.sample().orElseThrow();
            System.out.println(ret + " " + sampled.getPercentage(ret));
        }
    }
}
