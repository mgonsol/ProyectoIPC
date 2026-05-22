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
import upv.ipc.sportlib.Session;
import upv.ipc.sportlib.SportActivityApp;
import upv.ipc.sportlib.TrackPoint;
import upv.ipc.sportlib.User;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Window;

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
        // Obtenemos el elemento seleccionado sin asumir qué tipo de objeto es
        Object selectedItem = map_listview.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;

        // CASO 1: Han hecho clic en una ACTIVIDAD
        if (selectedItem instanceof Activity) {
            Activity selected = (Activity) selectedItem;
            selectedActivity = selected;

            MapRegion region = app.findMapForActivity(selected);
            if (region == null) { setStatus("No se encontró mapa para esta actividad."); return; }

            // Usamos tu método buildMap que ya tienes programado
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
            long   minutos = selected.getDuration().toMinutes();
            setStatus(String.format("📍 %s   |   %.2f km   |   %d min",
                    selected.getName(), distKm, minutos));
        } 
        
        // CASO 2: Han hecho clic en un MAPA
        else if (selectedItem instanceof MapRegion) {
            MapRegion region = (MapRegion) selectedItem;
            
            // Limpiamos la actividad seleccionada para que no intente dibujar rutas raras
            selectedActivity = null; 
            
            // Llamamos directamente a tu método buildMap con la ruta de la imagen
            buildMap(new File(region.getImagePath()));
            
            setStatus("🗺️ Mostrando mapa limpio: " + region.getName());
        } 
        
        // CASO 3: Han hecho clic en una SESIÓN
        else if (selectedItem instanceof Session) {
            Session session = (Session) selectedItem;
            
            // En las sesiones no hay mapa que mostrar, así que solo actualizamos la barra inferior
            long minutos = session.getDuration().toMinutes();
            setStatus("💻 Sesión visualizada. Duración total: " + minutos + " minutos");
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
        map_listview.setCellFactory(lv -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object a, boolean empty) {
                super.updateItem(a, empty);

                // 1. Si la celda está vacía o el objeto es nulo, la dejamos en blanco
                if (empty || a == null) {
                    setText(null);
                } 
                // 2. Si el objeto que entra es una Actividad...
                else if (a instanceof Activity) {
                    Activity act = (Activity) a; // Hacemos el cast
                    setText(act.getName()); // Aquí sí funciona el getName()
                } 
                // 3. Si el objeto que entra es una Sesión...
                else if (a instanceof Session) {
                    Session ses = (Session) a;
                    // Como no tiene getName(), usamos su fecha de inicio para identificarla
                    setText("Sesión del " + ses.getStartTime());
                } 
                // 4. Si el objeto que entra es un Mapa...
                else if (a instanceof MapRegion) {
                    MapRegion map = (MapRegion) a;
                    setText(map.getName()); // Aquí también funciona el getName()
                }
            }
        });
        
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

    @FXML
    private void mostrarSesiones(ActionEvent event) {
        // 1. Mantener tu lógica actual de rellenar la lista de la izquierda
        User usuarioActual = app.getCurrentUser(); 
        if (usuarioActual != null) {
            List<Session> sesiones = app.getSessionsByUser(usuarioActual);
            map_listview.getItems().clear();
            map_listview.getItems().addAll(sesiones);
        }

        // 2. CAMBIO DE VISTA: Cargar el menú de sesiones en el centro
        try {
            // Cargamos el archivo FXML de la nueva vista
            Pane nuevaVista = FXMLLoader.load(getClass().getResource("/sesiones/FXMLSesiones.fxml"));
            
            // Reemplazamos el mapa del centro por este nuevo panel
            map_scrollpane.setContent(nuevaVista);
            
            setStatus("Mostrando panel de control de Sesiones.");
        } catch (Exception e) {
            System.out.println("Error al cargar VistaMenuSesiones.fxml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void mostrarMapas(ActionEvent event) {
        // Recuperamos todas las regiones de mapas registradas [cite: 276]
        List<MapRegion> mapas = app.getMapRegions(); 
        
        // Limpiamos la lista y añadimos los mapas
        map_listview.getItems().clear();
        map_listview.getItems().addAll(mapas);
    }

    @FXML
    private void mostrarActividades() {
        // 1. Mantener tu lógica actual de la lista
        List<Activity> actividades = app.getUserActivities();
        map_listview.getItems().clear();
        map_listview.getItems().addAll(actividades);

        // 2. CAMBIO DE VISTA: Cargar el menú de actividades en el centro
        try {
            Pane nuevaVista = FXMLLoader.load(getClass().getResource("/actividades/FXMLActividades.fxml"));
            map_scrollpane.setContent(nuevaVista);
            
            setStatus("Mostrando panel de control de Actividades.");
        } catch (Exception e) {
            System.out.println("Error al cargar VistaMenuActividades.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void añadirActividad(ActionEvent event) {
        // Creamos el explorador de archivos
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar ruta deportiva (GPX)");
        
        // Ponemos un filtro para que el usuario solo pueda elegir archivos .gpx
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos GPS (*.gpx)", "*.gpx")
        );

        // Obtenemos la ventana actual de la aplicación (necesario para mostrar el diálogo)
        Window ventanaActual = map_listview.getScene().getWindow();
        
        // Abrimos la ventana y esperamos a que el usuario seleccione un archivo
        File archivoSeleccionado = fileChooser.showOpenDialog(ventanaActual);

        // Si el usuario seleccionó un archivo (es decir, no le dio a "Cancelar")
        if (archivoSeleccionado != null) {
            
            // Usamos la librería para importar el archivo GPX a la base de datos
            Activity nuevaActividad = app.importActivity(archivoSeleccionado);
            
            if (nuevaActividad != null) {
                System.out.println("¡Éxito! Actividad importada: " + nuevaActividad.getName());
                
                // Refrescamos automáticamente la lista para que el usuario vea su nueva actividad
                mostrarActividades(); 
            } else {
                System.out.println("Hubo un error al intentar procesar el archivo GPX.");
            }
        }
    }

    @FXML
    private void perfil(ActionEvent event) throws IOException {
        try {
        // 1. Cargamos el archivo FXML de la nueva interfaz de perfil
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/modificarPerfil/FXMLModificarPerfil.fxml"));
        Pane root = loader.load();
        
        // 2. Creamos un nuevo escenario (Stage) para la ventana secundaria
        Stage ventanaPerfil = new Stage();
        ventanaPerfil.setTitle("Modificar Perfil de Usuario");
        
        // 3. Hacemos que sea modal (bloquea la ventana principal mientras esté abierta)
        ventanaPerfil.initModality(Modality.WINDOW_MODAL);
        ventanaPerfil.initOwner(map_listview.getScene().getWindow());
        
        // 4. Asignamos la escena y mostramos la ventana
        Scene scene = new Scene(root);
        ventanaPerfil.setScene(scene);
        
        // Usamos showAndWait() para que el programa "espere" a que se cierre
        ventanaPerfil.showAndWait();
        
        // Opcional: Cuando se cierre la ventana, podemos refrescar el nombre de usuario
        // por si acaso lo cambiase (aunque el nick es fijo, el avatar o datos sí cambian)
        User user = app.getCurrentUser();
        if (user != null) {
            usernameLabel.setText("👤 " + user.getNickName());
        }
        
        } catch (Exception e) {
            System.out.println("Error al abrir la ventana de modificar perfil: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
