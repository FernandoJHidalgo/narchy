//package org.jewelsea.willow.logger;
//
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import javafx.animation.Animation;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.application.Application;
//import javafx.beans.binding.Bindings;
//import javafx.beans.property.*;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.css.PseudoClass;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.*;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.VBox;
//import javafx.stage.Stage;
//import javafx.util.Duration;
//import nars.$;
//import org.slf4j.Logger;
//import org.slf4j.event.Level;
//
//import java.text.SimpleDateFormat;
//import java.util.Collection;
//import java.util.Random;
//import java.util.concurrent.BlockingDeque;
//import java.util.concurrent.LinkedBlockingDeque;
//
//import static org.slf4j.event.Level.INFO;
//import static org.slf4j.event.Level.WARN;
//
//
///**
// * THANKS JEWELSEA
// * http://stackoverflow.com/questions/24116858/most-efficient-way-to-log-messages-to-javafx-textarea-via-threads-with-simple-cu
// *
// *
//
// */
//final class Log extends FXConsoleAppender {
//
//
//    private final BlockingDeque<ILoggingEvent> log;
//
//    public Log(int capacity) {
//        this.log = new LinkedBlockingDeque<>(capacity);
//
//        $.logRoot.addAppender(this);
//        setName("FX1");
//        start();
//
//    }
//
//
//
//    @Override
//    public void append(ILoggingEvent event) {
//
//        log.offer(event);
//    }
//
//    public void drainTo(Collection<? super ILoggingEvent> collection) {
//        log.drainTo(collection);
//    }
//
//
//
//}
//
//
//
////class LoggingEvent {
////    private Date   timestamp;
////    private Level  level;
////    private String context;
////    private String message;
////
////    public LoggingEvent(Level level, String context, String message) {
////        this.timestamp = new Date();
////        this.level     = level;
////        this.context   = context;
////        this.message   = message;
////    }
////
////    public Date getTimestamp() {
////        return timestamp;
////    }
////
////    public Level getLevel() {
////        return level;
////    }
////
////    public String getContext() {
////        return context;
////    }
////
////    public String getMessage() {
////        return message;
////    }
////}
//
//class LogView extends ListView<ILoggingEvent> {
//    private static final int MAX_ENTRIES = 1000;
//
//    private final static PseudoClass debug = PseudoClass.getPseudoClass("debug");
//    private final static PseudoClass info  = PseudoClass.getPseudoClass("info");
//    private final static PseudoClass warn  = PseudoClass.getPseudoClass("warn");
//    private final static PseudoClass error = PseudoClass.getPseudoClass("error");
//
//    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
//
//    private final BooleanProperty       showTimestamp = new SimpleBooleanProperty(false);
//    private final ObjectProperty<Level> filterLevel   = new SimpleObjectProperty<>(null);
//    private final BooleanProperty       tail          = new SimpleBooleanProperty(false);
//    private final BooleanProperty       paused        = new SimpleBooleanProperty(false);
//    private final DoubleProperty        refreshRate   = new SimpleDoubleProperty(60);
//
//    private final ObservableList<ILoggingEvent> logItems = FXCollections.observableArrayList();
//
//    public BooleanProperty showTimeStampProperty() {
//        return showTimestamp;
//    }
//
//    public ObjectProperty<Level> filterLevelProperty() {
//        return filterLevel;
//    }
//
//    public BooleanProperty tailProperty() {
//        return tail;
//    }
//
//    public BooleanProperty pausedProperty() {
//        return paused;
//    }
//
//    public DoubleProperty refreshRateProperty() {
//        return refreshRate;
//    }
//
//    public LogView(Log logger) {
//        getStyleClass().add("log-view");
//
//        Timeline logTransfer = new Timeline(
//                new KeyFrame(
//                        Duration.seconds(1),
//                        event -> {
//                            ObservableList<ILoggingEvent> ii = this.logItems;
//                            logger.drainTo(ii);
//
//                            int s1 = ii.size();
//                            if (s1 > MAX_ENTRIES) {
//                                ii.remove(0, s1 - MAX_ENTRIES);
//                            }
//
//                            if (tail.get()) {
//                                scrollTo(ii.size());
//                            }
//                        }
//                )
//        );
//        logTransfer.setCycleCount(Timeline.INDEFINITE);
//        logTransfer.rateProperty().bind(refreshRateProperty());
//
//        this.pausedProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue && logTransfer.getStatus() == Animation.Status.RUNNING) {
//                logTransfer.pause();
//                logger.stop();
//            }
//
//            if (!newValue && logTransfer.getStatus() == Animation.Status.PAUSED && getParent() != null) {
//                logTransfer.play();
//                logger.start();
//            }
//        });
//
//        this.parentProperty().addListener((observable, oldValue, newValue) -> {
//            if (newValue == null) {
//                logger.stop();
//                logTransfer.pause();
//            } else {
//                if (!paused.get()) {
//                    logTransfer.play();
//                    logger.start();
//                }
//            }
//        });
////
////        filterLevel.addListener((observable, oldValue, newValue) -> {
////            setItems(
////                    new FilteredList<ILoggingEvent>(
////                            logItems,
////                            LoggingEvent ->
////                                LoggingEvent.getLevel().ordinal() >=
////                                filterLevel.get().ordinal()
////                    )
////            );
////        });
////        filterLevel.set(this.DEBUG);
//
//
//        setCellFactory(param -> new ListCell<ILoggingEvent>() {
//            {
//                showTimestamp.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
//            }
//
//            @Override
//            protected void updateItem(ILoggingEvent item, boolean empty) {
//                super.updateItem(item, empty);
//
//                pseudoClassStateChanged(debug, false);
//                pseudoClassStateChanged(info, false);
//                pseudoClassStateChanged(warn, false);
//                pseudoClassStateChanged(error, false);
//
//                if (item == null || empty) {
//                    setText(null);
//                    return;
//                }
//
//                String context =
//                        (item.getLoggerName() == null)
//                                ? ""
//                                : item.getLoggerName() + " ";
//
//                if (showTimestamp.get()) {
//                    //long timestamp
//
//
//                                    //: timestampFormatter.format(item.getTimestamp()) + " ";
//                    setText(item.getTimeStamp() + context + item.getMessage());
//                } else {
//                    setText(context + item.getMessage());
//                }
//
////                if (item.getLevel().isGreaterOrEqual(DEBUG)) {
////
////                }
////                switch (item.getLevel()) {
////                    case DEBUG:
////                        pseudoClassStateChanged(debug, true);
////                        break;
////
////                    case INFO:
////                        pseudoClassStateChanged(info, true);
////                        break;
////
////                    case WARN:
////                        pseudoClassStateChanged(warn, true);
////                        break;
//////
//////                }
//            }
//        });
//    }
//}
//
//class Lorem {
//    private static final String[] IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque hendrerit imperdiet mi quis convallis. Pellentesque fringilla imperdiet libero, quis hendrerit lacus mollis et. Maecenas porttitor id urna id mollis. Suspendisse potenti. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Cras lacus tellus, semper hendrerit arcu quis, auctor suscipit ipsum. Vestibulum venenatis ante et nulla commodo, ac ultricies purus fringilla. Aliquam lectus urna, commodo eu quam a, dapibus bibendum nisl. Aliquam blandit a nibh tincidunt aliquam. In tellus lorem, rhoncus eu magna id, ullamcorper dictum tellus. Curabitur luctus, justo a sodales gravida, purus sem iaculis est, eu ornare turpis urna vitae dolor. Nulla facilisi. Proin mattis dignissim diam, id pellentesque sem bibendum sed. Donec venenatis dolor neque, ut luctus odio elementum eget. Nunc sed orci ligula. Aliquam erat volutpat.".split(" ");
//    private static final int MSG_WORDS = 8;
//    private int idx;
//
//    private final Random random = new Random(42);
//
//    synchronized public String nextString() {
//        int end = Math.min(idx + MSG_WORDS, IPSUM.length);
//
//        StringBuilder result = new StringBuilder();
//        for (int i = idx; i < end; i++) {
//            result.append(IPSUM[i]).append(" ");
//        }
//
//        idx += MSG_WORDS;
//        idx = idx % IPSUM.length;
//
//        return result.toString();
//    }
//
//    synchronized public Level nextLevel() {
//        double v = random.nextDouble();
//
//        if (v < 0.8) {
//            return DEBUG;
//        }
//
//        if (v < 0.95) {
//            return INFO;
//        }
//
//        if (v < 0.985) {
//            return WARN;
//        }
//
//        return ERROR;
//    }
//
//}
//
//public class LogViewer extends Application {
//    private final Random random = new Random(42);
//
//    @Override
//    public void start(Stage stage) throws Exception {
//        Lorem  lorem  = new Lorem();
//        Log    log    = new Log(256);
//
//        //Logger logger = new Logger(log, "main");
//        Logger logger = $.logger;
//
//        logger.info("Hello");
//        logger.warn("Don't pick up alien hitchhickers");
//
//        for (int x = 0; x < 20; x++) {
//            Thread generatorThread = new Thread(
//                    () -> {
//                        for (;;) {
//                            logger.info(
//                                lorem.nextString()
//                            );
//
//                            try {
//                                Thread.sleep(random.nextInt(1_000));
//                            } catch (InterruptedException e) {
//                                Thread.currentThread().interrupt();
//                            }
//                        }
//                    },
//                    "log-gen-" + x
//            );
//            generatorThread.setDaemon(true);
//            generatorThread.start();
//        }
//
//        LogView logView = new LogView(log);
//        logView.setPrefWidth(400);
//
//        ChoiceBox<Level> filterLevel = new ChoiceBox<>(
//                FXCollections.observableArrayList(
//                        values()
//                )
//        );
////        filterLevel.getSelectionModel().select(DEBUG);
//        logView.filterLevelProperty().bind(
//                filterLevel.getSelectionModel().selectedItemProperty()
//        );
//
//        ToggleButton showTimestamp = new ToggleButton("Show Timestamp");
//        logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());
//
//        ToggleButton tail = new ToggleButton("Tail");
//        logView.tailProperty().bind(tail.selectedProperty());
//
//        ToggleButton pause = new ToggleButton("Pause");
//        logView.pausedProperty().bind(pause.selectedProperty());
//
//        Slider rate = new Slider(0.1, 60, 60);
//        logView.refreshRateProperty().bind(rate.valueProperty());
//        Label rateLabel = new Label();
//        rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
//        rateLabel.setStyle("-fx-font-family: monospace;");
//        VBox rateLayout = new VBox(rate, rateLabel);
//        rateLayout.setAlignment(Pos.CENTER);
//
//        HBox controls = new HBox(
//                10,
//                filterLevel,
//                showTimestamp,
//                tail,
//                pause,
//                rateLayout
//        );
//        controls.setMinHeight(HBox.USE_PREF_SIZE);
//
//        VBox layout = new VBox(
//                10,
//                controls,
//                logView
//        );
//        VBox.setVgrow(logView, Priority.ALWAYS);
//
//        Scene scene = new Scene(layout);
//        scene.getStylesheets().setAll(
//            res("log-view.css"),
//            res("narfx.css")
//        );
//        stage.setScene(scene);
//        stage.show();
//
//    }
//
//    public static String res(String path) {
//        return LogViewer.class.getClassLoader().getResource(path).toExternalForm();
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}