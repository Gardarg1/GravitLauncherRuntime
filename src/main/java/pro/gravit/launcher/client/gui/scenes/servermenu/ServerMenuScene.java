package pro.gravit.launcher.client.gui.scenes.servermenu;

import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import pro.gravit.launcher.client.ServerPinger;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.DesignConstants;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.utils.JavaFxUtils;
import pro.gravit.launcher.events.request.GetAssetUploadUrlRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.cabinet.AssetUploadInfoRequest;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.*;

public class ServerMenuScene extends AbstractScene {
    private List<ClientProfile> lastProfiles;
    private Button downlaod_button;
    private Button info_button;


    public ServerMenuScene(JavaFXApplication application) {
        super("scenes/servermenu/servermenu.fxml", application);
    }

    @Override
    public void doInit() {


        LookupHelper.lookup(layout,  "#mods").setOnMouseClicked((e) -> {
            try {
                if (application.profilesService.getProfile() == null) return;
                switchScene(application.gui.optionsScene);
                application.gui.optionsScene.reset();
            } catch (Exception ex) {
                errorHandle(ex);
            }
        });
        LookupHelper.lookup(layout,  "#config").setOnMouseClicked((e) -> {
            try {
                switchScene(application.gui.settingsScene);
                application.gui.settingsScene.reset();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        LookupHelper.lookup(layout,  "#serverlist").setOnMouseClicked((e) -> {
            try {
                switchScene(application.gui.serverInfoScene);

            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
        LookupHelper.lookup(layout,  "#servers2").setOnMouseClicked((e) -> {
            try {
                switchScene(application.gui.serverInfoScene);

            } catch (Exception exception) {
                errorHandle(exception);
            }
        });


        ClientProfile profile = application.profilesService.getProfile();
        LookupHelper.<Button>lookup(layout, "#download_button").setOnMouseClicked((e) -> {
            try {

                runClient();
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });
    downlaod_button = LookupHelper.lookup(layout,"#download_button");
        info_button = LookupHelper.lookup(layout,"#info_button");

        downlaod_button.setOnMouseEntered((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), downlaod_button);
            transition.setToX(1.05);
            transition.setToY(1.05);
            transition.play();
        });
        downlaod_button.setOnMouseExited((event) -> {
            ScaleTransition transition = new ScaleTransition(Duration.seconds(0.1), downlaod_button);
            transition.setToX(1);
            transition.setToY(1);
            transition.play();
        });

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



        ScrollPane scrollPane = LookupHelper.lookup(layout, "#servers");
        scrollPane.setOnScroll(e -> {
            double widthContent = scrollPane.getWidth();
            double offset = (widthContent * 0.15) / (scrollPane.getContent().getBoundsInLocal().getWidth() - widthContent) * Math.signum(e.getDeltaY());
            scrollPane.setHvalue(scrollPane.getHvalue() - offset);
        });
        reset();

    }

    public static class ServerButtonCache {
        public ServerButton serverButton;
        public int position;
    }

    public static ServerButton getServerButton(JavaFXApplication application, ClientProfile profile) {
        return new ServerButton(application, profile);
    }

    @Override
    public void reset() {
        if (lastProfiles == application.profilesService.getProfiles()) return;
        lastProfiles = application.profilesService.getProfiles();
        Map<ClientProfile, ServerButtonCache> serverButtonCacheMap = new LinkedHashMap<>();
        
        List<ClientProfile> profiles = new ArrayList<>(lastProfiles);
        profiles.sort(Comparator.comparingInt(ClientProfile::getSortIndex).thenComparing(ClientProfile::getTitle));
        int position = 0;
        for (ClientProfile profile : profiles) {
            ServerButtonCache cache = new ServerButtonCache();
            cache.serverButton = getServerButton(application, profile);
            cache.position = position;
            serverButtonCacheMap.put(profile, cache);
            position++;
        }
        ScrollPane scrollPane = LookupHelper.lookup(layout, "#servers");
        FlowPane serverList = (FlowPane) scrollPane.getContent();
        serverList.setHgap(20);
        serverList.getChildren().clear();
        application.pingService.clear();

        serverButtonCacheMap.forEach((profile, serverButtonCache) -> {
            EventHandler<? super MouseEvent> handle = (event) -> {

                if (!event.getButton().equals(MouseButton.PRIMARY)) return;
                LookupHelper.<Button>lookup(layout, "#download_button").setDisable(false);
                LookupHelper.<Label>lookup(layout,"#server_tap").setText(application.getTranslation("runtime.scenes.servermenu.selected_server")+" "+profile.getTitle());
                LookupHelper.lookup(layout,"#mods").setVisible(true);
                LookupHelper.lookup(layout,"#config").setVisible(true);

changeServer(profile);
            };



            serverButtonCache.serverButton.addTo(serverList, serverButtonCache.position);
            serverButtonCache.serverButton.setOnMouseClicked(handle);
        });

        CommonHelper.newThread("ServerPinger", true, () -> {
            for (ClientProfile profile : lastProfiles) {
                for (ClientProfile.ServerProfile serverProfile : profile.getServers()) {
                    if (!serverProfile.socketPing || serverProfile.serverAddress == null) continue;
                    try {
                        ServerPinger pinger = new ServerPinger(serverProfile, profile.getVersion());
                        ServerPinger.Result result = pinger.ping();
                        contextHelper.runInFxThread(
                                () -> application.pingService.addReport(serverProfile.name, result));
                    } catch (IOException ignored) {
                    }
                }
            }
        }).start();


    }

    private void runClient() {
        application.launchService.launchClient().thenAccept((clientInstance -> {
            if(clientInstance.getSettings().debug) {
                contextHelper.runInFxThread(() -> {
                    try {
                        switchScene(application.gui.debugScene);
                        application.gui.debugScene.onClientInstance(clientInstance);
                    } catch (Exception ex) {
                        errorHandle(ex);
                    }
                });
            } else {
                clientInstance.start();
                clientInstance.getOnWriteParamsFuture().thenAccept((ok) -> {
                    LogHelper.info("Params write successful. Exit...");
                    Platform.exit();
                }).exceptionally((ex) -> {
                    contextHelper.runInFxThread(() -> {
                        errorHandle(ex);
                    });
                    return null;
                });
            }
        })).exceptionally((ex) -> {
            contextHelper.runInFxThread(() -> {
                errorHandle(ex);
            });
            return null;
        });
    }

    @Override
    public String getName() {
        return "serverMenu";
    }

    private void changeServer(ClientProfile profile) {
        application.profilesService.setProfile(profile);
        application.runtimeSettings.lastProfile = profile.getUUID();
    }
}
