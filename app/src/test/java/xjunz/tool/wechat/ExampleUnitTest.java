package xjunz.tool.wechat;

import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testEqual() {
        Foo a = new Foo("a", 1);
        Foo b = new Foo("a", 1);
        System.out.println(a.equals(b));
    }

    private static class Foo {
        String a;
        int b;

        public Foo(String a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Foo foo = (Foo) o;
            return b == foo.b &&
                    Objects.equals(a, foo.a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }
}