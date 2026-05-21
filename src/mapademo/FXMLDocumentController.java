package mapademo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import upv.ipc.sportlib.Activity;
import upv.ipc.sportlib.Annotation;
import upv.ipc.sportlib.AnnotationType;
import upv.ipc.sportlib.GeoPoint;
import upv.ipc.sportlib.MapProjection;
import upv.ipc.sportlib.MapRegion;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
import upv.ipc.sportlib.User;

public class FXMLDocumentController implements Initializable {

    private final SportActivityApp app = SportActivityApp.getInstance();

    private Group         zoomGroup;
    private Pane          mapPane;
    private ContextMenu   mapContextMenu;

    private double mapWidth  = 0;
    private double mapHeight = 0;
    private MapProjection currentProjection = null;

    // estado anotaciones
    private Activity       selectedActivity      = null;
    private AnnotationType pendingAnnotationType = null;
    private GeoPoint       firstAnnotationPoint  = null;
    private double         rightClickX, rightClickY;

    @FXML private ListView<Activity> map_listview;
    @FXML private ScrollPane         map_scrollpane;
    @FXML private Slider             zoom_slider;
    @FXML private SplitPane          splitPane;
    @FXML private Label              statusLabel;
    @FXML private Label              usernameLabel;

    // ── Zoom ──────────────────────────────────────────────────

    @FXML void zoomIn(ActionEvent event)  { zoom_slider.setValue(zoom_slider.getValue() + 0.1); }
    @FXML void zoomOut(ActionEvent event) { zoom_slider.setValue(zoom_slider.getValue() - 0.1); }

    private void zoom(double scale) {
        double h = map_scrollpane.getHvalue();
        double v = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(scale);
        zoomGroup.setScaleY(scale);
        map_scrollpane.setHvalue(h);
        map_scrollpane.setVvalue(v);
    }

    // ── Lista de actividades ──────────────────────────────────

    @FXML
    void listClicked(MouseEvent event) {
        Activity selected = map_listview.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        selectedActivity = selected;

        MapRegion region = app.findMapForActivity(selected);
        if (region == null) { setStatus("No se encontró mapa para esta actividad."); return; }

        buildMap(new File(region.getImagePath()));
        drawActivity(selected, region);
        for (Annotation ann : selected.getAnnotations()) drawAnnotation(ann);

        double distKm = selected.getTotalDistance() / 1000.0;
        long totalSegundos = selected.getDuration().toSeconds();
        long min = totalSegundos / 60;
        long seg = totalSegundos % 60;

        double velMedia = selected.getAverageSpeed();
        double ritmoMedio = selected.getAveragePace(); 
        double desPlus = selected.getElevationGain();
        double desMinus = selected.getElevationLoss();
        double altMin = selected.getMinElevation();
        double altMax = selected.getMaxElevation();

        setStatus(String.format(
            "📍 %s | 📏 %.2f km | ⏱ %d:%02d min | 🚀 Vel. Media: %.1f km/h (Ritmo: %.2f min/km) | 📈 Desn+: %.0fm Desn-: %.0fm | 🏔 Alt: %.0fm - %.0fm",
            selected.getName(), distKm, min, seg, velMedia, ritmoMedio, desPlus, desMinus, altMin, altMax
        ));
    }

    // ── Dibujar ruta ─────────────────────────────────────────

