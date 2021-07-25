package geniusweb.custom.junk;

import java.lang.reflect.InvocationTargetException;

public class Representation<T> {

    private T container; 
    private Class<T> TClass; 

    public Representation(T initialVal) throws Exception{
        this.container = TClass.getDeclaredConstructor(TClass).newInstance(initialVal);
    }

    public T getContainer() {
        return container;
    }

    public void setContainer(T container) {
        this.container = container;
    }
    
}
