package gameoflife;

import static gameoflife.GameOfLife.rule;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import gameoflife.GameOfLife.Board;
import gameoflife.GameOfLife.XY;
import org.junit.Test;

public class GameOfLifeTest {

    @Test public void rule_works() throws Exception {
        assertThat(rule(true, 1), equalTo(false/*death*/));
        assertThat(rule(true, 2), equalTo(true/*alive*/));
        assertThat(rule(true, 3), equalTo(true/*alive*/));
        assertThat(rule(true, 4), equalTo(false/*death*/));

        assertThat(rule(false, 1), equalTo(false/*death*/));
        assertThat(rule(false, 2), equalTo(false/*death*/));
        assertThat(rule(false, 3), equalTo(true/*alive*/));
        assertThat(rule(false, 4), equalTo(false/*death*/));
    }

    @Test public void expand_works() {
        assertThat(Board.expand(new XY(1, 1)).collect(toList()), equalTo(asList(
                new XY(0, 0), new XY(0, 1), new XY(0, 2),
                new XY(1, 0), new XY(1, 1), new XY(1, 2),
                new XY(2, 0), new XY(2, 1), new XY(2, 2))));
    }

    @Test public void board_isAlive_works() {
        assertThat(Board.of(new XY(1, 1)).isAlive(new XY(1, 1)), equalTo(true));
        assertThat(Board.of(new XY(1, 1)).isAlive(new XY(0, 0)), equalTo(false));
        assertThat(Board.of(new XY(1, 1)).isDeath(new XY(1, 1)), equalTo(false));
        assertThat(Board.of(new XY(1, 1)).isDeath(new XY(0, 0)), equalTo(true));
    }

    @Test public void board_neighborhoods_works() {
        Board board = Board.of(new XY(1, 1), new XY(1, 2));
        assertThat(board.neighborhoods(new XY(1, 1)), equalTo(1));
        assertThat(board.neighborhoods(new XY(2, 1)), equalTo(2));
        assertThat(board.neighborhoods(new XY(3, 1)), equalTo(0));
    }

    @Test public void board_push_works() {
        assertThat(Board.of(new XY(1, 1)).push(GameOfLife::rule).alive.size(), equalTo(0));
        assertThat(Board.of(new XY(1, 1), new XY(1, 2)).push(GameOfLife::rule).alive.size(), equalTo(0));
        XY[] square = { new XY(1, 1), new XY(1, 2), new XY(2, 1), new XY(2, 2) };
        assertThat(Board.of(square).push(GameOfLife::rule).alive, hasItems(square));
        assertThat(Board.of(new XY(1, 1), new XY(1, 2), new XY(2, 1)).push(GameOfLife::rule).alive, hasItems(square));
    }
}
