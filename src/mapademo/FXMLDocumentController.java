package mapademo;

import actividades.FXMLDetalleActividadesController;
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
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
import upv.ipc.sportlib.User;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;


public class FXMLDocumentController implements Initializable {

    private final SportActivityApp app = SportActivityApp.getInstance();
    
    private javafx.scene.Node mapaOriginal = null;

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

    @FXML private ListView<Object> map_listview;
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
        Object selectedItem = map_listview.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        if (selectedItem instanceof Activity) {
            Activity selected = (Activity) selectedItem;

            if (event.getClickCount() == 2) {
                Dialog<String> dlg = new Dialog<>();
                dlg.setTitle("Renombrar actividad");
                dlg.setHeaderText("Nuevo nombre para \"" + selected.getName() + "\"");
                ButtonType okBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
                dlg.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);
                TextField tf = new TextField(selected.getName());
                VBox box = new VBox(6, new Label("Nombre:"), tf);
                box.setPadding(new Insets(10));
                dlg.getDialogPane().setContent(box);
                dlg.setResultConverter(b -> b == okBtn ? tf.getText().trim() : null);
                dlg.showAndWait().ifPresent(nombre -> {
                    if (!nombre.isEmpty()) {
                        app.renameActivity(selected, nombre);
                        map_listview.refresh();
                        setStatus("✓ Actividad renombrada a \"" + nombre + "\".");
                    }
                });
                return;
            }

