// Nunca cambia la declaracion del package!
package cc.mercado;

import es.upm.babel.cclib.Monitor;
import org.jcsp.lang.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;


public class MercadoCSP implements Mercado, CSProcess {
  private Any2OneChannel chVenta;
  private Any2OneChannel chCompra;
  private Any2OneChannel chResultadoOferta;
  private Any2OneChannel chAlertaPrecioBajo;
  private Any2OneChannel chAlertaPrecioAlto;
  private Any2OneChannel chTick;

  private int id_cont;
  private int max;
  private int min;
  private HashMap<Integer, Oferta> compras;
  private HashMap<Integer, Oferta> ventas;

    //tipo privado oferta
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
    //aqui los paquetes que enviaran nuestro canales
    //tambien lo usamos para almacenar las aplazadas
  private static class MessageVenta{
    int minPrecio;
    int tks;
    ChannelOutputInt chRes; //es de tipo int porque queremos que el servidor devuelva el id el cual es un tipo entero
    MessageVenta(int minPrecio, int tks, ChannelOutputInt chRes){
      this.minPrecio=minPrecio;
      this.tks=tks;
      this.chRes=chRes;
    }
  }
  private static class MessageCompra{
    int maxPrecio;
    int tks;
    ChannelOutputInt chRes;
    MessageCompra(int maxPrecio, int tks, ChannelOutputInt chRes){
        this.maxPrecio=maxPrecio;
        this.tks=tks;
        this.chRes=chRes;
    }
  }
  //es una idea rapida para poder meter todos en la coleccion de peticiones aplazadas
  public interface Aplazable{};

  private static class MessageAlertaPrecioBajo implements Aplazable{
    int limite;
    ChannelOutputInt chRes;
    MessageAlertaPrecioBajo(int limite, ChannelOutputInt chRes){
      this.limite=limite;
      this.chRes=chRes;
    }
  }
  private static class MessageAlertaPrecioAlto implements Aplazable{
    int limite;
    ChannelOutputInt chRes;
    MessageAlertaPrecioAlto(int limite, ChannelOutputInt chRes){
      this.limite=limite;
      this.chRes=chRes;
    }
  }
  private static class MessageResOferta implements Aplazable{
    int id;
    ChannelOutputInt chRes;
    MessageResOferta(int id, ChannelOutputInt chRes){
      this.id=id;
      this.chRes=chRes;
    }
  }

  public MercadoCSP() {
      //todos nuestros canales
    chVenta=Channel.any2one();
    chCompra=Channel.any2one();
    chResultadoOferta=Channel.any2one();
    chAlertaPrecioAlto=Channel.any2one();
    chAlertaPrecioBajo=Channel.any2one();
    chTick=Channel.any2one();

    //los estados del recurso
    id_cont = 0;
    ventas = new HashMap<>();
    compras = new HashMap<>();
    max = Integer.MIN_VALUE;
    min = Integer.MAX_VALUE;
    // Puesta en marcha del servidor: alternativa sucia (desde el
    // punto de vista de CSP) a Parallel que nos ofrece JCSP para
    // poner en marcha un CSProcess

    new ProcessManager(this).start();
  }
  public int venta(int minPrecio, int tks) {
    One2OneChannelInt chRes = Channel.one2oneInt();
    chVenta.out().write(new MessageVenta(minPrecio,tks,chRes.out()));
    return chRes.in().read();
  }

  public int compra(int maxPrecio, int tks) {
    One2OneChannelInt chRes = Channel.one2oneInt();
    chCompra.out().write(new MessageCompra(maxPrecio,tks,chRes.out()));
    return chRes.in().read();
  }

  public int resultadoOferta(int id) {
    One2OneChannelInt chRes = Channel.one2oneInt();
    chResultadoOferta.out().write(new MessageResOferta(id, chRes.out()));
    return chRes.in().read();
  }

  public void alertaPrecioBajo(int limite) {
    One2OneChannelInt chRes = Channel.one2oneInt();
    chAlertaPrecioBajo.out().write(new MessageAlertaPrecioBajo(limite,chRes.out()));
    chRes.in().read();

  }
  
  public void alertaPrecioAlto(int limite) {
    One2OneChannelInt chRes = Channel.one2oneInt();
    chAlertaPrecioAlto.out().write(new MessageAlertaPrecioAlto(limite, chRes.out()));
    chRes.in().read();
  }

