/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olingo.odata2.client.fxml;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.apache.olingo.odata2.client.ODataClient;

/**
 * FXML Controller class
 *
 * @author d046871
 */
public class CreateController implements Initializable {

  @FXML Node root;

  @FXML Button createButton;
  @FXML VBox labelBox;
  @FXML VBox textBox;

  private EdmEntityType entityType;
  private ODataClient client;
  private boolean closed = false;
  private Stage viewStage;
  private String entitySetName;
  private Map<String, TextField> name2Input = new HashMap<>();

  public void init(ODataClient client, String entitySetName, EdmEntityType entityType) throws EdmException {
    this.entityType = entityType;
    this.client = client;
    this.entitySetName = entitySetName;

    List<String> propertyNames = entityType.getPropertyNames();
    for (String name : propertyNames) {
      EdmProperty property = (EdmProperty) entityType.getProperty(name);
              
      Label label = new Label(name + " (" + property.getType().getName() + "):");
      label.setAlignment(Pos.CENTER_RIGHT);
      label.setTextAlignment(TextAlignment.RIGHT);
      label.setMinHeight(20);
      label.setPrefHeight(30);
      label.setPrefWidth(200);
      
      TextField text = new TextField();
      text.setMinHeight(20);
      text.setPrefHeight(30);
      
      labelBox.getChildren().add(label);
      textBox.getChildren().add(text);

      name2Input.put(name, text);
    }
  }

  public Node getRoot() {
    return root;
  }

  public boolean isClosed() {
    return closed;
  }

  /**
   * Initializes the controller class.
   */
  @Override
  public void initialize(URL url, ResourceBundle rb) {
    // TODO
  }

  @FXML
  public void createEntity(ActionEvent event) {
    try {
      Map<String, Object> content = getContentMap();
      client.postEntity(this.entitySetName, content);

      closeStage();
    } catch (EdmException ex) {
      Logger.getLogger(CreateController.class.getName()).log(Level.SEVERE, null, ex);
      throw new RuntimeException("Error in entity creation.", ex);
    }
  }
  
  private Map<String, Object> getContentMap() throws EdmException {
    Map<String, Object> content = new HashMap<>();

    
    Set<Map.Entry<String, TextField>> entries = name2Input.entrySet();
    for (Map.Entry<String, TextField> entry : entries) {
      EdmProperty property = (EdmProperty) entityType.getProperty(entry.getKey());
      
      String text = entry.getValue().getText();
      Object value = text2Value(property, text);
      content.put(entry.getKey(), value);
    }

    return content;
  }

  @FXML
  public void cancelCreation(ActionEvent event) {
    closeStage();
  }
  
  private void closeStage() {
    viewStage.close();
    closed = true;
  }

  public void show() {
    try {
//      CreateController vc = l.getController();
      Scene viewScene = new Scene(getRoot().getParent());
      viewStage = new Stage(StageStyle.UNDECORATED);
      viewStage.setScene(viewScene);
      viewStage.show();
    } catch (Exception ex) {
      Logger.getLogger(CreateController.class.getName()).log(Level.SEVERE, null, ex);
      throw new RuntimeException("Error in preview pane creation", ex);
    }
  }

  private Object text2Value(EdmProperty property, String text) throws EdmException {
      
      final Object result;
      if(text != null && text.isEmpty()) {
        result = null;
      } else {
        EdmType propertyType = property.getType();
        if(property.isSimple()) {
          String propertyTypeName = propertyType.getName();
          if("String".equals(propertyTypeName)) {
            result = text;
          } else if("DateTime".equals(propertyTypeName)) {
            result = text2Date(text);
          } else if("Int32".equals(propertyTypeName)) {
            result = Integer.valueOf(text);
          } else if("Int64".equals(propertyTypeName)) {
            result = Long.valueOf(text);
          } else if("Double".equals(propertyTypeName)) {
            result = Double.valueOf(text);
          } else {
            result = null;
          }
        } else {
          result = null;
        }
      }
      
      return result;
  }

  private Date text2Date(String text) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      return sdf.parse(text);
    } catch (ParseException ex) {
      return new Date();
    }
  }
}
