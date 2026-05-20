/*
 * ============================================================
 *  PROYECTO – Running la Safor
 *  Asignatura: Interfaces Persona-Computador
 *  Universitat Politècnica de València
 * ============================================================
 *
 *  DESCRIPCIÓN GENERAL
 *  -------------------
 *  Controlador de la vista principal (mapa + lista de actividades).
 *
 *  Funcionalidades implementadas:
 *   1. Carga y visualización de una imagen de mapa.
 *   2. Zoom interactivo mediante un Slider.
 *   3. Importar actividad desde archivo GPX (Issue 6).
 *   4. Lista de actividades del usuario en el ListView.
 *   5. Al seleccionar actividad: carga el mapa correcto y dibuja la ruta (Issue 8).
 *   6. Clic derecho sobre el mapa: menú contextual para anotaciones y círculos.
 *   7. Cerrar sesión y volver al login.
 *
 * ============================================================
 */
package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
import upv.ipc.sportlib.User;

/**
 * Controlador principal: mapa + lista de actividades del usuario.
 */
public class FXMLDocumentController implements Initializable {

    // =========================================================
    //  LIBRERÍA IPC
    // =========================================================

    /** Punto de acceso único a la lógica de negocio (Singleton). */
    private final SportActivityApp app = SportActivityApp.getInstance();

    // =========================================================
    //  ESTRUCTURA DE NODOS PARA ZOOM
    // =========================================================
    //
    //  ScrollPane (map_scrollpane)
    //   └─ contentGroup
    //       └─ zoomGroup  ← se escala para el zoom
    //           └─ mapPane
    //               ├─ ImageView
    //               ├─ Polyline  ← ruta de la actividad
    //               └─ Circle / Text ← anotaciones
    //
    // =========================================================

    private Group zoomGroup;
    private Pane  mapPane;
    private ContextMenu mapContextMenu;
    private boolean insertionMode = false;

    /** Dimensiones reales de la imagen cargada — necesarias para MapProjection. */
    private double mapWidth  = 0;
    private double mapHeight = 0;

    /** Proyección activa — se guarda para usarla al añadir anotaciones (Issue 7). */
    private MapProjection currentProjection = null;

    // =========================================================
    //  ELEMENTOS FXML
    // =========================================================

    @FXML private ListView<Activity> map_listview;
    @FXML private ScrollPane         map_scrollpane;
    @FXML private Slider             zoom_slider;
    @FXML private SplitPane          splitPane;
    @FXML private Label              statusLabel;
    @FXML private Label              usernameLabel;

    // =========================================================
    //  ZOOM
    // =========================================================