  public void tick() {
    chTick.out().write(null);
  }
  public int EjVenta(int minPrecio, int tks) {
      Oferta v = null;
      //como la CPRE siempre es cierta entonces no se tiene que hacer nada de peticiones aplazadas
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
      return resultado;
  }
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
    private int EjCompra(int maxPrecio, int tks){
        Oferta c = null;
        //igual que Venta
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
        return resultado;
    }
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
    public int EjRes(int id) {
        Oferta oferta;
        int res = -1;
        //vemos si cumple la pre aunque no sea obligatorio
        if(compras.containsKey(id)){
            oferta = compras.get(id);
        }
        else if (ventas.containsKey(id)){
            oferta = ventas.get(id);
        }
        //damos el -1 de fallo
        else{
            return res;
        }
        //esta sería la CPRE donde se decide si aplazamos o no
        if(oferta.dinero==0 && oferta.ticks>0){
            return res;
        }
        //si cumple todas las condiciones, los rellenamos
        res = oferta.dinero;
        return res;
    }
    public void EjTick() {
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
    }

  // Código del servidor
  public void run() {
      //declaramos índices
      final int VENTA = 0;
      final int COMPRA = 1;
      final int RESOFERTA = 2;
      final int ALALTO = 3;
      final int ALBAJO = 4;
      final int TICK = 5;
      //usamos aplazable para poder guardar cualquiera de los 3 procesos
      Collection<Aplazable> aplazadas = new ArrayList<>();
      //canales
      AltingChannelInput[] entradas = {chVenta.in(),
                                     chCompra.in(),
                                     chResultadoOferta.in(),
                                     chAlertaPrecioAlto.in(),
                                     chAlertaPrecioBajo.in(),
                                     chTick.in()};
      //todas true porque 3 no tienen cpre, y las otras tres van con peticiones aplazadas
      boolean[] sincConds = {true,true,true,true,true,true};
      Alternative servicios = new Alternative(entradas);
    // Bucle principal del servicio
    while(true){
      int servicio;
      servicio = servicios.fairSelect(sincConds);
      switch (servicio){
      case VENTA:
        MessageVenta msgV = (MessageVenta) entradas[VENTA].read();
        int idV = EjVenta(msgV.minPrecio, msgV.tks);//lo mandamos a calcular
        msgV.chRes.write(idV);//se lo devolvemos al emisor
        break;
      case COMPRA:
          //sacamos el paquete que nos envia la compra
          MessageCompra msgC = (MessageCompra) entradas[COMPRA].read();
          int idC = EjCompra(msgC.maxPrecio,msgC.tks);
          msgC.chRes.write(idC);//devolvemos
          break;
      case RESOFERTA:
          MessageResOferta msgRes = (MessageResOferta) entradas[RESOFERTA].read();
          int idRes = EjRes(msgRes.id);
          //es el valor puesto para indicar si debe ser aplazada, añadimos a la lista
          if(idRes == -1){
              aplazadas.add(msgRes);
          }
          else{
              msgRes.chRes.write(idRes);
          }
          break;
      case ALBAJO:
        MessageAlertaPrecioBajo msgBajo = (MessageAlertaPrecioBajo)  entradas[ALBAJO].read();
        //sin metodo auxiliar porque es muy corto, evaluamos la condicion
        if(msgBajo.limite < min){
            aplazadas.add(msgBajo);
        }
        else{
            //si esta correcto, respodemos
            msgBajo.chRes.write(0);
        }
          break;
      case ALALTO:
        MessageAlertaPrecioAlto msgAlto = (MessageAlertaPrecioAlto) entradas[ALALTO].read();
        if(msgAlto.limite > max){
            aplazadas.add(msgAlto);
        }
        else{
            msgAlto.chRes.write(0);
        }
          break;
      case TICK:
          //tick no nos envia nada, simplemente pasamos a ejecutar
          entradas[TICK].read();
          EjTick();
          //no espera respuesta
          break;
      }
      //vamos a intentar limpiar las aplazadas
      ResAplazadas(aplazadas);
    }
  }

    private void ResAplazadas(Collection<Aplazable> aplazadas) {
      int res;
      Iterator<Aplazable> it = aplazadas.iterator();
          while(it.hasNext()){
              Aplazable a = it.next();
              //tenemos que averiguar que mensaje estamos mirando, comprobamos su clase
              if(a instanceof MessageResOferta){
                  res = EjRes(((MessageResOferta) a).id);
                  //si ahora funciona devolvemos, si no lo dejamos en la lista
                  if(res != -1){
                      ((MessageResOferta) a).chRes.write(res);
                      it.remove();
                  }
              }
              else if(a instanceof MessageAlertaPrecioAlto){
                  //comprobamos directamente la condición
                  if(((MessageAlertaPrecioAlto) a).limite <= max){
                      ((MessageAlertaPrecioAlto) a).chRes.write(0);
                      it.remove();
                  }
              }
              else if(a instanceof MessageAlertaPrecioBajo){
                  //igual que alto
                  if(((MessageAlertaPrecioBajo) a).limite >= min){
                      ((MessageAlertaPrecioBajo) a).chRes.write(0);
                      it.remove();
                  }
              }
          }

    }
}
