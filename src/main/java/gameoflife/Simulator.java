package gameoflife;

import static com.intendia.rxgwt2.client.RxGwt.retryDelay;
import static com.intendia.rxgwt2.elemento.RxElemento.fromEvent;
import static elemental2.dom.DomGlobal.document;
import static elemental2.dom.DomGlobal.window;
import static io.reactivex.Maybe.fromCallable;
import static io.reactivex.Observable.merge;
import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.function.Function.identity;
import static org.jboss.gwt.elemento.core.Elements.a;
import static org.jboss.gwt.elemento.core.Elements.body;
import static org.jboss.gwt.elemento.core.Elements.button;
import static org.jboss.gwt.elemento.core.Elements.div;
import static org.jboss.gwt.elemento.core.EventType.click;
import static org.jboss.gwt.elemento.core.EventType.mousedown;
import static org.jboss.gwt.elemento.core.EventType.mousemove;
import static org.jboss.gwt.elemento.core.EventType.mouseup;
import static org.jboss.gwt.elemento.core.EventType.touchend;
import static org.jboss.gwt.elemento.core.EventType.touchmove;
import static org.jboss.gwt.elemento.core.EventType.touchstart;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import elemental2.dom.Event;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import gameoflife.GameOfLife.Board;
import gameoflife.GameOfLife.XY;
import io.reactivex.Observable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import jsinterop.base.Js;
import jsinterop.base.JsPropertyMap;
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
        body().add(div().css(CSS.fork())
                .add(a("https://github.com/ibaca/gameoflife").textContent("Fork me on GitHub")));

        Map<XY, HTMLElement> cells = new HashMap<>();
        double yMax = window.innerWidth / 20., xMax = window.innerHeight / 20.;
        Stream.Builder<XY> initialAlive = Stream.builder();
        for (int x = 0; x < xMax; x++) {
            for (int y = 0; y < yMax; y++) {
                XY xy = new XY(x, y);
                int size = 20 - 4, top = x * (size + 4), left = y * (size + 4);
                HTMLDivElement el = div().style(""
                        + "width: " + size + "px; "
                        + "height: " + size + "px; "
                        + "top: " + top + "px; "
                        + "left: " + left + "px").get();
                Js.asPropertyMap(el).set("__xy", xy);
                board.appendChild(el);
                cells.put(xy, el);
                if (Math.random() > .7) initialAlive.add(xy);
            }
        }

        Iterable<Observable<?>> playMode = () -> Stream.generate(() -> Stream.of(
                Observable.never().doOnSubscribe(s -> play.textContent = "Play"),
                Observable.interval(1000, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x1"),
                Observable.interval(1000 / 2, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x2"),
                Observable.interval(1000 / 4, MILLISECONDS).doOnSubscribe(s -> play.textContent = "x3")))
                .flatMap(identity()).iterator();

        Observable<XY> mouseDrag$ = fromEvent(board, mousedown).doOnNext(Event::preventDefault)
                .switchMap(e -> fromEvent(board, mousemove).startWith(e).takeUntil(fromEvent(board, mouseup))
                        .flatMapMaybe(ev -> fromCallable(() -> Js.<JsPropertyMap<XY>>cast(ev.target).get("__xy")))
                        .distinctUntilChanged());
        Observable<XY> touchDrag$ = fromEvent(board, touchstart).doOnNext(Event::preventDefault)
                .switchMap(e -> fromEvent(board, touchmove).startWith(e).takeUntil(fromEvent(board, touchend))
                        .map(ev -> ev.touches.getAt(0))
                        .flatMapMaybe(ev -> fromCallable(() -> document.elementFromPoint(ev.clientX, ev.clientY)))
                        .flatMapMaybe(el -> fromCallable(() -> Js.<JsPropertyMap<XY>>cast(el).get("__xy")))
                        .distinctUntilChanged());

        Function<Board, Board> tick = board -> board.push(GameOfLife::rule);
        Observable.<Function<Board, Board>>empty()
                .mergeWith(fromEvent(clear, click).map(ev -> board -> new Board(Stream.empty())))
                .mergeWith(fromEvent(next, click).map(ev -> tick))
                .mergeWith(fromEvent(play, click).map(e -> TRUE).startWith(TRUE)
                        .zipWith(playMode, (e, m) -> m).switchMap(o -> o.map(n -> tick), 1))
                .mergeWith(merge(mouseDrag$, touchDrag$).map(xy -> board -> board.toggle(xy)))
                .scan(Board.of(initialAlive.build().toArray(XY[]::new)), (state, change) -> change.apply(state))
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
