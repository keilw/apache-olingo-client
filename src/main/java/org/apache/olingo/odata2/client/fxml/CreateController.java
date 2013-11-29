/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olingo.odata2.client.fxml;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.edm.EdmType;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.client.ODataClient;

/**
 * FXML Controller class
 *
 * @author d046871
 */
public class CreateController implements Initializable {

  @FXML Node root;

  @FXML Button submitButton;
  @FXML VBox labelBox;
  @FXML VBox textBox;

  private String entityKeyForUri;
  private EdmEntityType entityType;
  private ODataClient client;
  private boolean closed = false;
  private Stage viewStage;
  private String entitySetName;
  private Method method = Method.POST;
  private Map<String, TextField> name2Input = new HashMap<>();

  enum Method {PUT, POST, DELETE};
  
  void initPut(ODataClient client, String entitySetName, EdmEntityType entityType, ODataEntry oDataEntry) throws EdmException {
    this.entitySetName = entitySetName;
    this.entityType = entityType;
    this.client = client;
    this.method = Method.PUT;
    this.submitButton.setText("Update");

    Map<String, Object> oDataEntries = oDataEntry.getProperties();
    List<String> propertyNames = entityType.getPropertyNames();
    for (String name : propertyNames) {
      EdmProperty property = (EdmProperty) entityType.getProperty(name);
      Object value = oDataEntries.get(property.getName());

      addProperty(entityType, name, value2Text(value));
    }
    
    initEntityKeyForUri(entityType, oDataEntries);    
  }

  private void initEntityKeyForUri(EdmEntityType entityType, Map<String, Object> oDataEntries) throws EdmException {
    EdmProperty prop = entityType.getKeyProperties().get(0);
    String keyName = prop.getName();
    Object keyValue = oDataEntries.get(keyName);
    if("String".equals(prop.getType().getName())) {
      entityKeyForUri = "('" + value2Text(keyValue) + "')";
    } else {
      entityKeyForUri = "(" + value2Text(keyValue) + ")";
    }
  }

  public void initPost(ODataClient client, String entitySetName, EdmEntityType entityType) throws EdmException {
    this.entityType = entityType;
    this.client = client;
    this.entitySetName = entitySetName;
    this.method = Method.POST;
    this.submitButton.setText("Create");

    List<String> propertyNames = entityType.getPropertyNames();
    for (String name : propertyNames) {
      addProperty(entityType, name);
    }
  }

  private void addProperty(EdmEntityType entityType, String name) throws EdmException {
    addProperty(entityType, name, "");
  }
  
  private void addProperty(EdmEntityType entityType, String name, String value) throws EdmException {
    EdmProperty property = (EdmProperty) entityType.getProperty(name);
    final String propertyType = property.getType().getName();
    
    Label label = new Label(name + " (" + propertyType + "):");
    label.setAlignment(Pos.CENTER_RIGHT);
    label.setTextAlignment(TextAlignment.RIGHT);
    label.setMinHeight(20);
    label.setPrefHeight(30);
    label.setPrefWidth(200);
    
    final TextField text = new TextField(value);
    text.setMinHeight(20);
    text.setPrefHeight(30);
    if(value == null || value.isEmpty()) {
      text.setPromptText("Enter " + propertyType + " value here.");
    }
    
    labelBox.getChildren().add(label);
    textBox.getChildren().add(text);
    
    name2Input.put(name, text);
  }
  
  private void addProperty(String name, String value) {
    
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
      switch (method) {
        case POST:
          client.postEntity(entitySetName, content);
          break;
        case PUT:
          client.putEntity(entitySetName, entityKeyForUri, content);
          break;
      }

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

  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private Date text2Date(String text) {
    try {
      return sdf.parse(text);
    } catch (ParseException ex) {
      return new Date();
    }
  }
  
  private String value2Text(Object value) {
    if(value instanceof Date) {
      return date2Text((Date) value);
    } else if(value instanceof Calendar) {
      return date2Text(((Calendar)value).getTime());
    }
    return String.valueOf(value);
  }
          
  private String date2Text(Date date) {
    return sdf.format(date);
  }
}
