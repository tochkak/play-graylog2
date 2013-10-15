package controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Application extends Controller {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static Result index() {
    	log.info("index");
    	log.info("index2");
    	log.info("index3");
    	log.info("index4");
        return ok(index.render("Your new application is ready."));
    }

    public static Result chunked() {
        final String[] strings = new String[] {"abc", "cde", "efg"};
        Chunks<String> chunks = new StringChunks() {
            @Override
            public void onReady(Out<String> out) {
                for (String string : strings) {
                    out.write(string);
                }
                out.close();
            }
        };
        return ok(chunks);
    }

}
