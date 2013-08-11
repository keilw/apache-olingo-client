package org.apache.olingo.odata2.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

  public static void main(String[] args) throws Exception {
    launch(args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("fxml/Basic.fxml"));

    Scene scene = new Scene(root);

    stage.setTitle("Olingo Sample Client - OData-v2");
    stage.setScene(scene);
    stage.show();
  }
}
