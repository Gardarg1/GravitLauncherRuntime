package pro.gravit.launcher.client.gui.scenes.servermenu;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.client.gui.utils.JavaFxUtils;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.LogHelper;

import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

public class ServerButton extends AbstractVisualComponent {
    private static final String SERVER_BUTTON_FXML = "components/serverButton.fxml";
    private static final String SERVER_BUTTON_CUSTOM_FXML = "components/serverButton/%s.fxml";
    private static final String SERVER_BUTTON_DEFAULT_IMAGE = "images/servers/example.png";
    private static final String SERVER_BUTTON_CUSTOM_IMAGE = "images/servers/%s.png";
    public ClientProfile profile;
    private Button saveButton;
    private Button resetButton;
    private Region serverLogo;
    public ServerButton(JavaFXApplication application, ClientProfile profile) {
        super(getFXMLPath(application, profile), application);
        this.profile = profile;
    }
    private static String getFXMLPath(JavaFXApplication application, ClientProfile profile) {
        String customFxmlName = String.format(SERVER_BUTTON_CUSTOM_FXML, profile.getUUID());
        URL customFxml = application.tryResource(customFxmlName);
        if (customFxml != null) {
            return customFxmlName;
        }
        return SERVER_BUTTON_FXML;
    }
    @Override
    public String getName() {
        return "serverButton";
    }

    @Override
    protected void doInit() {
        LookupHelper.<Labeled>lookup(layout, "#nameServer").setText(profile.getTitle());
        LookupHelper.<Labeled>lookup(layout, "#genreServer").setText(profile.getInfo());

        layout.setOnMouseEntered((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), layout);
            transition.setToX(1.05);
            transition.setToY(1.05);
            transition.play();
        });
        layout.setOnMouseExited((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), layout);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });


        AtomicLong currentOnline = new AtomicLong(0);
        AtomicLong maxOnline = new AtomicLong(0);
        Runnable update = () -> contextHelper.runInFxThread(() -> {
            if (currentOnline.get() == 0 && maxOnline.get() == 0) {
                LookupHelper.<Labeled>lookup(layout, "#online").setText("?");
            } else {
                LookupHelper.<Labeled>lookup(layout, "#online").setText(String.valueOf(currentOnline.get()));
            }
        });
        for (ClientProfile.ServerProfile serverProfile : profile.getServers()) {
            application.pingService.getPingReport(serverProfile.name).thenAccept((report) -> {
                if (report != null) {
                    currentOnline.addAndGet(report.playersOnline);
                    maxOnline.addAndGet(report.maxPlayers);
                }
                update.run();
            });
        }

    }

    @Override
    protected void doPostInit() {

    }

    public void setOnMouseClicked(EventHandler<? super MouseEvent> eventHandler) {
        layout.setOnMouseClicked(eventHandler);
    }


    public void addTo(Pane pane) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(layout);
    }

    public void addTo(Pane pane, int position) {
        if (!isInit()) {
            try {
                init();
            } catch (Exception e) {
                LogHelper.error(e);
            }
        }
        pane.getChildren().add(position, layout);
    }

    @Override
    public void reset() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void enable() {

    }
}
