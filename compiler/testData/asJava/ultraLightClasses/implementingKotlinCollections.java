public final class MyList /* MyList*/ implements java.util.List<java.lang.String>, kotlin.collections.List<java.lang.String>, kotlin.jvm.internal.markers.KMappedMarker {
  @null()
  public  MyList();

  @org.jetbrains.annotations.NotNull()
  public java.lang.String get(int);

}

public abstract interface ASet /* ASet*/<T>  extends java.util.Collection<T>, kotlin.collections.MutableCollection<T>, kotlin.jvm.internal.markers.KMutableCollection {
}

public abstract class MySet /* MySet*/<T>  implements ASet<T> {
  @null()
  public  MySet();

  public boolean remove(@org.jetbrains.annotations.NotNull() java.lang.String);

}

public abstract class SmartSet /* SmartSet*/<T>  extends java.util.AbstractSet<T> {
  @org.jetbrains.annotations.NotNull()
  public java.util.Iterator<T> iterator();

  private  SmartSet();

  public boolean add(@null() T);

  public boolean contains(@null() T);

}