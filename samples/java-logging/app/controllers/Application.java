package controllers;

import play.*;
import play.mvc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import views.html.*;

public class Application extends Controller {
    private static final Logger log = LoggerFactory.getLogger(Application.class);
    public static Result index() {
    	log.info("index");
    	log.info("index2");
    	log.info("index3");
    	log.info("index4");
        return ok(index.render("Your new application is ready."));
    }

}
