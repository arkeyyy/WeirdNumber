package org.example.weird_number;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CancellationException;

public class HelloController {
    @FXML private TextField upperLimitField;
    @FXML private TextField threadCountField;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Label errorLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane workerPane;
    @FXML private TextArea resultsArea;
    @FXML private Label weirdCountLabel;
    @FXML private Label otherCountLabel;
    @FXML private Label totalCountLabel;

    private List<RangeWorker> activeWorkers = List.of();
    private int finishedThreads;

    @FXML
    private void initialize() {
        upperLimitField.textProperty().addListener((field, before, after) -> setError(""));
        threadCountField.textProperty().addListener((field, before, after) -> setError(""));
    }

    @FXML
    private void startRun() {
        if (!activeWorkers.isEmpty()) {
            return;
        }
        int limit = positiveInteger(upperLimitField.getText());
        int threads = positiveInteger(threadCountField.getText());
        if (limit == 0) {
            setError("Upper limit must be a positive 32-bit integer.");
            return;
        }
        if (threads == 0 || threads > Math.min(limit, 64)) {
            setError("Threads must be a whole number from 1 to " + Math.min(limit, 64) + ".");
            return;
        }

        setError("");
        clearResults();
        workerPane.getChildren().clear();
        finishedThreads = 0;
        setRunning(true);
        activeWorkers = new ArrayList<>();
        for (WeirdNumbers.Range range : WeirdNumbers.ranges(limit, threads)) {
            RangeWorker worker = new RangeWorker(range);
            activeWorkers.add(worker);
            int number = activeWorkers.size();
            Label state = new Label("Waiting");
            workerPane.getChildren().add(workerCard(number, worker, state));
            worker.setOnRunning(event -> state.setText("Running"));
            worker.setOnSucceeded(event -> {
                if (activeWorkers.contains(worker)) {
                    state.setText("Done");
                    workerFinished(limit);
                }
            });
            worker.setOnFailed(event -> {
                if (activeWorkers.contains(worker)) {
                    discardRun("The calculation failed.");
                }
            });
            Thread thread = new Thread(worker, "weird-number-" + number);
            thread.setDaemon(true);
            thread.start();
        }
        updateRunningStatus();
    }

    private static int positiveInteger(String text) {
        try {
            return Math.max(0, Integer.parseInt(text.strip()));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private static VBox workerCard(int number, RangeWorker worker, Label state) {
        ProgressIndicator progress = new ProgressIndicator(0);
        progress.progressProperty().bind(worker.progressProperty());
        progress.setPrefSize(42, 42);
        progress.setMaxSize(42, 42);
        WeirdNumbers.Range range = worker.range;
        Label bounds = new Label(range.first() + " to " + range.last());
        bounds.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(6, new Label("Thread " + number), progress, bounds,
                new Label(range.size() == 1 ? "1 number" : range.size() + " numbers"), state);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(150);
        return card;
    }

    private void workerFinished(int limit) {
        finishedThreads++;
        if (finishedThreads < activeWorkers.size()) {
            updateRunningStatus();
            return;
        }

        StringJoiner matches = new StringJoiner(", ");
        int weirdCount = 0;
        for (RangeWorker worker : activeWorkers) {
            List<Integer> values = worker.getValue();
            weirdCount += values.size();
            for (int value : values) {
                matches.add(Integer.toString(value));
            }
        }
        resultsArea.setText(matches.toString());
        showCounts(weirdCount, limit);
        statusLabel.setText("Complete. Checked all " + limit + " numbers from 1 to " + limit
                + ". Finished threads: " + finishedThreads + " of " + activeWorkers.size() + ".");
        activeWorkers = List.of();
        setRunning(false);
    }

    private void updateRunningStatus() {
        statusLabel.setText("Running. Finished threads: " + finishedThreads + " of " + activeWorkers.size() + ".");
    }

    @FXML
    private void stopRun() {
        discardRun("Stopped.");
    }

    private void discardRun(String status) {
        cancelWorkers();
        workerPane.getChildren().clear();
        clearResults();
        setRunning(false);
        statusLabel.setText(status);
    }

    void cancelWorkers() {
        for (RangeWorker worker : activeWorkers) {
            worker.cancel(true);
        }
        activeWorkers = List.of();
    }

    private void clearResults() {
        resultsArea.clear();
        showCounts(0, 0);
    }

    private void showCounts(int weird, int checked) {
        weirdCountLabel.setText("Weird numbers: " + weird);
        otherCountLabel.setText("Other numbers: " + (checked - weird));
        totalCountLabel.setText("Total checked: " + checked);
    }

    private void setError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(!message.isEmpty());
        errorLabel.setVisible(!message.isEmpty());
    }

    private void setRunning(boolean running) {
        upperLimitField.setDisable(running);
        threadCountField.setDisable(running);
        startButton.setDisable(running);
        stopButton.setDisable(!running);
    }

    private static final class RangeWorker extends Task<List<Integer>> {
        private final WeirdNumbers.Range range;

        private RangeWorker(WeirdNumbers.Range range) {
            this.range = range;
            updateProgress(0, range.size());
        }

        @Override
        protected List<Integer> call() {
            List<Integer> matches = new ArrayList<>();
            int size = range.size();
            long lastUpdate = System.nanoTime();
            for (long value = range.first(); value <= range.last(); value++) {
                if (isCancelled()) {
                    throw new CancellationException();
                }
                if (WeirdNumbers.isWeird((int) value)) {
                    matches.add((int) value);
                }
                long now = System.nanoTime();
                if (value == range.last() || now - lastUpdate >= 50_000_000L) {
                    updateProgress(value - range.first() + 1, size);
                    lastUpdate = now;
                }
            }
            return matches;
        }
    }
}
