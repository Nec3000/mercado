// Nunca cambia la declaracion del package!
package cc.mercado;

import org.jcsp.lang.*;


public class MercadoCSP implements Mercado, CSProcess {
  private Any2OneChannel chVenta;
  private Any2OneChannel chCompra;
  private Any2OneChannel chResultadoOferta;
  private Any2OneChannel chAlertaPrecioBajo;
  private Any2OneChannel chAlertaPrecioAlto;
  private Any2OneChannel chTick;

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

  // Código del servidor
  public void run() {
    // TODO: declaración e inicialización del estado del recurso

    // TODO: declaración e inicialización de estructuras de datos para
    // almacenar peticiones de los clientes

    // TODO: declaración e inicialización de arrays necesarios para
    // poder hacer la recepción no determinista (Alternative)

    // TODO: cambiar null por el array de canales
    Alternative servicios = new Alternative(null);

    // Bucle principal del servicio
    while(true){
      // TODO: declaración de variables auxiliares
      int servicio;

      // TODO: cálculo de las guardas

      // TODO: cambiar null por el array de guardas
      servicio = servicios.fairSelect(null);

      // TODO: ejecutar la operación solicitada por el cliente
      switch (servicio){
      case 0:
        // TODO: ejecutar operación 0 o almacenar la petición y
        // responder al cliente si es posible

        break;
      }

      // TODO: atender peticiones pendientes que puedan ser atendidas
    }
  }
}
