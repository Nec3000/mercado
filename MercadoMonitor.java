package cc.mercado;

import es.upm.babel.cclib.Monitor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MercadoMonitor implements Mercado {
  Monitor mutex;

  // atributos para representar el estado del recurso
  private HashMap<Integer,Oferta> compras;
  private HashMap<Integer,Oferta> ventas;
  //Mapas para almacenar las colas de hilos en las variables de condición
  private HashMap<Monitor.Cond, Integer> alertabajo=new HashMap<>();
  private HashMap<Monitor.Cond, Integer> alertaalto=new HashMap<>();
  private HashMap<Monitor.Cond, Integer> Resultado = new HashMap<>();
  //Variables de control del precio historico en el intervalo permitido
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
      compras = new HashMap<>();
      ventas = new HashMap<>();
      //queremos emular el infinito del C-tad, es el menor numero posible dado por java, lo mismo con max
      //además deduciendo del C-tad llegamos a la conclusión de que estos datos se inicializan el revés de lo que se podría pensar
      max = Integer.MIN_VALUE;
      min = Integer.MAX_VALUE;
      id_cont = 0;
      //creamos el monitor que se usará a lo largo del programa
      mutex = new Monitor();
  }
  /*
  Esta funcion auxiliar permite buscar una oferta de compra que ayude a lo más conveniente de maximizar la venta de mayor precio y de menor id
  */
  private int matchV(int preciominimo, int id){
      int resultado = -1;
      int maximo = 0;
      //entra en un bucle en el cual busca
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
      Oferta v = null;
      //protocolo de entrada
      mutex.enter();
      //como la CPRE siempre es cierta entonces no se tiene que hacer ninguna condición que mande a dormir a los hilos
      int resultado = id_cont;
      id_cont++;
      int compatible = matchV(minPrecio, resultado);
      if(tks == 0 || compatible == -1){
          v = new Oferta(minPrecio, tks, 0);
          ventas.put(resultado, v);
      }
      else {
          Oferta c = compras.get(compatible);
          int precio = (minPrecio + c.precio) / 2;
          v = new Oferta(minPrecio, tks, precio);
          //esto es el dinero ganado
          v.dinero=precio;
          //esto el dinero gastado por el comprador
          c.dinero = precio;
          //actualizamos con lo nuevo
          ventas.put(resultado, v);
          //modificación simultanea de las ventas y compras
          compras.put(compatible, c);
          //se actualiza el valor más hacia los extremos
          this.max = precio > max? precio:max;
          this.min = precio < min ? precio:min;
      }
      //el siguiente fragmento de código es el protocolo de salida
      desbloqueo();
      mutex.leave();
    return resultado;
  }
  /*
   Esta funcion auxiliar ayuda a encontrar la venta que mejor beneficio otroque, es decie menor precio y menor id
  */
    private int matchC(int preciomaximo, int id){
        int resultado = -1;
        //es para buscar minimos correctamente desde el valor más alto posible
        int minimo = Integer.MAX_VALUE;
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
    /*
    Una oferta de compra que lanza algún comprador
    * */
  public int compra(int maxPrecio, int tks) {
      Oferta c = null;
      //protocolo de entrada al monitor
      mutex.enter();
      //como la CPRE siempre es cierta entonces no se tiene que hacer ninguna condición que mande a dormir a los hilos
      int resultado = id_cont;
      id_cont++;
      int compatible = matchC(maxPrecio, resultado);
      if(tks == 0 || compatible == -1){
          c = new Oferta(maxPrecio, tks, 0);
          compras.put(resultado, c);
      }
      else {
          Oferta v = ventas.get(compatible);
          int precio = (maxPrecio + v.precio) / 2;
          c = new Oferta(maxPrecio, tks, precio);
          //dinero ganado por el comprador
          v.dinero = precio;
          //dinero gastado
          c.dinero = precio;
          //actualizamos con lo nuevo
          ventas.put(compatible, v);
          compras.put(resultado, c);
          this.max = precio > max? precio:max;
          this.min = precio < min ? precio:min;
      }
      //protocolo de salida
      desbloqueo();
      mutex.leave();
      return resultado;
  }

  /*
  Es el resultado de una oferta, siendo que está puede haber coincidido y haberse realizado una compra o una venta,
  o simplemente haber expirado porque se agotó el tiempo de espera
  */
  public int resultadoOferta(int id) {
      Oferta oferta;
      int res = 0;
      mutex.enter();
      //como aquí ya existe una CPRE se requiere generar la condicion del monitor para mandar a esperar a los hilos
      Monitor.Cond cond = mutex.newCond();
      if(compras.containsKey(id)){
          oferta = compras.get(id);
      }
      else{
          oferta = ventas.get(id);
      }
      //esta sería la CPRE donde se decide si un hilo se va a esperar o puede ser usado
      if(oferta!=null){
          if(oferta.dinero==0 && oferta.ticks>0){
            Resultado.put(cond,id);
            cond.await();
            Resultado.remove(cond);
            //cuando el hilo vuelve, necesita volver a tener la oferta
            if(compras.containsKey(id)){
              oferta = compras.get(id);
            }
            else{
                oferta = ventas.get(id);
            }
          }
          res = oferta.dinero;
      }
      //el desbloque se encarga de ir despertando en orden según cumplan las condiciones los hilos que se mandan a dormir
      desbloqueo();
      mutex.leave();
      return res;
  }
  /*
  Esta funcion realiza una alerta en la cual un hilo se queda esperando el que aparezca o se de el caso de que el precio está bajo y avisa antes de desaparecer
  */
  public void alertaPrecioBajo(int limite) {
      mutex.enter();
      Monitor.Cond Condi=mutex.newCond();
      //damos datos al hilo que se vaya a poner
      alertabajo.put(Condi,limite);
      //como tiene CPRE se hace una parte donde si no se cumple la condición se los pone a dormir
      if (limite < min){
          // se manda a dormir el hilo
          Condi.await();
      }
      // se elimina el hilo ya que se cumplió su condición
      alertabajo.remove(Condi);
      desbloqueo();
      mutex.leave();
  }
  /*
    funciona igual que su función opuesta genera una alerta, pero para el caso opuesto
  */
  public void alertaPrecioAlto(int limite) {
      mutex.enter();
      Monitor.Cond condi = mutex.newCond();
      alertaalto.put(condi,limite);
      if(limite>max){
          condi.await();
      }
      alertaalto.remove(condi);
      //se llama a la funcion de desbloque que es en sí un sistema que lleva la lógica del como despertar a los hilos
      desbloqueo();
      mutex.leave();
  }
  /*
  Contador del tiempo de vida de las alertas
  */
  public void tick() {
      mutex.enter();
      //se declara de forma inversa para que la primera declaración de valor en una oferta,
      // ya sea de compra o venta acabe correctamente colocada
      this.max = Integer.MIN_VALUE; //-inf
      this.min = Integer.MAX_VALUE; //inf
      //se recorre la lista generada con los valores de las claves de las ventas
      for (Integer ids : ventas.keySet()) {
          Oferta c = ventas.get(ids);
          c.ticks = Math.max(c.ticks-1, 0);
          ventas.put(ids, c);
      }
      //se recorre la lista generada con los valores de las claves de las compras
      for (Integer ids : compras.keySet()) {
          Oferta c = compras.get(ids);
          c.ticks = Math.max(c.ticks-1, 0);
          compras.put(ids, c);
      }
      //protocolo de salida del monitor
      /*
      En este caso se requiere usar desbloqueo porque requieres eliminar los hilos que han terminado su tiempo,
      es decir los hilos en los que tick llegó a 0 y no haya tomado ninguna de las ofertas que vino
      */
      desbloqueo();
      mutex.leave();
  }

    /*  Es el código correspondiente a un desbloqueo generico el cual irá haciendo las revisiones de los distintos monitores
        de cada uno de los métodos y los irá despertando según vayan cumpliendo sus CPREs.
        Todos los bucles siguen una lógica parecida y en lo que difieren es el iterador que se usa para cada búsqueda de los hilos
        y no entrar en bucles que no nos interesan.
        Notar que solo hay bucles para despertar hilos en la misma cantidad que métodos que tienen una CPRE y que se despierta según cumplen estas condiciones
    */
    private void desbloqueo(){
        //esta parte del código corresponde a analizar con un iterador los hilos que existen para alerta del precio bajo
        Iterator<Map.Entry<Monitor.Cond, Integer>> it = alertabajo.entrySet().iterator();
        boolean encontrado = false;
        //nos apoyamos de una variable "encontrado" para poder dar fin a los bucles
        while (it.hasNext() && !encontrado) {
            Map.Entry<Monitor.Cond, Integer> e = it.next();//se avanza en el iterador
            Monitor.Cond cond = e.getKey();
            int limiteAlerta = e.getValue();
            if(cond.waiting()>0 && limiteAlerta>=min){
                //aquí despertamos al hilo  que cumple con la condición
                cond.signal();
                //tenemos que parar al encontrar uno para no hacer signals sin parar y cargarnoslo
                encontrado = true;
            }
        }
        //este es el bucle de la alerta de precio alto
        Iterator<Map.Entry<Monitor.Cond,Integer>> it2 = alertaalto.entrySet().iterator();
        while(it2.hasNext() && !encontrado) {
            Map.Entry<Monitor.Cond, Integer> e = it2.next();
            Monitor.Cond cond = e.getKey();
            int limiteAlertaAlto = e.getValue();
            if (cond.waiting() > 0 && limiteAlertaAlto <= max) {
                cond.signal();
                encontrado = true;
            }
        }
        Iterator<Map.Entry<Monitor.Cond,Integer>> it3 = Resultado.entrySet().iterator();
        //Para este bucle se tienen algunas diferencias en las condiciones ya que vienen dadas en sí por un objeto que sería Oferta
        //Se obtiene el id y en base a eso se busca la oferta
        while(it3.hasNext() && !encontrado) {
            Map.Entry<Monitor.Cond, Integer> e = it3.next();
            Monitor.Cond cond = e.getKey();
            int id = e.getValue();
            //La oferta de identificara como de compra si compra tiene el id que sería el Value de it3 y como venta caso contrario
            Oferta o = compras.containsKey(id)? compras.get(id): ventas.get(id);
            // la condicion aquí es que no sea nula la oferta y que está sea mayor que 0 o que sus ticks no hayan terminado para despertarlo
            if (cond.waiting() > 0 && o!=null && (o.dinero>0||o.ticks==0)) {
                cond.signal();
                encontrado = true;
            }
        }
    }
}


