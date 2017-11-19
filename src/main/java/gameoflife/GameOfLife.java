package gameoflife;

import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class GameOfLife {

    public static boolean rule(boolean alive, int neighborhoods) {
        return neighborhoods == 3 || neighborhoods == 2 && alive;
    }

    public static class Board {

        public static Board of(XY... alive) { return new Board(Stream.of(alive)); }

        public static Stream<XY> expand(XY p) {
            return range(p.x - 1, p.x + 2).mapToObj(x -> range(p.y - 1, p.y + 2)
                    .mapToObj(y -> new XY(x, y))).flatMap(identity());
        }

        public final Set<XY> alive;

        public Board(Stream<XY> alive) { this.alive = unmodifiableSet(alive.collect(toSet())); }

        boolean isAlive(XY p) { return alive.contains(p); }

        boolean isDeath(XY p) { return !isAlive(p); }

        public int neighborhoods(XY p) {
            return (int) expand(p).filter(n -> !n.equals(p)).filter(this::isAlive).count();
        }

        public Board push(BiPredicate<Boolean, Integer> rule) {
            return new Board(alive.stream().flatMap(Board::expand).distinct()
                    .filter(p -> rule.test(isAlive(p), neighborhoods(p))));
        }
    }

    public static class XY implements Comparable<XY> {
        public final int x, y;
        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
        @Override public int compareTo(XY that) {
            int compare = Integer.compare(this.x, that.x);
            if (compare == 0) compare = Integer.compare(this.y, that.y);
            return compare;
        }
        @Override public boolean equals(Object o) {
            return this == o || o != null && getClass() == o.getClass() && equals((XY) o);
        }
        private boolean equals(XY xy) { return x == xy.x && y == xy.y; }
        @Override public int hashCode() { return x * 31 + y; }
        @Override public String toString() { return x + ":" + y; }
    }
}
