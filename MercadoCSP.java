// Nunca cambia la declaracion del package!
package cc.mercado;

import org.jcsp.lang.*;

import java.util.HashMap;


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
  private static class MessageAlertaPrecioBajo{
    int limite;
    ChannelOutputInt chRes;
    MessageAlertaPrecioBajo(int limite, ChannelOutputInt chRes){
      this.limite=limite;
      this.chRes=chRes;
    }
  }
  private static class MessageAlertaPrecioAlto{
    int limite;
    ChannelOutputInt chRes;
    MessageAlertaPrecioAlto(int limite, ChannelOutputInt chRes){
      this.limite=limite;
      this.chRes=chRes;
    }
  }
  private static class MessageResOferta{
    int id;
    ChannelOutputInt chRes;
    MessageResOferta(int id, ChannelOutputInt chRes){
      this.id=id;
      this.chRes=chRes;
    }
  }

  public MercadoCSP() {
    chVenta=Channel.any2one();
    chCompra=Channel.any2one();
    chResultadoOferta=Channel.any2one();
    chAlertaPrecioAlto=Channel.any2one();
    chAlertaPrecioBajo=Channel.any2one();
    chTick=Channel.any2one();
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

  // Código del servidor
  public void run() {
    // TODO: declaración e inicialización del estado del recurso
      final int VENTA = 0;
      final int COMPRA = 1;
      final int RESOFERTA = 2;
      final int ALBAJO = 3;
      final int ALALTO = 4;
      final int TICK = 5;
    // TODO: declaración e inicialización de estructuras de datos para
    // almacenar peticiones de los clientes
      boolean[] sincConds = new boolean[6];
      sincConds[VENTA]=true;
      sincConds[COMPRA]=true;

      sincConds[TICK]=true;

    // TODO: declaración e inicialización de arrays necesarios para
    // poder hacer la recepción no determinista (Alternative)
    // TODO: cambiar null por el array de canales
    AltingChannelInput[] entradas = {chVenta.in(),
                                     chCompra.in(),
                                     chResultadoOferta.in(),
                                     chAlertaPrecioAlto.in(),
                                     chAlertaPrecioBajo.in(),
                                     chTick.in()};

    Alternative servicios = new Alternative(entradas);

    // Bucle principal del servicio
    while(true){
      // TODO: declaración de variables auxiliares
      int servicio;

      // TODO: cálculo de las guardas

      // TODO: cambiar null por el array de guardas
      servicio = servicios.fairSelect(sincConds);

      // TODO: ejecutar la operación solicitada por el cliente
      switch (servicio){
      case VENTA:
        // TODO: ejecutar operación 0 o almacenar la petición y
        // responder al cliente si es posible
        MessageVenta msgV = (MessageVenta) entradas[VENTA].read();
        int id = EjVenta(msgV.minPrecio, msgV.tks);
        break;
      case COMPRA:
          //sacamos el paquete que nos envia la compra
          MessageCompra msgC = (MessageCompra) entradas[COMPRA].read();

          break;
      case RESOFERTA:
          break;
      case ALBAJO:
          break;
      case ALALTO:
          break;
      case TICK:
          break;
      }

      // TODO: atender peticiones pendientes que puedan ser atendidas
    }
  }
}
