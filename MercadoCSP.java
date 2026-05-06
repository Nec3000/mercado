// Nunca cambia la declaracion del package!
package cc.mercado;

import org.jcsp.lang.*;

public class MercadoCSP implements Mercado, CSProcess {
  // TODO: Declaración de canales
  // Ej. private Any2One chOp;

  public MercadoCSP() {

    // TODO: Creación de canales para comunicación con el servidor
    // Ej. chOp = Channel.any2one();

    // Puesta en marcha del servidor: alternativa sucia (desde el
    // punto de vista de CSP) a Parallel que nos ofrece JCSP para
    // poner en marcha un CSProcess
    new ProcessManager(this).start();
  }

  public int venta(int minPrecio, int tks) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute venta
    return -1;
  }

  public int compra(int maxPrecio, int tks) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute compra
    return -1;
  }

  public int resultadoOferta(int id) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute resultadoOferta
    return -1;
  }

  public void alertaPrecioBajo(int limite) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute alertaPrecioBajo
  }
  
  public void alertaPrecioAlto(int limite) {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute alertaPrecioAlto
  }

  public void tick() {
    // TODO: código que ejecuta el cliente para enviar/recibir un
    // mensaje al server para que ejecute tick
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