    private void drawActivity(Activity activity, MapRegion region) {
        List<TrackPoint> pts = activity.getTrackPoints();
if (pts == null || pts.isEmpty()) { 
    setStatus("La actividad no tiene puntos GPS."); 
    return; 
}

currentProjection = new MapProjection(region, mapWidth, mapHeight);

for (int i = 0; i < pts.size() - 1; i++) {
    TrackPoint actual = pts.get(i);
    TrackPoint siguiente = pts.get(i + 1);

    Point2D p1 = currentProjection.project(actual);
    Point2D p2 = currentProjection.project(siguiente);

    Line segmento = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    segmento.setStrokeWidth(4.0);

    double velocidadTramo = actual.speedTo(siguiente); 

    if (velocidadTramo < 6.0) {
        segmento.setStroke(Color.TOMATO);          
    } else if (velocidadTramo < 12.0) {
        segmento.setStroke(Color.ORANGE);          
    } else {
        segmento.setStroke(Color.LIMEGREEN);       
    }

    mapPane.getChildren().add(segmento);
}

Point2D inicio = currentProjection.project(pts.get(0));
Circle ci = new Circle(8, Color.GREEN);
ci.setStroke(Color.WHITE); ci.setStrokeWidth(2);
ci.setCenterX(inicio.getX()); ci.setCenterY(inicio.getY());

Point2D fin = currentProjection.project(pts.get(pts.size() - 1));
Circle cf = new Circle(8, Color.RED);
cf.setStroke(Color.WHITE); cf.setStrokeWidth(2);
cf.setCenterX(fin.getX()); cf.setCenterY(fin.getY());

mapPane.getChildren().addAll(ci, cf);

double distKm = activity.getTotalDistance() / 1000.0;
long totalSegundos = activity.getDuration().toSeconds();
long min = totalSegundos / 60;
long seg = totalSegundos % 60;

double velMedia = activity.getAverageSpeed();
double ritmoMedio = activity.getAveragePace(); 
double desPlus = activity.getElevationGain();
double desMinus = activity.getElevationLoss();
double altMin = activity.getMinElevation();
double altMax = activity.getMaxElevation();

setStatus(String.format(
    "📍 %s | 📏 %.2f km | ⏱ %d:%02d min | 🚀 Vel. Media: %.1f km/h (Ritmo: %.2f min/km) | 📈 Desn+: %.0fm Desn-: %.0fm | 🏔 Alt: %.0fm - %.0fm",
    activity.getName(), distKm, min, seg, velMedia, ritmoMedio, desPlus, desMinus, altMin, altMax
));
    }

    // ── Anotaciones ──────────────────────────────────────────

    private void startAnnotation(AnnotationType type) {
        if (type == AnnotationType.POINT || type == AnnotationType.TEXT) {
            GeoPoint geo = currentProjection.unproject(rightClickX, rightClickY);
            showAnnotationDialog(type, Arrays.asList(geo));
        } else {
            pendingAnnotationType = type;
            firstAnnotationPoint  = null;
            mapPane.setCursor(Cursor.CROSSHAIR);
            String nombre = (type == AnnotationType.LINE) ? "línea" : "círculo";
            setStatus("Clic en el primer punto de la " + nombre + " (1/2)…");
        }
    }

    private void handleAnnotationClick(double x, double y) {
        GeoPoint geo = currentProjection.unproject(x, y);
        if (firstAnnotationPoint == null) {
            firstAnnotationPoint = geo;
            setStatus("Punto 1 fijado. Clic en el segundo punto (2/2)…");
        } else {
            List<GeoPoint> points = Arrays.asList(firstAnnotationPoint, geo);
            AnnotationType type   = pendingAnnotationType;
            pendingAnnotationType = null;
            firstAnnotationPoint  = null;
            mapPane.setCursor(Cursor.DEFAULT);
            showAnnotationDialog(type, points);
        }
    }

    private void showAnnotationDialog(AnnotationType type, List<GeoPoint> geoPoints) {
        Dialog<Annotation> dialog = new Dialog<>();
        dialog.setTitle("Nueva anotación");
        dialog.setHeaderText(switch (type) {
            case POINT -> "Punto"; case TEXT -> "Texto";
            case LINE  -> "Línea"; case CIRCLE -> "Círculo";
        });

        ButtonType ok = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        TextField   textField = new TextField();
        textField.setPromptText("Texto (opcional)");
        ColorPicker colorPicker = new ColorPicker(Color.web("#E74C3C"));

        VBox content = new VBox(8, new Label("Texto:"), textField, new Label("Color:"), colorPicker);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> btn == ok
            ? new Annotation(type, textField.getText().trim(), colorToHex(colorPicker.getValue()), 2.5, geoPoints)
            : null);

