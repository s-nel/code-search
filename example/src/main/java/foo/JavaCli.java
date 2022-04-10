package foo;

import bar.Bar;
import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;

public class JavaCli {
    public static void main(String[] args) {
        Transaction transaction = ElasticApm.startTransaction();
        transaction.setName("JavaCli");
        try {
            new Foo(6L, new Bar("hello world")).explode(transaction);
        } catch(Exception e) {
            transaction.captureException(e);
        }
        transaction.end();
    }
}
