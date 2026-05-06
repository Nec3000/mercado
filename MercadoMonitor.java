// Nunca cambia la declaracion del package!
package cc.mercado;

import es.upm.babel.cclib.Monitor;

import java.util.HashMap;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class MercadoMonitor implements Mercado {
  // TODO: añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)
  Monitor mutex;

  // atributos para representar el estado del recurso
  private HashMap<Integer,Oferta> compras;
  private HashMap<Integer,Oferta> ventas;
  private int max;
  private int min;
  private int id_cont;

  private static class Oferta {
      int precio;
      int ticks;
      int dinero;
      public Oferta(int precio, int ticks, int dinero) {
            this.precio = precio;
            this.ticks = ticks;
            this.dinero = dinero;
      }
  }

  public MercadoMonitor() {
    // TODO: inicializar estado, monitor y conditions
      compras = new HashMap<>();
      ventas = new HashMap<>();
      max = Integer.MAX_VALUE;
      min = Integer.MIN_VALUE;//queremos emular el infinito del C-tad, es el menor numero posible dado por java, lo mismo con max
      id_cont = 0;
      mutex = new Monitor();
  }

  public int venta(int minPrecio, int tks) {
    // TODO: implementar venta
      mutex.enter();
      int resultado = id_cont;
      id_cont++;



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


