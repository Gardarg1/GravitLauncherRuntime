package pro.gravit.launcher.client.gui.scenes.update;

import javafx.animation.ScaleTransition;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerButton;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.hasher.FileNameMatcher;
import pro.gravit.launcher.hasher.HashedDir;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.optional.OptionalView;
import pro.gravit.utils.helper.LogHelper;

import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class UpdateScene extends AbstractScene {
    private ProgressBar progressBar;

    private Label volume;

    private Button cancel;
    private Button info_button;


    private VisualDownloader downloader;
    private volatile DownloadStatus downloadStatus = DownloadStatus.COMPLETE;

    public UpdateScene(JavaFXApplication application) {
        super("scenes/update/update.fxml", application);
    }

    @Override
    protected void doInit() {
        progressBar = LookupHelper.lookup(layout, "#progress");
        info_button = LookupHelper.lookup(layout, "#info_button");


        info_button.setOnMouseEntered((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), info_button);
            transition.setToX(1.05);
            transition.setToY(1.05);
            transition.play();
        });
        info_button.setOnMouseExited((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), info_button);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });

        cancel = LookupHelper.lookup(layout, "#cancel_button");
        volume = LookupHelper.lookup(layout, "#volume");
        cancel.setOnMouseEntered((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), cancel);
            transition.setToX(1.05);
            transition.setToY(1.05);
            transition.play();
        });
        cancel.setOnMouseExited((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), cancel);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });

        downloader = new VisualDownloader(application, progressBar, volume, this::errorHandle,
                                          (log) -> contextHelper.runInFxThread(() -> addLog(log)), this::onUpdateStatus);
        LookupHelper.<ButtonBase>lookup(layout, "#cancel_button").setOnAction((e) -> {
            if (downloadStatus == DownloadStatus.DOWNLOAD && downloader.isDownload()) {
                downloader.cancel();
                try {
                    switchScene(application.gui.serverMenuScene);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else if(downloadStatus == DownloadStatus.ERROR || downloadStatus == DownloadStatus.COMPLETE) {
                try {
                    switchScene(application.gui.serverMenuScene);
                } catch (Exception exception) {
                    errorHandle(exception);
                }
            }
        });
    }

    private void onUpdateStatus(DownloadStatus newStatus) {
        this.downloadStatus = newStatus;
        LogHelper.debug("Update download status: %s", newStatus.toString());
    }

    public void sendUpdateAssetRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest,
            String assetIndex, Consumer<HashedDir> onSuccess) {
        downloader.sendUpdateAssetRequest(dirName, dir, matcher, digest, assetIndex, onSuccess);
    }

    public void sendUpdateRequest(String dirName, Path dir, FileNameMatcher matcher, boolean digest, OptionalView view,
            boolean optionalsEnabled, Consumer<HashedDir> onSuccess) {
        downloader.sendUpdateRequest(dirName, dir, matcher, digest, view, optionalsEnabled, onSuccess);
    }

    public void addLog(String string) {


    }

    @Override
    public void reset() {
        progressBar.progressProperty().setValue(0);
        progressBar.getStyleClass().removeAll("progress");
        Pane serverButtonContainer = LookupHelper.lookup(layout, "#serverButton");
        serverButtonContainer.getChildren().clear();
        ClientProfile profile = application.profilesService.getProfile();
        ServerButton serverButton = ServerMenuScene.getServerButton(application, profile);
        serverButton.addTo(serverButtonContainer);

    }

    @Override
    public void errorHandle(Throwable e) {
        if(e instanceof CompletionException) {
            e = e.getCause();
        }
        addLog("Exception %s: %s".formatted(e.getClass(), e.getMessage() == null ? "" : e.getMessage()));
        progressBar.getStyleClass().add("progressError");
        LogHelper.error(e);
    }

    @Override
    public String getName() {
        return "update";
    }

    public enum DownloadStatus {
        ERROR, HASHING, REQUEST, DOWNLOAD, COMPLETE, DELETE
    }
}
