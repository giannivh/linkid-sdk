package net.link.safeonline.demo.cinema.webapp;

import org.apache.wicket.Page;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.wicketstuff.javaee.injection.JavaEEComponentInjector;

public class CinemaApplication extends WebApplication {

    @Override
    protected void init() {

        addComponentInstantiationListener(new JavaEEComponentInjector(this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Page<?>> getHomePage() {

        return LoginPage.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session newSession(Request request, Response response) {

        return new CinemaSession(request);
    }

}
