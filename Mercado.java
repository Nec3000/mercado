package cc.mercado;

public interface Mercado {
  public int venta(int minPrecio, int tks);
  public int compra(int maxPrecio, int tks);
  public int resultadoOferta(int id);
  public void alertaPrecioBajo(int limite);
  public void alertaPrecioAlto(int limite);
  public void tick();
}

