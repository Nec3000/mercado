// Nunca cambia la declaracion del package!
package cc.mercado;

import es.upm.babel.cclib.Monitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
  private HashMap<Monitor.Cond, Integer> alertabajo=new HashMap<>();
  private HashMap<Monitor.Cond, Integer> alertaalto=new HashMap<>();
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
      max = Integer.MIN_VALUE;
      min = Integer.MAX_VALUE;//queremos emular el infinito del C-tad, es el menor numero posible dado por java, lo mismo con max
      id_cont = 0;
      mutex = new Monitor();
  }
  private int matchV(int preciominimo, int id){
      int resultado = -1;
      int maximo = 0;
      for(Integer ids : compras.keySet()) {
        Oferta c = compras.get(ids);
        if (c.ticks > 0 && c.dinero == 0 && c.precio >= preciominimo && (c.precio > maximo || (c.precio == maximo && ids < resultado))){
            //La condicion entra cuando es compatible, y la mejor de todas las compatibles hasta ese punto
            maximo = c.precio;
            resultado = ids;
        }
      }
      return resultado;
  }

  public int venta(int minPrecio, int tks) {
    // TODO: implementar venta
      Oferta v = null;
      mutex.enter();
      int resultado = id_cont;
      id_cont++;
      int compatible = matchV(minPrecio, resultado);
      if(tks == 0 || compatible == -1){
          v = new Oferta(minPrecio, tks, 0);
          ventas.put(resultado, v);
      }
      else {
          Oferta c = compras.get(resultado);
          int precio = (minPrecio + c.precio) / 2;
          v = new Oferta(minPrecio, tks, precio);
          c.precio = precio;
          //actualizamos con lo nuevo
          ventas.put(resultado, v);
          compras.put(compatible, c);
          this.max = precio > max? precio:max;
          this.min = precio < min ? precio:min;
      }
      mutex.leave();
    return resultado;
  }
    private int matchC(int preciomaximo, int id){
        int resultado = -1;
        int minimo = 0;
        for(Integer ids : ventas.keySet()) {
            Oferta c = ventas.get(ids);
            if (c.ticks > 0 && c.dinero == 0 && c.precio <= preciomaximo && (c.precio < minimo || (c.precio == minimo && ids < resultado))){
                //La condicion entra cuando es compatible, y la mejor de todas las compatibles hasta ese punto
                minimo = c.precio;
                resultado = ids;
            }
        }
        return resultado;
    }

  public int compra(int maxPrecio, int tks) {
    // TODO: implementar compra
      Oferta c = null;
      mutex.enter();
      int resultado = id_cont;
      id_cont++;
      int compatible = matchC(maxPrecio, resultado);
      if(tks == 0 || compatible == -1){
          c = new Oferta(maxPrecio, tks, 0);
          compras.put(resultado, c);
      }
      else {
          Oferta v = ventas.get(resultado);
          int precio = (maxPrecio + v.precio) / 2;
          c = new Oferta(maxPrecio, tks, precio);
          v.precio = precio;
          //actualizamos con lo nuevo
          ventas.put(compatible, v);
          compras.put(resultado, c);
          this.max = precio > max? precio:max;
          this.min = precio < min ? precio:min;
      }
      mutex.leave();
      return resultado;
  }

  public int resultadoOferta(int id) {
    // TODO: CPRE
      int res = 0;
      if(compras.containsKey(id) || ventas.containsKey(id)){
          mutex.enter();
          if(compras.containsKey(id)){
              res = compras.get(id).precio;
          }
          else{
              res = ventas.get(id).precio;
          }
          mutex.leave();
      }
    return res;
  }

  public void alertaPrecioBajo(int limite) {
    // TODO: implementar alertaPrecioBajo
      mutex.enter();
      Monitor.Cond Condi=mutex.newCond();
      alertabajo.put(Condi,limite);
      if (limite < min){
          Condi.await();
      }
      alertabajo.remove(Condi);
      desbloqueo(limite);
  }
  private void desbloqueo(int limite){
        Iterator<Map.Entry<Monitor.Cond, Integer>> it = alertabajo.entrySet().iterator();
        boolean encontrado = false;
        while (it.hasNext() && !encontrado) {
            Map.Entry<Monitor.Cond, Integer> e = it.next();
            Monitor.Cond cond = e.getKey();
            int limiteAlerta = e.getValue();
            if(cond.waiting()>0 && limiteAlerta>=min){
                cond.signal();
                encontrado = true;//tenemos que parar al encontrar uno para no hacer signals sin parar y cargarnoslo
            }
        }
      mutex.leave();
  }
  
  public void alertaPrecioAlto(int limite) {
    // TODO: implementar alertaPrecioAlto
  }

  public void tick() {
    // TODO: implementar singals?
      mutex.enter();
      this.max = Integer.MIN_VALUE;
      this.min = Integer.MAX_VALUE;
      for (Integer ids : ventas.keySet()) {
          Oferta c = ventas.get(ids);
          c.ticks = Math.max(c.ticks-1, 0);
          ventas.put(ids, c);
      }
      for (Integer ids : compras.keySet()) {
          Oferta c = compras.get(ids);
          c.ticks = Math.max(c.ticks-1, 0);
          compras.put(ids, c);
      }
      mutex.leave();
  }
}


