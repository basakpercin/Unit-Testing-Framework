import java.util.NoSuchElementException;

public class Assertion {
    /* You'll need to change the return type of the assertThat methods */
    static AssertObject assertThat(Object o) {
        return new AssertObject(o);
    }
    static AssertString assertThat(String s) {
        return new AssertString(s);
    }
    static AssertBoolean assertThat(boolean b) {
        return new AssertBoolean(b);
    }
    static AssertInteger assertThat(int i) {
        return new AssertInteger(i);
    }
}

class AssertObject {
    private final Object object;

    public AssertObject(Object object) {
        this.object = object;
    }

    //isNotNull
    public AssertObject isNotNull(){
        if(object == null){
            throw new NoSuchElementException("Object is Null");
        }
        return this;
    }

    //isNull
    public AssertObject isNull(){
        if (object != null){
            throw new NoSuchElementException("Object is not Null");
        }
        return this;
    }

    //isEqualTo(Object o2)
    public AssertObject isEqualTo(Object o2){
        if(!object.equals(o2)){
            throw new NoSuchElementException("Objects are not equal");
        }
        return this;
    }

    //isNotEqualTo(Object o2)
    public AssertObject isNotEqualTo(Object o2){
        if(object.equals(o2)){
            throw new NoSuchElementException("Objects are equal");
        }
        return this;
    }

    //isInstanceOf(Class c)
    public AssertObject isInstanceOf(Class c){
        if(!c.isInstance(object)){
            throw new NoSuchElementException("object is not an instance of c");
        }
        return this;
    }
}

class AssertString {
    private final String string;

    AssertString(String string) {
        this.string = string;
    }

    //isNotNull
    public AssertString isNotNull(){
        if(string == null){
            throw new NoSuchElementException("String is Null");
        }
        return this;
    }

    //isNull
    public AssertString isNull(){
        if (string != null){
            throw new NoSuchElementException("Object is not Null");
        }
        return this;
    }

    //isEqualTo(Object o2)
    public AssertString isEqualTo(Object o2){
        if(!string.equals(o2)){
            throw new NoSuchElementException("Objects are not equal");
        }
        return this;
    }

    //isNotEqualTo(Object o2)
    public AssertString isNotEqualTo(Object o2){
        if(string.equals(o2)){
            throw new NoSuchElementException("Objects are equal");
        }
        return this;
    }

    //startsWith(string s2) raises an exception is s does not start with s2
    public AssertString startsWith(String s2){
        if(!string.startsWith(s2)){
            throw new NoSuchElementException("string does not start with " + s2);
        }
        return this;
    }

    //isEmpty() raises an exception if s in not the empty string
    public AssertString isEmpty(){
        if(!string.isEmpty()){
            throw new NoSuchElementException("string is not empty");
        }
        return this;
    }

    //contains(string s2) raises an exception if s does not contain s2.
    public AssertString contains(String s2){
        if(!string.contains(s2)){
            throw new NoSuchElementException("string does not contain input string");
        }
        return this;
    }
}

class AssertBoolean {

    private final boolean b;

    AssertBoolean(Boolean b) {
        this.b = b;
    }

    //    isEqualTo(boolean b2) raises an exception if b != b2.

    public AssertBoolean isEqualTo(boolean b2){
        if(b != b2){
            throw new NoSuchElementException("Booleans are not equal");
        }
        return this;
    }


//    isTrue() raises an exception if b is false.
    public AssertBoolean isTrue(){
        if(!b){
            throw new NoSuchElementException("Boolean is not true");
        }
        return this;
    }

//    isFalse() raises an exception if b is true.
    public AssertBoolean isFalse(){
        if(b){
            throw new NoSuchElementException("Boolean is not false");
        }
        return this;
    }
}

class AssertInteger {
    private final int i;

    AssertInteger(int i) {
        this.i = i;
    }

    // isEqualTo(int i2) raises an exception if i != i2.
    public AssertInteger isEqualTo(int i2){
        if(i != i2){
            throw new NoSuchElementException("numbers are not equal");
        }
        return this;
    }

    // isLessThan(int i2) raises an exception if i >= i2.
    public AssertInteger isLessThan(int i2){
        if(i >= i2){
            throw new NoSuchElementException("i is not less than " + i2);
        }
        return this;
    }

    // isGreaterThan(int i2) raises an exception if i <= i2.
    public AssertInteger isGreaterThan(int i2){
        if(i <= i2){
            throw new NoSuchElementException("i is not greater than " + i2);
        }
        return this;
    }
}