package pro.gravit.launcher.client.gui.scenes.settings;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import oshi.SystemInfo;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.DesignConstants;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerButton;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.client.gui.stage.ConsoleStage;
import pro.gravit.launcher.client.gui.utils.JavaFxUtils;
import pro.gravit.launcher.events.request.GetAssetUploadUrlRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.cabinet.AssetUploadInfoRequest;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;
import pro.gravit.launcher.client.gui.scenes.settings.components.LanguageSelectorComponent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.function.Consumer;

public class SettingsScene extends AbstractScene {
    private LanguageSelectorComponent languageSelectorComponent;

    private final static long MAX_JAVA_MEMORY_X64 = 32 * 1024;
    private final static long MAX_JAVA_MEMORY_X32 = 1536;
    private Pane componentList;
    private Pane settingsList;
    private Label ramLabel;
    private Slider ramSlider;
    private ProgressBar ramBar;
    private RuntimeSettings.ProfileSettingsView profileSettings;
    private JavaSelectorComponent javaSelector;
    private ImageView avatar;
    private Image originalAvatarImage;

    public SettingsScene(JavaFXApplication application) {
        super("scenes/settings/settings.fxml", application);
    }

    @Override
    protected void doInit() {

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
        LookupHelper.lookup(layout,  "#servers2").setOnMouseClicked((e) -> {
            try {
                switchScene(application.gui.serverInfoScene);

            } catch (Exception exception) {
                errorHandle(exception);
            }
        });

        LookupHelper.lookup(layout,  "#home").setOnMouseClicked((e) -> {
            try {
      switchScene(application.gui.serverMenuScene);
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });

        LookupHelper.lookup(layout,  "#save").setOnMouseClicked((e) -> {
            try {
                ClientProfile profile = application.profilesService.getProfile();
                profileSettings.apply();
                application.triggerManager.process(profile, application.profilesService.getOptionalView());
                switchScene(application.gui.serverInfoScene);
            } catch (Exception exception) {
                errorHandle(exception);
            }
        });

        componentList = (Pane) LookupHelper.<ScrollPane>lookup(layout, "#settingslist").getContent();
        settingsList = LookupHelper.lookup(componentList, "#settings-list");
        languageSelectorComponent = new LanguageSelectorComponent(application, componentList);

        ramSlider = LookupHelper.lookup(componentList, "#ramSlider");
        ramBar = LookupHelper.lookup(componentList, "#ramBar");

        ramLabel = LookupHelper.lookup(componentList, "#ramLabel");
        long maxSystemMemory;
        try {
            SystemInfo systemInfo = new SystemInfo();
            maxSystemMemory = (systemInfo.getHardware().getMemory().getTotal() >> 20);
        } catch (Throwable e) {
            LogHelper.error(e);
            maxSystemMemory = 2048;
        }
        ramSlider.setMax(Math.min(maxSystemMemory, getJavaMaxMemory()));

        ramSlider.setSnapToTicks(true);
        ramSlider.setShowTickMarks(true);
        ramSlider.setShowTickLabels(true);
        ramSlider.setMinorTickCount(1);
        ramSlider.setMajorTickUnit(1024);
        ramSlider.setBlockIncrement(1024);
        ramSlider.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double object) {
                return "%.0fG".formatted(object / 1024);
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
        Hyperlink updateDirLink = LookupHelper.lookup(componentList, "#folder", "#path");
        String directoryUpdates = DirBridge.dirUpdates.toAbsolutePath().toString();
        updateDirLink.setText(directoryUpdates);
        if (updateDirLink.getTooltip() != null) {
            updateDirLink.getTooltip().setText(directoryUpdates);
        }
        updateDirLink.setOnAction((e) -> application.openURL(directoryUpdates));
        LookupHelper.<ButtonBase>lookup(componentList, "#changeDir").setOnAction((e) -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(application.getTranslation("runtime.scenes.settings.dirTitle"));
            directoryChooser.setInitialDirectory(DirBridge.dir.toFile());
            File choose = directoryChooser.showDialog(application.getMainStage().getStage());
            if (choose == null) return;
            Path newDir = choose.toPath().toAbsolutePath();
            try {
                DirBridge.move(newDir);
            } catch (IOException ex) {
                errorHandle(ex);
            }
            application.runtimeSettings.updatesDirPath = newDir.toString();
            application.runtimeSettings.updatesDir = newDir;
            String oldDir = DirBridge.dirUpdates.toString();
            DirBridge.dirUpdates = newDir;
            for (ClientProfile profile : application.profilesService.getProfiles()) {
                RuntimeSettings.ProfileSettings settings = application.getProfileSettings(profile);
                if (settings.javaPath != null && settings.javaPath.startsWith(oldDir)) {
                    settings.javaPath = newDir.toString().concat(settings.javaPath.substring(oldDir.length()));
                }
            }
            application.javaService.update();
            javaSelector.reset();
            updateDirLink.setText(application.runtimeSettings.updatesDirPath);
        });
        LookupHelper.<ButtonBase>lookupIfPossible(layout, "#deleteDir").ifPresent(a -> a.setOnAction(
                (e) -> application.messageManager.showApplyDialog(
                        application.getTranslation("runtime.scenes.settings.deletedir.header"),
                        application.getTranslation("runtime.scenes.settings.deletedir.description"), () -> {
                            LogHelper.debug("Delete dir: %s", DirBridge.dirUpdates);
                            try {
                                IOHelper.deleteDir(DirBridge.dirUpdates, false);
                            } catch (IOException ex) {
                                LogHelper.error(ex);
                                application.messageManager.createNotification(
                                        application.getTranslation("runtime.scenes.settings.deletedir.fail.header"),
                                        application.getTranslation(
                                                "runtime.scenes.settings.deletedir.fail.description"));
                            }
                        }, () -> {}, true)));

        reset();
    }
    private void updateRamBar() {
        double maxRam = ramSlider.getMax();
        double currentRam = ramSlider.getValue();
        double progress = currentRam / maxRam;

        ramBar.setProgress(progress);
    }

