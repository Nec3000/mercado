// Nunca cambia la declaracion del package!
package cc.mercado;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class MercadoMonitor implements Mercado {
  // TODO: añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)

  public MercadoMonitor() {
    // TODO: inicializar estado, monitor y conditions
  }

  public int venta(int minPrecio, int tks) {
    // TODO: implementar venta
    return -1;
  }

  public int compra(int maxPrecio, int tks) {
    // TODO: implementar compra
    return -1;
  }

  public int resultadoOferta(int id) {
    // TODO: implementar resultadoOferta
    return -1;
  }

  public void alertaPrecioBajo(int limite) {
    // TODO: implementar alertaPrecioBajo
  }
  
  public void alertaPrecioAlto(int limite) {
    // TODO: implementar alertaPrecioAlto
  }

  public void tick() {
    // TODO: implementar tick
  }
}
