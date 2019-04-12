package ru.tochkak.logback.graylog2;

import com.typesafe.config.Config;
import play.Environment;
import play.inject.Binding;
import play.inject.Module;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")

public class Graylog2Module extends Module {

    public List<Binding<?>> bindings(Environment environment, Config configuration) {
        return Collections.singletonList(
            bindClass(Graylog2Component.class).to(Graylog2Impl.class).in(Singleton.class)
        );
    }
}