            selectedActivity = selected;
            mostrarDetalleActividad(selected);
            double distKm = selected.getTotalDistance() / 1000.0;
            setStatus(String.format("📍 %s   |   %.2f km", selected.getName(), distKm));
        } else if (selectedItem instanceof MapRegion) {
            MapRegion region = (MapRegion) selectedItem;
            selectedActivity = null;
            Group mapaGroup = buildMap(new File(region.getImagePath()));
            if (mapaGroup != null) map_scrollpane.setContent(mapaGroup);
            setStatus("🗺️ Mapa: " + region.getName());
        } else if (selectedItem instanceof Session) {
            Session session = (Session) selectedItem;
            long minutos = session.getDuration().toMinutes();
            setStatus("💻 Sesión: " + session.getStartTime().toLocalDate() + " | " + minutos + " min");
        }
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

    private Group buildMap(File imgFile) {
        if (!imgFile.exists()) {
        System.out.println("Imagen no encontrada: " + imgFile.getPath());
        return null;
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
    
        return contentGroup;
    }

    // ── Inicialización ────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        map_listview.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setText(null);
                } else if (a instanceof Activity) {
                    Activity act = (Activity) a;
                    String fecha = (act.getStartTime() != null)
                            ? act.getStartTime().toLocalDate().toString() : "";
                    setText(String.format("%s\n%.2f km  ·  %s",
                            act.getName(), act.getTotalDistance() / 1000.0, fecha));
                } else if (a instanceof Session) {
                    Session ses = (Session) a;
                    setText(String.format("📅 %s  ·  %d min",
                            ses.getStartTime().toLocalDate(), ses.getDuration().toMinutes()));
                } else if (a instanceof MapRegion) {
                    MapRegion map = (MapRegion) a;
                    setText("🗺️ " + map.getName());
                }
            }
        });
        
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(2.5);
        zoom_slider.setValue(1.0);
        zoom_slider.valueProperty().addListener((obs, o, n) -> zoom((Double) n));

        ContextMenu listContextMenu = new ContextMenu();
        MenuItem miEliminar = new MenuItem("🗑 Eliminar actividad");
        listContextMenu.getItems().add(miEliminar);

        miEliminar.setOnAction(e -> {
            Object sel = map_listview.getSelectionModel().getSelectedItem();
            if (!(sel instanceof Activity)) return;
            Activity activity = (Activity) sel;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Eliminar actividad");
            confirm.setHeaderText("¿Eliminar \"" + activity.getName() + "\"?");
            confirm.setContentText("Esta acción no se puede deshacer.");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    boolean ok = app.removeActivity(activity);
                    if (ok) {
                        map_listview.getItems().remove(activity);
                        if (activity.equals(selectedActivity)) {
                            selectedActivity = null;
                            List<MapRegion> regions = app.getMapRegions();
                            if (regions != null && !regions.isEmpty()) {
                                Group g = buildMap(new File(regions.get(0).getImagePath()));
                                if (g != null) map_scrollpane.setContent(g);
                            }
                        }
                        setStatus("✓ Actividad eliminada.");
                    } else {
                        setStatus("✗ No se pudo eliminar la actividad.");
                    }
                }
            });
        });

        map_listview.setOnContextMenuRequested(e -> {
            Object sel = map_listview.getSelectionModel().getSelectedItem();
            if (sel instanceof Activity) {
                listContextMenu.show(map_listview, e.getScreenX(), e.getScreenY());
            } else {
                listContextMenu.hide();
            }
        });

        MenuItem miPoint  = new MenuItem("📍 Añadir punto");
        MenuItem miText   = new MenuItem("📝 Añadir texto");
        MenuItem miLine   = new MenuItem("📏 Añadir línea");
        MenuItem miCircle = new MenuItem("⭕ Añadir círculo");
        mapContextMenu = new ContextMenu(miPoint, miText, miLine, miCircle);
        miPoint.setOnAction(e  -> startAnnotation(AnnotationType.POINT));
        miText.setOnAction(e   -> startAnnotation(AnnotationType.TEXT));
        miLine.setOnAction(e   -> startAnnotation(AnnotationType.LINE));
        miCircle.setOnAction(e -> startAnnotation(AnnotationType.CIRCLE));


        User user = app.getCurrentUser();
        if (user != null) {
            usernameLabel.setText("👤 " + user.getNickName());
            List<Activity> acts = app.getUserActivities();
            if (acts != null) map_listview.getItems().addAll(acts);
        }

        List<MapRegion> regions = app.getMapRegions();
        if (regions != null && !regions.isEmpty()) {
            Group mapaInicial = buildMap(new File(regions.get(0).getImagePath()));
            if (mapaInicial != null) {
                map_scrollpane.setContent(mapaInicial);
                mapaOriginal = mapaInicial;
            }
        }
        
        

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
                Group mapaGroup = buildMap(new File(region.getImagePath()));
                drawActivity(activity, region);
                if (mapaGroup != null) map_scrollpane.setContent(mapaGroup);
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
            Pane pane = FXMLLoader.load(getClass().getResource("/login/FXMLLogin.fxml"));
            usernameLabel.getScene().setRoot(pane);
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    // ── Añadir mapa ───────────────────────────────────────────

    @FXML
    private void añadirMapa(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen del mapa");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
        File imgFile = fc.showOpenDialog(map_listview.getScene().getWindow());
        if (imgFile == null) return;

        Dialog<MapRegion> dialog = new Dialog<>();
        dialog.setTitle("Añadir mapa");
        dialog.setHeaderText("Introduce los datos del mapa");

        ButtonType okBtn = new ButtonType("Añadir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okBtn, ButtonType.CANCEL);

        TextField tfNombre = new TextField(); tfNombre.setPromptText("Nombre del mapa");
        TextField tfLatMin = new TextField(); tfLatMin.setPromptText("Latitud mínima");
        TextField tfLatMax = new TextField(); tfLatMax.setPromptText("Latitud máxima");
        TextField tfLonMin = new TextField(); tfLonMin.setPromptText("Longitud mínima");
        TextField tfLonMax = new TextField(); tfLonMax.setPromptText("Longitud máxima");

        VBox content = new VBox(6,
            new Label("Nombre:"), tfNombre,
            new Label("Lat. mínima:"), tfLatMin,
            new Label("Lat. máxima:"), tfLatMax,
            new Label("Lon. mínima:"), tfLonMin,
            new Label("Lon. máxima:"), tfLonMax
        );
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        final File finalImg = imgFile;
        dialog.setResultConverter(btn -> {
            if (btn != okBtn) return null;
            try {
                double latMin = Double.parseDouble(tfLatMin.getText().trim());
                double latMax = Double.parseDouble(tfLatMax.getText().trim());
                double lonMin = Double.parseDouble(tfLonMin.getText().trim());
                double lonMax = Double.parseDouble(tfLonMax.getText().trim());
                return app.addMapRegion(tfNombre.getText().trim(), finalImg, latMin, latMax, lonMin, lonMax);
            } catch (NumberFormatException e) {
                return null;
            }
        });

        dialog.showAndWait().ifPresent(region -> {
            if (region != null) {
                setStatus("✓ Mapa añadido: " + region.getName());
                mostrarMapas();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("No se pudo añadir el mapa");
                err.setContentText("Comprueba que las coordenadas son válidas y el nombre no está vacío.");
                err.showAndWait();
            }
        });
    }

    // ── Cambiar mapa ──────────────────────────────────────────

    @FXML
    private void cambiarMapa(ActionEvent event) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setInitialDirectory(new File("."));
        File imgFile = fc.showOpenDialog(zoom_slider.getScene().getWindow());
        if (imgFile != null) {
            selectedActivity = null;
            Group mapaGroup = buildMap(imgFile);
            if (mapaGroup != null) map_scrollpane.setContent(mapaGroup);
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

    @FXML
    private void mostrarSesiones(ActionEvent event) {
        User usuarioActual = app.getCurrentUser();
        if (usuarioActual != null) {
            List<Session> sesiones = app.getSessionsByUser(usuarioActual);
            map_listview.getItems().clear();
            if (sesiones != null) map_listview.getItems().addAll(sesiones);
        }
        try {
            Pane nuevaVista = FXMLLoader.load(getClass().getResource("/sesiones/FXMLSesiones.fxml"));
            map_scrollpane.setContent(nuevaVista);
            map_scrollpane.setFitToWidth(true);
            map_scrollpane.setFitToHeight(true);
            map_scrollpane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setPannable(false);
            setStatus("Historial de sesiones.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void mostrarMapas() {
        map_scrollpane.setFitToWidth(false);
        map_scrollpane.setFitToHeight(false);
        map_scrollpane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        map_scrollpane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        map_scrollpane.setPannable(true);

        List<MapRegion> regions = app.getMapRegions();
        map_listview.getItems().clear();
        if (regions != null) map_listview.getItems().addAll(regions);

        if (mapaOriginal != null) map_scrollpane.setContent(mapaOriginal);
        setStatus("Selecciona un mapa de la lista para visualizarlo.");
    }

    @FXML
    private void mostrarActividades() {
        List<Activity> actividades = app.getUserActivities();
        map_listview.getItems().clear();
        if (actividades != null) map_listview.getItems().addAll(actividades);
        try {
            Pane nuevaVista = FXMLLoader.load(getClass().getResource("/actividades/FXMLActividades.fxml"));
            map_scrollpane.setContent(nuevaVista);
            map_scrollpane.setFitToWidth(true);
            map_scrollpane.setFitToHeight(true);
            map_scrollpane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setPannable(false);
            setStatus("Estadísticas acumuladas.");
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void añadirActividad(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo GPX");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos GPX", "*.gpx"));
        File archivo = fc.showOpenDialog(map_listview.getScene().getWindow());
        if (archivo == null) return;
        Activity nueva = app.importActivity(archivo);
        if (nueva != null) {
            mostrarActividades();
            setStatus("✓ Actividad importada: " + nueva.getName());
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error al importar");
            alert.setHeaderText("No se pudo importar el archivo GPX");
            alert.showAndWait();
        }
    }

    private void mostrarGraficaDesnivel(Activity actividad) {
    List<TrackPoint> puntos = actividad.getTrackPoints();
    if (puntos == null || puntos.isEmpty()) return;

    NumberAxis ejeX = new NumberAxis();
    ejeX.setLabel("Distancia (km)");
    NumberAxis ejeY = new NumberAxis();
    ejeY.setLabel("Altitud (m)");

    AreaChart<Number, Number> areaChart = new AreaChart<>(ejeX, ejeY);
    areaChart.setTitle("Perfil de Desnivel: " + actividad.getName());
    areaChart.setLegendVisible(false);

    XYChart.Series<Number, Number> series = new XYChart.Series<>();
    double distanciaAcumulada = 0.0;
    
    TrackPoint primerPunto = puntos.get(0);
    series.getData().add(new XYChart.Data<>(0.0, primerPunto.getElevation()));

    for (int i = 0; i < puntos.size() - 1; i++) {
        TrackPoint pActual = puntos.get(i);
        TrackPoint pSiguiente = puntos.get(i + 1);
        distanciaAcumulada += pActual.distanceTo(pSiguiente);
        
        double km = distanciaAcumulada / 1000.0;
        double altitud = pSiguiente.getElevation();
        series.getData().add(new XYChart.Data<>(km, altitud));
    }

    areaChart.getData().add(series);

    Stage stageGrafica = new Stage();
    stageGrafica.setTitle("Análisis de Altitud - Running la Safor");
    stageGrafica.initModality(Modality.NONE);
    stageGrafica.initOwner(map_listview.getScene().getWindow());
    
    Scene escena = new Scene(areaChart, 600, 350);
    stageGrafica.setScene(escena);
    stageGrafica.show();
}
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    @FXML
    private void perfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/modificarPerfil/FXMLModificarPerfil.fxml"));
            Pane root = loader.load();
            Stage ventanaPerfil = new Stage();
            ventanaPerfil.setTitle("Modificar Perfil");
            ventanaPerfil.initModality(Modality.WINDOW_MODAL);
            ventanaPerfil.initOwner(map_listview.getScene().getWindow());
            ventanaPerfil.setScene(new Scene(root));
            ventanaPerfil.showAndWait();
            User user = app.getCurrentUser();
            if (user != null) usernameLabel.setText("👤 " + user.getNickName());
        } catch (Exception e) { e.printStackTrace(); }
    }
     
    private void mostrarDetalleActividad(Activity actividad) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/actividades/FXMLDetalleActividades.fxml"));
            Pane vistaDetalle = loader.load();
            FXMLDetalleActividadesController controladorDetalle = loader.getController();
            MapRegion region = app.findMapForActivity(actividad);
            if (region != null) {
                Group mapaConRuta = buildMap(new File(region.getImagePath()));
                drawActivity(actividad, region);
                for (Annotation ann : actividad.getAnnotations()) drawAnnotation(ann);
                controladorDetalle.setActividad(actividad, mapaConRuta);
            } else {
                controladorDetalle.setActividad(actividad, null);
            }
            map_scrollpane.setFitToWidth(true);
            map_scrollpane.setFitToHeight(true);
            map_scrollpane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            map_scrollpane.setPannable(false);
            map_scrollpane.setContent(vistaDetalle);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