        dialog.showAndWait().ifPresent(ann -> {
            Annotation saved = app.addAnnotation(selectedActivity, ann);
            if (saved != null) { drawAnnotation(saved); setStatus("✓ Anotación guardada."); }
            else                { setStatus("✗ No se pudo guardar la anotación."); }
        });
    }

    private void drawAnnotation(Annotation ann) {
        Color color;
        try { color = Color.web(ann.getColor()); } catch (Exception e) { color = Color.RED; }

        List<GeoPoint> pts = ann.getGeoPoints();
        if (pts == null || pts.isEmpty() || currentProjection == null) return;

        switch (ann.getType()) {

            case POINT -> {
                Point2D p = currentProjection.project(pts.get(0));
                Circle c = new Circle(7, color);
                c.setStroke(color.darker()); c.setStrokeWidth(1.5);
                c.setCenterX(p.getX()); c.setCenterY(p.getY());
                Group g = new Group(c);
                if (!ann.getText().isEmpty()) {
                    Text t = new Text(ann.getText());
                    t.setX(p.getX() + 10); t.setY(p.getY() + 4);
                    t.setFill(color);
                    g.getChildren().add(t);
                }
                g.setUserData(ann);
                mapPane.getChildren().add(g);
            }

            case TEXT -> {
                Point2D p = currentProjection.project(pts.get(0));
                Text t = new Text(ann.getText());
                t.setX(p.getX()); t.setY(p.getY());
                t.setFill(color);
                t.setUserData(ann);
                mapPane.getChildren().add(t);
            }

            case LINE -> {
                if (pts.size() < 2) break;
                Point2D p1 = currentProjection.project(pts.get(0));
                Point2D p2 = currentProjection.project(pts.get(1));
                Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                line.setStroke(color);
                line.setStrokeWidth(ann.getStrokeWidth());
                line.setUserData(ann);
                mapPane.getChildren().add(line);
            }

            case CIRCLE -> {
                if (pts.size() < 2) break;
                // Los dos puntos definen el diámetro; el centro es su punto medio
                Point2D p1 = currentProjection.project(pts.get(0));
                Point2D p2 = currentProjection.project(pts.get(1));
                double cx     = (p1.getX() + p2.getX()) / 2.0;
                double cy     = (p1.getY() + p2.getY()) / 2.0;
                double radius = Math.hypot(p2.getX() - p1.getX(), p2.getY() - p1.getY()) / 2.0;
                Circle c = new Circle(radius);
                c.setCenterX(cx); c.setCenterY(cy);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(color);
                c.setStrokeWidth(ann.getStrokeWidth());
                c.setUserData(ann);
                mapPane.getChildren().add(c);
            }
        }
    }

    private void deleteAnnotationNode(javafx.scene.Node node) {
        Object data = node.getUserData();
        if (!(data instanceof Annotation ann)) return;
        boolean ok = app.removeAnnotation(ann);
        if (ok) {
            mapPane.getChildren().remove(node);
            setStatus("✓ Anotación eliminada.");
        } else {
            setStatus("✗ No se pudo eliminar la anotación.");
        }
    }

    private String colorToHex(Color c) {
        return String.format("#%02X%02X%02X",
            (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }

    // ── Construcción del mapa ─────────────────────────────────

    private void buildMap(File imgFile) {
        if (!imgFile.exists()) {
            map_scrollpane.setContent(new Label("Imagen no encontrada: " + imgFile.getPath()));
            return;
        }

        Image img = new Image(imgFile.toURI().toString());
        mapWidth  = img.getWidth();
        mapHeight = img.getHeight();
        currentProjection = null;

        mapPane = new Pane();
        mapPane.setPrefSize(mapWidth, mapHeight);
        mapPane.setMinSize(mapWidth, mapHeight);
        mapPane.setMaxSize(mapWidth, mapHeight);
        mapPane.getChildren().add(new ImageView(img));

        mapPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                mapContextMenu.hide();
                if (selectedActivity != null && currentProjection != null) {
                    rightClickX = e.getX();
                    rightClickY = e.getY();
                    // comprobar si el clic fue sobre una anotación
                    javafx.scene.Node target = e.getTarget() instanceof javafx.scene.Node n ? n : null;
                    while (target != null && target != mapPane) {
                        if (target.getUserData() instanceof Annotation) break;
                        target = target.getParent();
                    }
                    if (target != null && target != mapPane && target.getUserData() instanceof Annotation) {
                        final javafx.scene.Node annNode = target;
                        ContextMenu deleteMenu = new ContextMenu();
                        MenuItem miDelete = new MenuItem("🗑 Eliminar anotación");
                        miDelete.setOnAction(ev -> deleteAnnotationNode(annNode));
                        deleteMenu.getItems().add(miDelete);
                        deleteMenu.show(mapPane.getScene().getWindow(),
                            mapPane.localToScreen(e.getX(), e.getY()).getX(),
                            mapPane.localToScreen(e.getX(), e.getY()).getY());
                    } else {
                        mapContextMenu.show(mapPane.getScene().getWindow(),
                            mapPane.localToScreen(e.getX(), e.getY()).getX(),
                            mapPane.localToScreen(e.getX(), e.getY()).getY());
                    }
                }
            } else if (e.getButton() == MouseButton.PRIMARY && pendingAnnotationType != null) {
                handleAnnotationClick(e.getX(), e.getY());
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

    // ── Inicialización ────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(2.5);
        zoom_slider.setValue(1.0);
        zoom_slider.valueProperty().addListener((obs, o, n) -> zoom((Double) n));

        MenuItem miPoint  = new MenuItem("📍 Añadir punto");
        MenuItem miText   = new MenuItem("📝 Añadir texto");
        MenuItem miLine   = new MenuItem("📏 Añadir línea");
        MenuItem miCircle = new MenuItem("⭕ Añadir círculo");
        mapContextMenu = new ContextMenu(miPoint, miText, miLine, miCircle);
        miPoint.setOnAction(e  -> startAnnotation(AnnotationType.POINT));
        miText.setOnAction(e   -> startAnnotation(AnnotationType.TEXT));
        miLine.setOnAction(e   -> startAnnotation(AnnotationType.LINE));
        miCircle.setOnAction(e -> startAnnotation(AnnotationType.CIRCLE));

        map_listview.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Activity a, boolean empty) {
                super.updateItem(a, empty);
                setText((empty || a == null) ? null : a.getName());
            }
        });

        User user = app.getCurrentUser();
        if (user != null) {
            usernameLabel.setText("👤 " + user.getNickName());
            List<Activity> acts = app.getUserActivities();
            if (acts != null) map_listview.getItems().addAll(acts);
        }

        buildMap(new File("maps/valencia.jpg"));
    }

    // ── Importar GPX ─────────────────────────────────────────

    @FXML
    private void importarGPX(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo GPX");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos GPX", "*.gpx"));
        fc.setInitialDirectory(new File("."));

        File gpxFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (gpxFile == null) return;

        Activity activity = app.importActivity(gpxFile);
        if (activity != null) {
            map_listview.getItems().add(activity);
            map_listview.getSelectionModel().select(activity);
            selectedActivity = activity;

            MapRegion region = app.findMapForActivity(activity);
            if (region != null) {
                buildMap(new File(region.getImagePath()));
                drawActivity(activity, region);
                setStatus(String.format("✓ GPX importado: %s   |   %.2f km",
                        activity.getName(), activity.getTotalDistance() / 1000.0));
            } else {
                setStatus("✓ GPX importado: " + activity.getName() + " (sin mapa disponible)");
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al importar");
            alert.setHeaderText("No se pudo importar el archivo GPX");
            alert.showAndWait();
        }
    }

    // ── Cerrar sesión ─────────────────────────────────────────

    @FXML
    private void cerrarSesion(ActionEvent event) {
        app.logout();
        try {
            Pane pane = new FXMLLoader(getClass().getResource("/login/FXMLLogin.fxml")).load();
            usernameLabel.getScene().setRoot(pane);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Cambiar mapa ──────────────────────────────────────────

    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (imgFile != null) {
            selectedActivity = null;
            buildMap(imgFile);
            setStatus("Mapa cambiado: " + imgFile.getName());
        }
    }

    // ── Acerca de ─────────────────────────────────────────────

    @FXML
    private void about(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        ((Stage) a.getDialogPane().getScene().getWindow()).getIcons()
            .add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        a.setTitle("Acerca de");
        a.setHeaderText("Running la Safor – IPC 2026");
        a.setContentText("Aplicación de seguimiento de actividades deportivas.");
        a.showAndWait();
    }

    // ── Estado ────────────────────────────────────────────────

    private void setStatus(String msg) { statusLabel.setText(msg); }
}