    private long getJavaMaxMemory() {
        if (application.javaService.isArchAvailable(JVMHelper.ARCH.X86_64) || application.javaService.isArchAvailable(
                JVMHelper.ARCH.ARM64)) {
            return MAX_JAVA_MEMORY_X64;
        }
        return MAX_JAVA_MEMORY_X32;
    }

    @Override
    public void reset() {
        profileSettings = new RuntimeSettings.ProfileSettingsView(application.getProfileSettings());
        javaSelector = new JavaSelectorComponent(application.javaService, componentList, profileSettings,
                                                 application.profilesService.getProfile());
        ramSlider.setValue(profileSettings.ram);
        ramSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            profileSettings.ram = newValue.intValue();
            updateRamLabel();
            updateRamBar(); // Обновляем ProgressBar
        });
        updateRamLabel();
        updateRamBar();

        settingsList.getChildren().clear();
        Label settingsListHeader = new Label(application.getTranslation("runtime.scenes.settings.header.options"));
        settingsListHeader.getStyleClass().add("settings-header");
        settingsList.getChildren().add(settingsListHeader);
        add("Debug", profileSettings.debug, (value) -> profileSettings.debug = value);
        add("AutoEnter", profileSettings.autoEnter, (value) -> profileSettings.autoEnter = value);
        add("Fullscreen", profileSettings.fullScreen, (value) -> profileSettings.fullScreen = value);
        if(JVMHelper.OS_TYPE == JVMHelper.OS.LINUX) {
            add("WaylandSupport", profileSettings.waylandSupport, (value) -> profileSettings.waylandSupport = value);
        }

    }



    @Override
    public String getName() {
        return "settings";
    }
    public void add(String languageName, boolean value, Consumer<Boolean> onChanged) {
        String nameKey = "runtime.scenes.settings.properties.%s.name".formatted(languageName.toLowerCase());
        String descriptionKey = "runtime.scenes.settings.properties.%s.description".formatted(
                languageName.toLowerCase());
        add(application.getTranslation(nameKey, languageName), application.getTranslation(descriptionKey, languageName),
            value, onChanged);
    }

    public void add(String name, String description, boolean value, Consumer<Boolean> onChanged) {
        HBox hBox = new HBox();
        CheckBox checkBox = new CheckBox();
        Label header = new Label();
        Label label = new Label();
        VBox vBox = new VBox();
        hBox.getStyleClass().add("settings-container");
        checkBox.getStyleClass().add("settings-checkbox");
        header.getStyleClass().add("settings-label-header");
        label.getStyleClass().add("settings-label");
        checkBox.setSelected(value);
        checkBox.setOnAction((e) -> onChanged.accept(checkBox.isSelected()));
        header.setText(name);
        label.setText(description);
        label.setWrapText(true);
        vBox.getChildren().add(header);
        vBox.getChildren().add(label);
        hBox.getChildren().add(checkBox);
        hBox.getChildren().add(vBox);
        settingsList.getChildren().add(hBox);
    }

    public void updateRamLabel() {
        ramLabel.setText(profileSettings.ram == 0
                                 ? application.getTranslation("runtime.scenes.settings.ramAuto")
                                 : MessageFormat.format(application.getTranslation("runtime.scenes.settings.ram"),
                                                        profileSettings.ram));
    }
}
