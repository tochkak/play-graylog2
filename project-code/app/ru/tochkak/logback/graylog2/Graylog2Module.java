package ru.tochkak.logback.graylog2;

import play.api.Configuration;
import play.api.Environment;
import play.api.inject.Binding;
import play.api.inject.Module;
import scala.collection.Seq;

import javax.inject.Singleton;

@SuppressWarnings("unused")

public class Graylog2Module extends Module {
    @Override
    public Seq<Binding<?>> bindings(Environment environment, Configuration configuration) {
        return seq(
                bind(Graylog2Component.class).to(Graylog2Impl.class).in(Singleton.class)
        );
    }
}
