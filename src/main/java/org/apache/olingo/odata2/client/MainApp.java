package org.apache.olingo.odata2.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);
  private final Properties properties;
  
  public static void main(String[] args) throws Exception {
    launch(args);
  }

  public MainApp() {
    properties = new Properties();
    
    try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("client.properties");) {
      properties.load(inputStream);
    } catch (IOException e) {
      LOG.error("Failure during properties initialization with message: {}.", e.getMessage());
    }
  }

  
  
  @Override
  public void start(Stage stage) throws Exception {
    Parent root = FXMLLoader.load(getClass().getResource("fxml/Basic.fxml"));

    Scene scene = new Scene(root);

    String buildVersion = properties.getProperty("build.version");
    String title = "Olingo Sample Client - OData-v2 - " + buildVersion;
    if(!buildVersion.contains("SNAPSHOT")) {
      title = title.substring(0, title.lastIndexOf("-"));
    }
    stage.setTitle(title);
    stage.setScene(scene);
    stage.show();
  }
}
