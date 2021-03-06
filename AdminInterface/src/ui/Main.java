package ui;

import client.admin.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class Main extends Application {

    public Popup createPopup(String popupText, double PosX, double PosY) {
        final Popup popup = new Popup();
        Label label = new Label(popupText);
        label.setMinHeight(50);
        label.setMinWidth(15);
        label.setStyle("-fx-min-height:60;-fx-min-width:30;-fx-background-color: grey;-fx-background-radius: 5px;-fx-text-fill:white");
        popup.getContent().add(label);
        popup.setX(PosX);
        popup.setY(PosY);
        return popup;
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainWindow.fxml"));
        Parent root = loader.load();
        stage.setTitle("Администратор");
        stage.setScene(new Scene(root, 1158, 700));
        MainWindowController controller = loader.getController();


        controller.settingsBtn.setOnAction((ActionEvent) -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/SettingsWindow.fxml"));
            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Stage prStage = new Stage();
            prStage.initModality(Modality.APPLICATION_MODAL);
            prStage.setTitle("Настройки");
            SettingsController settingsController = fxmlLoader.getController();

            Popup popup = createPopup("Настройки сохранены!", 665, 385);
            settingsController.saveSettings.setOnMouseClicked(event -> {
                settingsController.save();
                popup.show(prStage);
                new Timer().schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(popup::hide);
                            }
                        },
                        2000
                );
            });
            prStage.setScene(new Scene(root1));
            prStage.show();
        });

        controller.updateBtn.setOnAction(ActionEvent->controller.updateTableData());
        controller.resetDatabaseBtn.setOnAction(ActionEvent -> {
            try {
                controller.truncateDatabase();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        controller.resetVehicleStateBtn.setOnAction(ActionEvent -> {
            try {
                controller.resetVehicleState();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        controller.reconnectBtn.setOnAction(ActionEvent -> {
            try {
                controller.reconnect();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        controller.editDatabaseBtn.setOnAction((ActionEvent) -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("resources/EditingWindow.fxml"));
            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            EditingController editingController = fxmlLoader.getController();
            Stage prStage = new Stage();
            prStage.initModality(Modality.APPLICATION_MODAL);
            prStage.setTitle("Редактор базы данных");
            prStage.setOnCloseRequest(event -> {
                try {
                    editingController.shutdown();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            prStage.setScene(new Scene(root1));
            prStage.show();
        });

        controller.archiveBtn.setOnAction(ActionEvent -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("resources/ArchiveWindow.fxml"));
            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ArchiveController archiveController = fxmlLoader.getController();
            Stage prStage = new Stage();
            prStage.initModality(Modality.APPLICATION_MODAL);
            prStage.setTitle("Архив");
            prStage.setOnCloseRequest(event -> {
                try {
                    archiveController.shutdown();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            prStage.setScene(new Scene(root1));
            prStage.show();
        });

        controller.editRegNumBtn.setOnAction(ActionEvent -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("resources/RegistrationNumberMaintenance.fxml"));
            Parent root1 = null;
            try {
                root1 = fxmlLoader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
            RegNumMaintenanceController regNumMaintenanceController = fxmlLoader.getController();
            Stage prStage = new Stage();
            prStage.initModality(Modality.APPLICATION_MODAL);
            prStage.setTitle("Управление гос. номерами");
            Popup popup = createPopup("Транспорт добавлен!", 750, 385);
            regNumMaintenanceController.addVehicleBtn.setOnAction(event -> {
                try {
                    regNumMaintenanceController.addVehicle();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                popup.show(prStage);
                new Timer().schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                Platform.runLater(popup::hide);
                            }
                        },
                        2000
                );
            });

            prStage.setOnCloseRequest(event -> {
                try {
                    regNumMaintenanceController.shutdown();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
            prStage.setScene(new Scene(root1));
            prStage.show();
        });
        stage.setOnCloseRequest(ActionEvent -> {
            try {
                controller.shutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Stopping application");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
