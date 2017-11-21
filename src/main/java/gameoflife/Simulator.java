package gameoflife;

import static com.intendia.rxgwt2.client.RxGwt.retryDelay;
import static com.intendia.rxgwt2.elemento.RxElemento.fromEvent;
import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.window;
import static io.reactivex.BackpressureStrategy.DROP;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jboss.gwt.elemento.core.Elements.a;
import static org.jboss.gwt.elemento.core.Elements.body;
import static org.jboss.gwt.elemento.core.Elements.button;
import static org.jboss.gwt.elemento.core.Elements.div;
import static org.jboss.gwt.elemento.core.EventType.click;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import gameoflife.GameOfLife.Board;
import gameoflife.GameOfLife.XY;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import jsinterop.base.JsPropertyMap;
import jsinterop.base.JsPropertyMapOfAny;
import org.jboss.gwt.elemento.core.Elements;

public class Simulator implements EntryPoint {
    public static final Theme THEME = GWT.create(Theme.class);
    public static final Style CSS = THEME.style();

    private HTMLDivElement board;
    private HTMLButtonElement clear;
    private HTMLButtonElement next;
    private HTMLButtonElement play;

    @Override public void onModuleLoad() {
        CSS.ensureInjected();
        body().add(board = div().css(CSS.game()).get());
        body().add(div().css(CSS.controls())
                .add(clear = button("Clear").get())
                .add(next = button("Next").get())
                .add(play = button("Play").get()));
        body().add(div().css(CSS.fork()).add(a("https://github.com/ibaca/gameoflife").add("Fork me on GitHub")));

        Map<XY, HTMLElement> cells = new HashMap<>();
        double yMax = window.innerWidth / 20, xMax = window.innerHeight / 20;
        for (int x = 0; x < xMax; x++) {
            for (int y = 0; y < yMax; y++) {
                XY xy = new XY(x, y);
                int size = 20 - 4, top = x * (size + 4), left = y * (size + 4);
                HTMLDivElement el = div().style(""
                        + "width: " + size + "px; "
                        + "height: " + size + "px; "
                        + "top: " + top + "px; "
                        + "left: " + left + "px").get();
                JsPropertyMap.of(el).set("__xy", xy);
                board.appendChild(el);
                cells.put(xy, el);
            }
        }

        Flowable<Observable<?>> playMode = Flowable.just(
                Observable.never().doOnSubscribe(s -> play.textContent = "Play"),
                Observable.interval(1000, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x1"),
                Observable.interval(1000 / 2, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x2"),
                Observable.interval(1000 / 4, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x3"))
                .repeat().doOnNext(n -> console.log(n));

        Function<Board, Board> tick = board -> board.push(GameOfLife::rule);
        Observable.<Function<Board, Board>>empty()
                .mergeWith(fromEvent(clear, click).map(ev -> board -> new Board(Stream.empty())))
                .mergeWith(fromEvent(next, click).map(ev -> tick))
                .mergeWith(fromEvent(play, click).map(e -> TRUE).startWith(TRUE).toFlowable(DROP)
                        .zipWith(playMode, (e, mode) -> mode, false, 1).toObservable()
                        .switchMap(o -> o.map(n -> tick), 1))
                .mergeWith(fromEvent(board, click)
                        .flatMapMaybe(el -> {
                            if (!(el.target instanceof HTMLElement)) return Maybe.empty();
                            JsPropertyMapOfAny ds = JsPropertyMap.of(el.target);
                            if (!ds.has("__xy")) return Maybe.empty();
                            return Maybe.just(ds.getAny("__xy").<XY>cast());
                        })
                        .map(xy -> board -> board.toggle(xy)))
                .scan(Board.of(), (state, change) -> change.apply(state))
                .doOnNext(state -> {
                    Elements.iterator(board.querySelectorAll("." + CSS.alive()))
                            .forEachRemaining(e -> e.classList.remove(CSS.alive()));
                    state.alive.forEach(xy -> {
                        @Nullable HTMLElement el = cells.get(xy);
                        if (el != null) el.classList.add(CSS.alive());
                    });
                })
                .ignoreElements()
                .compose(retryDelay(attempt -> GWT.log("ups… something is going wrong", attempt.err)))
                .subscribe();

    }

    interface Theme extends ClientBundle {
        @Source("style.gss") Style style();
    }

    interface Style extends CssResource {
        String alive();
        String controls();
        String fork();
        String game();
    }
}
