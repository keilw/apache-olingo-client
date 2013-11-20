package org.apache.olingo.odata2.client.fxml;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Proxy;
import java.net.URL;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
import org.apache.olingo.odata2.client.ODataClient;
import org.apache.olingo.odata2.client.StringHelper;

/**
 *
 * @author mibo
 */
public class BasicController implements Initializable {

  @FXML TextField inputArea;
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
  
  //
  private TableView tableView;
  private Task<Void> runningRequest = null;

  @Override
  public void initialize(URL url, ResourceBundle rb) {
//    inputArea.setText("http://www.olingo.org");
//    inputArea.setText("http://localhost:8080/com.sap.core.odata.performance-web/ReferenceScenario.svc/");
    inputArea.setText("http://services.odata.org/Northwind/Northwind.svc/");

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
  public void sendSingleRequest(ActionEvent event) {
    if(runningRequest == null) {
      
      Task<Void> singleRequest = new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          try {
            String serviceUrl = getValidUrl();
            InputStream is = ODataClient.getRawHttpEntity(serviceUrl, ODataClient.APPLICATION_JSON);
            final String content = StringHelper.inputStreamToString(is);

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                rawView.setText(content);
                webView.getEngine().loadContent(content);
                finishRunningRequest();
                singleRequestButton.setText("Single Request");
              }
            });

          } catch (IllegalArgumentException | IOException | HttpException ex) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                failRunningRequest(ex);
                singleRequestButton.setText("Single Request");
              }
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
    if(runningRequest == null) {
      Task<Void> singleRequest = new Task<Void>() {
        @Override
        protected Void call() {
          try {
            final String serviceUrl = getValidUrl();
            final ODataClient client = getODataClient(serviceUrl);
            createEdmView(client);

            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                String rawContent = client.getRawContentOfLastRequest();
                webView.getEngine().loadContent(rawContent);
                rawView.setText(rawContent);
                exploreServiceButton.setText("Explore Service");
                finishRunningRequest();
              }
            });

          } catch (ODataException | IllegalArgumentException | IOException | HttpException ex) {
            Platform.runLater(new Runnable() {
              @Override
              public void run() {
                exploreServiceButton.setText("Explore Service");
                failRunningRequest(ex);
              }
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
    if(runningRequest != null) {
      runningRequest.cancel();
      progress.setProgress(0);
      writeToLogArea("Canceled Running Request");
      runningRequest = null;
      enableRequestButtons();
    }
  }
  
  private void finishRunningRequest() {
    writeToLogArea("All requests successfull processed");
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

  private void createEdmView(ODataClient client) throws ODataException, EdmException, IOException, HttpException {
    entityListView.getItems().clear();
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
      Runnable run = new Runnable() {
        @Override
        public void run() {
          if(runningRequest == null) {
            return;
          }
          entityListView.getItems().add(holder);
          if (progress.getProgress() < 0) {
            progress.setProgress(0);
          }
          progress.setProgress(progress.getProgress() + countStep);
        }
      };
      Platform.runLater(run);
    }
  }

  private String getValidUrl() throws IllegalArgumentException {
    String serviceUrl = inputArea.getText();
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
   * Use of Event to allow processing of
   * <code>MouseEvents</code> as well as
   * <code>ActionEvents</code>.
   *
   * @param event
   */
  @FXML
  public void updateTableView(Event event) {
    try {
      if (tableView != null) {
        edmPane.getItems().remove(tableView);
      }
      ODataFeedItemHolder feed = (ODataFeedItemHolder) entityListView.getSelectionModel().getSelectedItem();
      //      EdmEntityType entityType = edm.getEntityContainer(containerName).getEntitySet(setName).getEntityType();
      tableView = createTable(feed.feed, feed.type);
      edmPane.getItems().add(tableView);
    } catch (EdmException ex) {
      writeToLogArea(ex);
    }

  }

  private boolean isEmpty(String forValidation) {
    return forValidation == null || forValidation.length() == 0;
  }

  private TableView createTable(ODataFeed feed, EdmEntityType edmEntityType) throws EdmException {
    TableView table = new TableView();
    List<String> propertyNames = edmEntityType.getPropertyNames();

    for (String propertyName : propertyNames) {
      TableColumn tc = new TableColumn();
      tc.setText(propertyName);
      tc.setCellValueFactory(new ODataEntryFactory(propertyName));
      table.getColumns().add(tc);
    }

    ObservableList<ODataEntry> values = FXCollections.observableList(feed.getEntries());
    table.setItems(values);

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