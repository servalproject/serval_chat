package org.servalproject.mid;

/**
 * Created by jeremy on 24/10/16.
 */
public class Interface {
    public final int id;
    public final String name;

    public Interface(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Interface that = (Interface) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
