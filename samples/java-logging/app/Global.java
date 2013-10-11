import org.graylog2.logback.appender.AccessLog;
import play.GlobalSettings;
import play.api.mvc.EssentialFilter;

@SuppressWarnings("unused")
public class Global extends GlobalSettings {
    @Override
    @SuppressWarnings("unchecked")
    public <T extends EssentialFilter> Class<T>[] filters() {
        return new Class[] {AccessLog.class};
    }
}
