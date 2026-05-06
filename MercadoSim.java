package cc.mercado;

import java.util.Random;

import java.util.List;
import java.util.ArrayList;

import es.upm.babel.cclib.ConcIO;


public class MercadoSim {

  static final int MAX_AGENTES = 5;
  static final int MAX_ALERTAS = 5;

  public static void main(String[] args) {
    Mercado mercado;
    Generador g = new Generador();
    List<Integer> ids = new ArrayList<>();

    // Por defecto el simulador usa la implementacion programado con monitores
    // Para comprobar la implementacion usando CSP cambia las dos lineas abajo.
    mercado = new MercadoMonitor();
    // mercado = new MercadoCSP(); // arranca servidor!!

    // Crea procesos
    for (int i=0; i<MAX_AGENTES; i++)
      new Comprador(mercado,ids,g).start();

    for (int i=0; i<MAX_AGENTES; i++)
      new Vendedor(mercado,ids,g).start();

    new Consultor(mercado,ids,g).start();
    new Reloj(mercado,g).start();

    for (int i=0; i<MAX_ALERTAS; i++)
      new Alertar(mercado,g).start();
  }
}


class Generador {
  private final int MAX_VALOR = 10;
  private Random r = new Random();

  public Generador()
  {
  }
  
  /**
   * Devuele un entero positivo entre 1 y max.
   */
  public int positivo(int max)
  {
    return 1 + r.nextInt(max);
  }
}


class Comprador extends Thread {
  private Mercado m;
  private List<Integer> ids;
  private Generador g;

  public Comprador(Mercado m, List<Integer> ids, Generador g) {
    this.m = m;
    this.ids = ids;
    this.g = g;
  }

  public void run() {
    int p = 1;
    int t = 1;
    int id = 1;
    while (true) {
      p = g.positivo(100);
      t = g.positivo(4)-1;

      try { id = m.compra(p,t);
        ids.add(id);
        // Imprimir información sobre
        // la transferencia realizada
        ConcIO.printfnl("Compra %d: %d", p, t);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Compra %d: %d lanzó la excepción IllegalArgumentException", p,t);
      }
      try { sleep(g.positivo(6000)); } catch (InterruptedException exc) { }
    }
  }
}

class Vendedor extends Thread {
  private Mercado m;
  private List<Integer> ids;
  private Generador g;

  public Vendedor(Mercado m, List<Integer> ids, Generador g) {
    this.m = m;
    this.ids = ids;
    this.g = g;
  }

  public void run() {
    int p = 1;
    int t = 1;
    int id = 1;
    
    while (true) {
      p = g.positivo(100);
      t = g.positivo(4)-1;

      try { id = m.venta(p,t);
        ids.add(id);
        // Imprimir información sobre
        // la transferencia realizada
        ConcIO.printfnl("Venta %d: %d", p, t);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Venta %d: %d lanzó la excepción IllegalArgumentException", p,t);
      }
      try { sleep(g.positivo(6000)); } catch (InterruptedException exc) { }
    }
  }
}

class Consultor extends Thread {
  private Mercado m;
  private List<Integer> ids;
  private Generador g;

  public Consultor(Mercado m, List<Integer> ids, Generador g) {
    this.m = m;
    this.ids = ids;
    this.g = g;
  }

  public void run() {
    while (true) {
      int id = 1;
      int d = 0;
      
      try {
        id = g.positivo(ids.size())-1;
        d = m.resultadoOferta(id);
        // Imprime información sobre
        // el saldo disponible
        ConcIO.printfnl("Resultado de oferta %d: %d", id, d);
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Resultado de la oferta %d lanzó la excepción IllegalArgumentException",
                        id);
      }

      try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
    }
  }
}

class Alertar extends Thread {
  private Mercado m;
  private Generador g;

  public Alertar(Mercado m, Generador g) {
    this.m = m;
    this.g = g;
  }

  public void run() {
    while (true) {

      int limit = 1;
      int choice = 1;

      try {
        choice = g.positivo(2);
        limit = g.positivo(100);
        if (choice == 1)
          m.alertaPrecioBajo(limit);
        else
          m.alertaPrecioAlto(limit);
        // Imprime información sobre
        // el saldo disponible
        ConcIO.printfnl("Alertar %d: termino", limit);
      } catch (IllegalArgumentException exc) {
          if (choice == 1) 
            ConcIO.printfnl("La llamada alertaPrecioBajo %d lanzó la excepción IllegalArgumentException",
                            limit);
          else 
            ConcIO.printfnl("La llamada alertaPrecioAlto %d lanzó la excepción IllegalArgumentException",
                            limit);
      }

      try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
    }
  }
}

class Reloj extends Thread {
  private Mercado m;
  private Generador g;

  public Reloj(Mercado m, Generador g) {
    this.m = m;
    this.g = g;
  }

  public void run() {
    while (true) {

      try {
        try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
        m.tick();
        // Imprime información sobre
        // el saldo disponible
        ConcIO.printfnl("Tick");
      } catch (IllegalArgumentException exc) {
        ConcIO.printfnl("Tick lanzó la excepción IllegalArgumentException");
      }

      try { sleep(g.positivo(2000)); } catch (InterruptedException exc) { }
    }
  }
}