    @FXML
    void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.1);
    }

    private void zoom(double scaleValue) {
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    // =========================================================
    //  SELECCIÓN EN EL LISTVIEW → DIBUJA ACTIVIDAD EN EL MAPA
    // =========================================================

    /**
     * Al hacer clic en una actividad de la lista:
     * - Busca el mapa que cubre esa actividad.
     * - Carga el mapa.
     * - Dibuja la ruta (Polyline) sobre él.
     * - Muestra las estadísticas básicas en la barra de estado.
     */
    @FXML
    void listClicked(MouseEvent event) {
        Activity selected = map_listview.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        MapRegion region = app.findMapForActivity(selected);
        if (region != null) {
            buildMap(new File(region.getImagePath()));
            drawActivity(selected, region);
        } else {
            setStatus("No se encontró un mapa para esta actividad.");
            return;
        }

        // Estadísticas básicas en la barra inferior
        double distKm = selected.getTotalDistance() / 1000.0;
        long minutos   = selected.getDuration().toMinutes();
        setStatus(String.format("📍 %s   |   %.2f km   |   %d min",
                selected.getName(), distKm, minutos));
    }

    // =========================================================
    //  DIBUJAR LA RUTA DE UNA ACTIVIDAD
    // =========================================================

    /**
     * Dibuja la ruta de una actividad sobre el mapa.
     *
     * Sigue el enfoque del dossier: itera los TrackPoints uno a uno con
     * proj.project(tp) en lugar de projectActivity(), que puede devolver
     * resultados inesperados según la versión de la librería.
     *
     * También guarda la proyección en currentProjection para usarla
     * al añadir anotaciones (unproject del clic del ratón).
     */
    private void drawActivity(Activity activity, MapRegion region) {
        List<TrackPoint> trackPoints = activity.getTrackPoints();
        if (trackPoints == null || trackPoints.isEmpty()) {
            setStatus("La actividad no tiene puntos GPS.");
            return;
        }

        // Creamos la proyección con las dimensiones reales de la imagen
        currentProjection = new MapProjection(region, mapWidth, mapHeight);

        // ── Polyline de la ruta ────────────────────────────────────────
        Polyline ruta = new Polyline();
        ruta.setStroke(Color.BLUE);
        ruta.setStrokeWidth(3.5);
        ruta.setFill(Color.TRANSPARENT);   // importante: evita relleno negro por defecto

        for (TrackPoint tp : trackPoints) {
            Point2D p = currentProjection.project(tp);
            ruta.getPoints().addAll(p.getX(), p.getY());
        }
        mapPane.getChildren().add(ruta);

        // ── Marcador de inicio (verde) ─────────────────────────────────
        Point2D inicio = currentProjection.project(trackPoints.get(0));
        Circle markerInicio = new Circle(8, Color.LIMEGREEN);
        markerInicio.setStroke(Color.DARKGREEN);
        markerInicio.setStrokeWidth(2);
        markerInicio.setCenterX(inicio.getX());
        markerInicio.setCenterY(inicio.getY());

        // ── Marcador de fin (rojo) ─────────────────────────────────────
        Point2D fin = currentProjection.project(trackPoints.get(trackPoints.size() - 1));
        Circle markerFin = new Circle(8, Color.TOMATO);
        markerFin.setStroke(Color.DARKRED);
        markerFin.setStrokeWidth(2);
        markerFin.setCenterX(fin.getX());
        markerFin.setCenterY(fin.getY());

        mapPane.getChildren().addAll(markerInicio, markerFin);
    }

    // =========================================================
    //  CONSTRUCCIÓN DEL MAPA
    // =========================================================

    /**
     * Carga una imagen y reconstruye la jerarquía de nodos del mapa.
     * Puede llamarse varias veces (al cambiar mapa o seleccionar actividad).
     */
    private void buildMap(File imgFile) {
        if (!imgFile.exists()) {
            map_scrollpane.setContent(
                new Label("Imagen no encontrada: " + imgFile.getPath()));
            return;
        }

        Image img = new Image(imgFile.toURI().toString());
        double W = img.getWidth();
        double H = img.getHeight();
        // Guardamos las dimensiones para usarlas en drawActivity y en anotaciones
        mapWidth  = W;
        mapHeight = H;
        currentProjection = null; // se recalcula al dibujar la actividad

        mapPane = new Pane();
        mapPane.setPrefSize(W, H);
        mapPane.setMinSize(W, H);
        mapPane.setMaxSize(W, H);

        ImageView iv = new ImageView(img);
        iv.setFitWidth(W);
        iv.setFitHeight(H);
        mapPane.getChildren().add(iv);

        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                onMapRightClick(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.PRIMARY && insertionMode) {
                insertionMode = false;
                mapPane.setStyle("");
                addLabel(e.getX(), e.getY());
            }
        });

        zoomGroup = new Group();
        Group contentGroup = new Group();
        zoomGroup.getChildren().add(mapPane);
        contentGroup.getChildren().add(zoomGroup);

        zoomGroup.setScaleX(zoom_slider.getValue());
        zoomGroup.setScaleY(zoom_slider.getValue());

        map_scrollpane.setContent(contentGroup);
    }

    // =========================================================
    //  MENÚ CONTEXTUAL (clic derecho sobre el mapa)
    // =========================================================

    private void onMapRightClick(double x, double y) {
        mapContextMenu.hide();
        mapContextMenu.getItems().get(0).setOnAction(e -> addLabel(x, y));
        mapContextMenu.getItems().get(1).setOnAction(e -> addCircle(x, y));
        mapContextMenu.show(
            mapPane.getScene().getWindow(),
            mapPane.localToScreen(x, y).getX(),
            mapPane.localToScreen(x, y).getY()
        );
    }

    // =========================================================
    //  INICIALIZACIÓN
    // =========================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Slider de zoom
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(2.5);
        zoom_slider.setValue(1.0);
        zoom_slider.valueProperty().addListener(
            (obs, oldVal, newVal) -> zoom((Double) newVal)
        );

        // Menú contextual del mapa
        MenuItem miLabel  = new MenuItem("🏷 Añadir etiqueta");
        MenuItem miCircle = new MenuItem("⭕ Añadir círculo");
        mapContextMenu = new ContextMenu(miLabel, miCircle);

        // CellFactory: muestra el nombre de la actividad
        map_listview.setCellFactory(listView -> new ListCell<Activity>() {
            @Override
            protected void updateItem(Activity activity, boolean empty) {
                super.updateItem(activity, empty);
                if (empty || activity == null) {
                    setText(null);
                } else {
                    setText(activity.getName());
                }
            }
        });

        // Nombre del usuario logueado
        User currentUser = app.getCurrentUser();
        if (currentUser != null) {
            usernameLabel.setText("👤 " + currentUser.getNickName());
            // Cargar actividades del usuario en el ListView
            List<Activity> actividades = app.getUserActivities();
            if (actividades != null) {
                map_listview.getItems().addAll(actividades);
            }
        }

        // Mapa inicial
        buildMap(new File("maps/upv.jpg"));
    }

    // =========================================================
    //  IMPORTAR GPX (Issue 6)
    // =========================================================

    /**
     * Abre un selector de archivo .gpx, lo importa con la librería IPC
     * y añade la actividad resultante al ListView.
     * Si la librería encuentra un mapa adecuado, lo carga y dibuja la ruta.
     */
    @FXML
    private void importarGPX(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo GPX");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos GPX", "*.gpx")
        );
        fc.setInitialDirectory(new File("."));

        File gpxFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (gpxFile == null) return;

        Activity activity = app.importActivity(gpxFile);
        if (activity != null) {
            map_listview.getItems().add(activity);
            map_listview.getSelectionModel().select(activity);

            MapRegion region = app.findMapForActivity(activity);
            if (region != null) {
                buildMap(new File(region.getImagePath()));
                drawActivity(activity, region);
                double distKm = activity.getTotalDistance() / 1000.0;
                setStatus(String.format("✓ GPX importado: %s   |   %.2f km",
                        activity.getName(), distKm));
            } else {
                setStatus("✓ GPX importado: " + activity.getName() +
                        " (no se encontró mapa para esta zona)");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al importar");
            alert.setHeaderText("No se pudo importar el archivo GPX");
            alert.setContentText("Comprueba que el archivo es un GPX válido.");
            alert.showAndWait();
        }
    }

    // =========================================================
    //  CERRAR SESIÓN
    // =========================================================

    /**
     * Llama a app.logout() para limpiar la sesión y navega al login.
     */
    @FXML
    private void cerrarSesion(ActionEvent event) {
        app.logout();
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/login/FXMLLogin.fxml"));
            Pane pane = loader.load();
            usernameLabel.getScene().setRoot(pane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    //  CAMBIAR MAPA
    // =========================================================

    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (imgFile != null) {
            buildMap(imgFile);
            map_listview.getSelectionModel().clearSelection();
            setStatus("Mapa cambiado: " + imgFile.getName());
        }
    }

    // =========================================================
    //  DIÁLOGO "ACERCA DE"
    // =========================================================

    @FXML
    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
            new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );
        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("Running la Safor – IPC 2026");
        mensaje.setContentText("Aplicación de seguimiento de actividades deportivas.");
        mensaje.showAndWait();
    }

    // =========================================================
    //  AÑADIR ETIQUETA DE TEXTO AL MAPA (clic derecho)
    // =========================================================

    private void addLabel(double x, double y) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Nueva etiqueta");
        dialog.setHeaderText("Introduce el texto");

        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Texto de la etiqueta");
        dialog.getDialogPane().setContent(new VBox(8, new Label("Texto:"), nameField));

        dialog.setResultConverter(btn -> btn == okButton ? nameField.getText().trim() : null);

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(texto -> {
            if (!texto.isEmpty()) {
                Text label = new Text(texto);
                label.setX(x);
                label.setY(y);
                label.setFill(Color.DARKBLUE);
                mapPane.getChildren().add(label);
            }
        });
    }

    // =========================================================
    //  AÑADIR CÍRCULO AL MAPA (clic derecho)
    // =========================================================

    private void addCircle(double x, double y) {
        Circle circle = new Circle(10, Color.RED);
        circle.setOpacity(0.65);
        circle.setCenterX(x);
        circle.setCenterY(y);
        mapPane.getChildren().add(circle);
    }

    // =========================================================
    //  BARRA DE ESTADO
    // =========================================================

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }
}
