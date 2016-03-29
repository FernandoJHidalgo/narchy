//package nars.rover.system;
//
//import com.artemis.annotations.Wire;
//import com.badlogic.gdx.maps.MapLayer;
//import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
//import net.mostlyoriginal.ns2d.MyMapRendererImpl;
//import net.mostlyoriginal.ns2d.api.PassiveSystem;
//import net.mostlyoriginal.ns2d.system.passive.MapSystem;
//
///**
// * @author Daan van Yperen
// */
//@Wire
//public class WorldRenderSystem extends PassiveSystem {
//
//    private MapSystem mapSystem;
//    private CameraSystem cameraSystem;
//
//    public MyMapRendererImpl renderer;
//
//
//    @Override
//    protected void initialize() {
//        renderer = new MyMapRendererImpl(mapSystem.map);
//    }
//
//    @Override
//    protected void processSystem() {
//        for (MapLayer layer : mapSystem.map.getLayers()) {
//      			if (layer.isVisible()) {
//      				if (!layer.getName().equals("infront")) {
//                        renderLayer((TiledMapTileLayer) layer);
//                    }
//                }
//        }
//    }
//
//    private void renderLayer(final TiledMapTileLayer layer) {
//        renderer.setView(cameraSystem.camera);
//        renderer.renderLayer(layer);
//    }
//}
