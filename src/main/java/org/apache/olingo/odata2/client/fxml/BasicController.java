package org.apache.olingo.odata2.client.fxml;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntitySetInfo;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.client.HttpException;
import org.apache.olingo.odata2.client.MainApp;
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;

import java.io.*;
import java.net.Proxy;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class BasicController implements Initializable {

  @FXML ComboBox<String> uriSelector;
  @FXML TextArea rawView;
  @FXML WebView webView;
  @FXML SplitPane edmPane;
  @FXML TextArea logArea;
  @FXML ListView entityListView;
  @FXML ProgressIndicator progress;
  @FXML CheckBox proxyCheckbox;
  @FXML TextField proxyHost;
  @FXML TextField proxyPort;
  @FXML CheckBox loginCheckbox;
  @FXML TextField loginUser;
  @FXML PasswordField loginPassword;
  @FXML Button singleRequestButton;
  @FXML Button exploreServiceButton;
//  @FXML AnchorPane createPane;

  //
  private CreateController createController;

  private TableView tableView;
  private Task<Void> runningRequest = null;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
    ObservableList<String> items = FXCollections.observableArrayList();
    items.add("http://www.olingo.org");
    items.add("http://localhost:8080/MyFormula.svc/");
    items.add("http://services.odata.org/Northwind/Northwind.svc/");

    uriSelector.setItems(items);
    uriSelector.getSelectionModel().selectLast();
  }

  @FXML
  public void changeProxy(ActionEvent event) {
    if (proxyCheckbox.isSelected()) {
      proxyHost.setDisable(false);
      proxyPort.setDisable(false);
    } else {
      proxyHost.setDisable(true);
      proxyPort.setDisable(true);
    }
  }

  @FXML
  public void changeLogin(ActionEvent event) {
    if (loginCheckbox.isSelected()) {
      loginUser.setDisable(false);
      loginPassword.setDisable(false);
    } else {
      loginUser.setDisable(true);
      loginPassword.setDisable(true);
    }
  }

  
  @FXML
  public void createEntity(MouseEvent e) {
    if(e.getClickCount() > 1) {
      if(createController == null || createController.isClosed()) {
        Object value = entityListView.getSelectionModel().getSelectedItem();
        if(value instanceof ODataFeedItemHolder) {
          ODataFeedItemHolder holder = (ODataFeedItemHolder) value;
          createController = createEntityCreateDialog(holder);
        }
      }
    }
  }
  
  private CreateController createEntityCreateDialog(ODataFeedItemHolder holder) {
    try {
      CreateController create = createCreateController();
      
      String serviceUrl = getValidUrl();
      ODataClient client = getODataClient(serviceUrl);

      create.initPost(client, holder.name, holder.type);
      create.show();

      return create;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error in preview pane creation", ex);
    }
  }

  private CreateController createEntityUpdateDialog(String entitySetName, EdmEntityType edmEntityType, ODataEntry oDataEntry) {
    try {
      CreateController create = createCreateController();
      
      String serviceUrl = getValidUrl();
      ODataClient client = getODataClient(serviceUrl);

      create.initPut(client, entitySetName, edmEntityType, oDataEntry);
      create.show();

      return create;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new RuntimeException("Error in preview pane creation", ex);
    }
  }

  private CreateController createCreateController() throws IOException {
      FXMLLoader l = new FXMLLoader(MainApp.class.getResource("fxml/Create.fxml"));
      l.load();
      return l.getController();
  }
  
  @FXML
  public void sendSingleRequest(ActionEvent event) {
    if (runningRequest == null) {

      Task<Void> singleRequest = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          try {
            String serviceUrl = getValidUrl();
            InputStream is = ODataClient.getRawHttpEntity(serviceUrl, ODataClient.APPLICATION_JSON);
            final String content = StringHelper.inputStreamToString(is);

            Platform.runLater(() -> {
              rawView.setText(content);
              webView.getEngine().loadContent(content);
              finishRunningRequest();
              singleRequestButton.setText("Single Request");
            });

          } catch (IllegalArgumentException | IOException | HttpException ex) {
            Platform.runLater(() -> {
              failRunningRequest(ex);
              singleRequestButton.setText("Single Request");
            });
          }
          return null;
        }
      };

      clearViews();
      singleRequestButton.setText("Cancel");
      exploreServiceButton.setDisable(true);
      runningRequest = singleRequest;
      new Thread(singleRequest).start();
    } else {
      cancelRunningRequest();
      singleRequestButton.setText("Single Request");
    }

  }

  @FXML
  public void exploreService(ActionEvent event) {
    if (runningRequest == null) {
      Task<Void> singleRequest = new Task<Void>() {
        @Override
        protected Void call() {
          try {
            final String serviceUrl = getValidUrl();
            final ODataClient client = getODataClient(serviceUrl);
            createEdmView(client);

            Platform.runLater(() -> {
              String rawContent = client.getRawContentOfLastRequest();
              webView.getEngine().loadContent(rawContent);
              rawView.setText(rawContent);
              exploreServiceButton.setText("Explore Service");
              finishRunningRequest();
            });

          } catch (ODataException | IllegalArgumentException | IOException | HttpException ex) {
            Platform.runLater(() -> {
              exploreServiceButton.setText("Explore Service");
              failRunningRequest(ex);
            });
          }
          return null;
        }
      };

      clearViews();
      exploreServiceButton.setText("Cancel");
      singleRequestButton.setDisable(true);
      runningRequest = singleRequest;
      new Thread(singleRequest).start();
    } else {
      cancelRunningRequest();
      exploreServiceButton.setText("Explore Service");
    }
  }

  private void cancelRunningRequest() {
    if (runningRequest != null) {
      runningRequest.cancel();
      progress.setProgress(0);
      writeToLogArea("Canceled Running Request");
      runningRequest = null;
      enableRequestButtons();
    }
  }

  private void finishRunningRequest() {
    writeToLogArea("All requests successful processed");
    progress.setProgress(1);
    runningRequest = null;
    enableRequestButtons();
  }

  private void failRunningRequest(Exception ex) {
    progress.setProgress(0);
    writeToLogArea(ex);
    runningRequest = null;
    enableRequestButtons();
  }

  private void enableRequestButtons() {
    singleRequestButton.setDisable(false);
    exploreServiceButton.setDisable(false);
  }

  private ODataClient getODataClient(final String serviceUrl) throws ODataException, IOException, HttpException {
    if (proxyCheckbox.isSelected() && loginCheckbox.isSelected()) {
      return new ODataClient(serviceUrl, Proxy.Type.HTTP, proxyHost.getText(), Integer.valueOf(proxyPort.getText()),
              loginUser.getText(), loginPassword.getText());
    } else if (proxyCheckbox.isSelected()) {
      return new ODataClient(serviceUrl, Proxy.Type.HTTP, proxyHost.getText(), Integer.valueOf(proxyPort.getText()));
    } else if (loginCheckbox.isSelected()) {
      return new ODataClient(serviceUrl, loginUser.getText(), loginPassword.getText());
    }
    return new ODataClient(serviceUrl);
  }

  private void clearViews() {
    logArea.setText("");
    webView.getEngine().loadContent("");
    rawView.setText("");
    progress.setProgress(-1);
    entityListView.getItems().clear();
  }

  private void writeToLogArea(Exception ex) {
    StringBuilder b = new StringBuilder();
    Writer out = new StringWriter();
    PrintWriter pw = new PrintWriter(out, true);
    ex.printStackTrace(pw);
    b.append("Exception occured: ").append(ex.getMessage()).append("\n").append(out.toString());
    writeToLogArea(b.toString());
  }

  private void writeToLogArea(String content) {
    writeToLogArea(content, true);
  }

  private void writeToLogArea(String content, boolean append) {
    final StringBuilder b;
    if (append) {
      b = new StringBuilder(logArea.getText());
    } else {
      b = new StringBuilder();
    }
    b.append("--- ").append(DateFormat.getDateTimeInstance().format(new Date())).append("--------\n");
    b.append(content);
    b.append("\n--------------------------");
    logArea.setText(b.toString());
  }

  private void createEdmView(ODataClient client) throws ODataException, IOException, HttpException {
    entityListView.getItems().clear();
//    entityListView.setEditable(true);
    entityListView.setCellFactory(param -> new ODataFeedCell());
    
    List<EdmEntitySetInfo> entitySets = client.getEntitySets();
    final double countStep = 1d / entitySets.size();

    for (EdmEntitySetInfo edmEntitySetInfo : entitySets) {
      String containerName = edmEntitySetInfo.getEntityContainerName();
      String setName = edmEntitySetInfo.getEntitySetName();

      ODataFeed feed = client.readFeed(containerName, setName, ODataClient.APPLICATION_ATOM_XML);
      writeToLogArea(client.getRawContentOfLastRequest());
      Edm edm = client.getEdm();
      EdmEntityType entityType = edm.getEntityContainer(containerName).getEntitySet(setName).getEntityType();

      final ODataFeedItemHolder holder = new ODataFeedItemHolder(feed, entityType, setName);
      Runnable run = () -> {
        if (runningRequest == null) {
          return;
        }
        entityListView.getItems().add(holder);
        if (progress.getProgress() < 0) {
          progress.setProgress(0);
        }
        progress.setProgress(progress.getProgress() + countStep);
      };
      Platform.runLater(run);
    }
  }
  
  private class ODataFeedCell extends ListCell<ODataFeedItemHolder> {
    HBox hbox = new HBox();
    Label label = new Label("(empty)");
    Pane pane = new Pane();
    Button refreshButton = new Button("Refresh");
    Button createButton = new Button("Create");
    ODataFeedItemHolder lastItem;

    public ODataFeedCell() {
      super();
      hbox.getChildren().addAll(label, pane, refreshButton, createButton);
      HBox.setHgrow(pane, Priority.ALWAYS);
      
      refreshButton.setOnAction(event -> refreshODataFeed(lastItem));

      createButton.setOnAction(event -> createEntityCreateDialog(lastItem));
    }

    @Override
    protected void updateItem(ODataFeedItemHolder item, boolean empty) {
      super.updateItem(item, empty);
      setText(null);  // No text in label of super class
      if (empty) {
        lastItem = null;
        setGraphic(null);
      } else {
        lastItem = item;
        label.setText(item != null ? item.name : "<null>");
        setGraphic(hbox);
      }
    }
  }

  private void refreshODataFeed(ODataFeedItemHolder lastItem) {
    try {
      String setName = lastItem.name;
      final ODataClient client = getODataClient(getValidUrl());
      List<EdmEntitySetInfo> entitySets = client.getEntitySets();
      EdmEntitySetInfo edmEntitySetInfo = null;
      for (EdmEntitySetInfo info : entitySets) {
        if(info.getEntitySetName().equals(setName)) {
          edmEntitySetInfo = info;
        }
      }
      if(edmEntitySetInfo != null) {
        String containerName = edmEntitySetInfo.getEntityContainerName();

        ODataFeed feed = client.readFeed(containerName, setName, ODataClient.APPLICATION_ATOM_XML);
        updateTableView(new ODataFeedItemHolder(feed, lastItem.type, setName));
      }
    } catch (Exception ex) {
      Logger.getLogger(BasicController.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private String getValidUrl() throws IllegalArgumentException {
    String serviceUrl = uriSelector.getSelectionModel().getSelectedItem();
    if (isEmpty(serviceUrl)) {
      throw new IllegalArgumentException("No url given.");
    }
    return serviceUrl;
  }

  private class ODataFeedItemHolder {

    private final ODataFeed feed;
    private final EdmEntityType type;
    private final String name;

    public ODataFeedItemHolder(ODataFeed feed, EdmEntityType type, String name) {
      this.feed = feed;
      this.type = type;
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Use of Event to allow processing of <code>MouseEvents</code> as well as <code>ActionEvents</code>.
   *
   * @param event
   */
  @FXML
  public void updateTableView(Event event) {
    ODataFeedItemHolder feed = (ODataFeedItemHolder) entityListView.getSelectionModel().getSelectedItem();
    updateTableView(feed);
  }

  private void updateTableView(ODataFeedItemHolder feed) {
    try {
      if(feed != null) {
        if (tableView != null) {
          edmPane.getItems().remove(tableView);
        }
        tableView = createTable(feed);
        edmPane.getItems().add(tableView);
      }
    } catch (EdmException ex) {
      writeToLogArea(ex);
    }
  }

  
  private boolean isEmpty(String forValidation) {
    return forValidation == null || forValidation.length() == 0;
  }

  private TableView createTable(final ODataFeedItemHolder feedItem) throws EdmException {
    final TableView table = new TableView();
    List<String> propertyNames = feedItem.type.getPropertyNames();

    for (String propertyName : propertyNames) {
      TableColumn tc = new TableColumn();
      tc.setText(propertyName);
      tc.setCellValueFactory(new ODataEntryFactory(propertyName));
      table.getColumns().add(tc);
    }

    ObservableList<ODataEntry> values = FXCollections.observableList(feedItem.feed.getEntries());
    table.setItems(values);

    table.setEditable(true);
    table.setOnMouseClicked((MouseEvent t) -> {
        if (t.getClickCount() > 1) {
          Object selectedItem = table.getSelectionModel().getSelectedItem();
          createEntityUpdateDialog(feedItem.name, feedItem.type, (ODataEntry) selectedItem);
        }
    });
    return table;
  }

  /**
   *
   */
  private class ODataEntryFactory implements Callback<TableColumn.CellDataFeatures<ODataEntry, String>, ObservableValue<String>> {

    private final String property;

    public ODataEntryFactory(String property) {
      this.property = property;
    }

    @Override
    public ObservableValue<String> call(TableColumn.CellDataFeatures<ODataEntry, String> p) {
      ODataEntry ode = p.getValue();
      Object object = ode.getProperties().get(this.property);
      if (object == null) {
        object = "NULL";
      } else if (object instanceof Calendar) {
        object = DateFormat.getDateTimeInstance().format(((Calendar) object).getTime());
      } else if (object instanceof Map) {
        object = "(complex) " + object.toString();
      }
      return new SimpleStringProperty(String.valueOf(object));
    }
  };
}
