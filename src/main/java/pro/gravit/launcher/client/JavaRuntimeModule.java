package pro.gravit.launcher.client;

import javafx.stage.Stage;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.events.ClientEngineInitPhase;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.events.ClientPreGuiPhase;
import pro.gravit.launcher.client.events.ClientUnlockConsoleEvent;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.service.OfflineService;
import pro.gravit.launcher.gui.RuntimeProvider;
import pro.gravit.launcher.modules.LauncherInitContext;
import pro.gravit.launcher.modules.LauncherModule;
import pro.gravit.launcher.modules.LauncherModuleInfo;
import pro.gravit.launcher.modules.events.OfflineModeEvent;
import pro.gravit.launcher.request.websockets.OfflineRequestService;
import pro.gravit.utils.Version;
import pro.gravit.utils.helper.JVMHelper;
import pro.gravit.utils.helper.LogHelper;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.Base64;

public class JavaRuntimeModule extends LauncherModule {

    public final static String RUNTIME_NAME = "stdruntime";
    static LauncherEngine engine;
    private RuntimeProvider provider;

    public JavaRuntimeModule() {
        super(new LauncherModuleInfo("StdJavaRuntime",
                                     new Version(3, 0, 5, 1, Version.Type.STABLE),
                                     0, new String[]{}, new String[]{"runtime"}));
    }

    private static void noJavaFxAlert() {
        String message = """
                Библиотеки JavaFX не найдены. У вас %s(x%d) ОС %s(x%d). Java %s. Установите Java с поддержкой JavaFX, например OracleJRE 8 x%d с официального сайта.
                %s
                Если вы не можете решить проблему самостоятельно обратитесь к администрации своего проекта
                """.formatted(JVMHelper.RUNTIME_MXBEAN.getVmName(), JVMHelper.JVM_BITS, JVMHelper.OS_TYPE.name,
                              JVMHelper.OS_BITS, JVMHelper.RUNTIME_MXBEAN.getSpecVersion(), JVMHelper.OS_BITS,
                              JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE
                                      ? "Убедитесь что вы запускаете с правильной Java(ПКМ->Открыть с помощью->Java 8)"
                                      : "Убедитесь, что по умолчанию стоит запуск с подходяшей Java и установлен openjfx той же версии");
        JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
    }

    private static void noInitMethodAlert() {
        String message = """
                JavaFX приложение собрано некорректно. Обратитесь к администратору проекта с скриншотом этого окна
                Описание:
                При сборке отсутствовали библиотеки JavaFX. Пожалуйста установите Java с поддержкой JavaFX на стороне лаунчсервера и повторите сборку лаунчера
                """;
        JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
    }

    public static void noLocaleAlert(String file) {
        String message = """
                Не найден файл языка '%s' при инициализации GUI. Дальнейшая работа невозможна.
                Убедитесь что все файлы дизайна лаунчера присутствуют в папке runtime при сборке лаунчера
                """.formatted(file);
        JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
    }

    public static void noEnFSAlert() {
        String message = """
                Запуск лаунчера невозможен из-за ошибки расшифровки рантайма.
                Администраторам: установите библиотеку EnFS для исправления этой проблемы
                """;
        JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
    }

    public static void errorHandleAlert(Throwable e) {
        String message = """
                Произошла серьезная ошибка при инициализации интерфейса лаунчера.
                Для пользователей:
                Обратитесь к администрации своего проекта с скриншотом этого окна
                Java %d (x%d) Ошибка %s
                Описание: %s
                Более подробную информацию можно получить из лога
                """.formatted(JVMHelper.JVM_VERSION, JVMHelper.JVM_BITS, e.getClass().getName(),
                              e.getMessage() == null ? "null" : e.getMessage());
        JOptionPane.showMessageDialog(null, message, "GravitLauncher", JOptionPane.ERROR_MESSAGE);
    }

    public static String getLauncherInfo() {
        return "Launcher %s | Java %d(%s %s) x%d | %s x%d"
                .formatted(Version.getVersion().toString(), JVMHelper.JVM_VERSION, JVMHelper.RUNTIME_MXBEAN.getVmName(),
                           System.getProperty("java.version"), JVMHelper.JVM_BITS, JVMHelper.OS_TYPE.name(),
                           JVMHelper.OS_BITS);
    }

    public static String getMiniLauncherInfo() {
        return "Launcher %s | Java %d(%s) x%d | %s x%d"
                .formatted(Version.getVersion().toString(), JVMHelper.JVM_VERSION, System.getProperty("java.version"),
                           JVMHelper.JVM_BITS, JVMHelper.OS_TYPE.name(), JVMHelper.OS_BITS);
    }

    @Override
    public void init(LauncherInitContext initContext) {
        registerEvent(this::preGuiPhase, ClientPreGuiPhase.class);
        registerEvent(this::engineInitPhase, ClientEngineInitPhase.class);
        registerEvent(this::exitPhase, ClientExitPhase.class);
        registerEvent(this::consoleUnlock, ClientUnlockConsoleEvent.class);
        registerEvent(this::offlineMode, OfflineModeEvent.class);
    }

    private void preGuiPhase(ClientPreGuiPhase phase) {
        try {
            Class.forName("javafx.application.Application");
        } catch (ClassNotFoundException e) {
            noJavaFxAlert();
            LauncherEngine.exitLauncher(0);
        }
        try {
            Method m = JavaFXApplication.class.getMethod(new String(Base64.getDecoder().decode("c3RhcnQ=")),
                                                         Stage.class); // Fix proguard remapping
            if (m.getDeclaringClass() != JavaFXApplication.class)
                throw new RuntimeException("Method start not override");
        } catch (Throwable exception) {
            LogHelper.error(exception);
            noInitMethodAlert();
            LauncherEngine.exitLauncher(0);
        }
        provider = new StdJavaRuntimeProvider();
        phase.runtimeProvider = provider;
    }

    private void consoleUnlock(ClientUnlockConsoleEvent event) {
        if (engine.runtimeProvider instanceof StdJavaRuntimeProvider stdJavaRuntimeProvider) {
            stdJavaRuntimeProvider.registerPrivateCommands();
        }
    }

    private void offlineMode(OfflineModeEvent event) {
        OfflineService.applyRuntimeProcessors((OfflineRequestService) event.service);
    }

    private void engineInitPhase(ClientEngineInitPhase initPhase) {
        JavaRuntimeModule.engine = initPhase.engine;
    }

    private void exitPhase(ClientExitPhase exitPhase) {
        if (provider != null && provider instanceof StdJavaRuntimeProvider stdJavaRuntimeProvider) {
            try {
                stdJavaRuntimeProvider.getApplication().saveSettings();
            } catch (Throwable e) {
                LogHelper.error(e);
            }
        }
    }
}
