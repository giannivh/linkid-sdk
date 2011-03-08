package net.link.safeonline.sdk.auth.protocol;

import java.security.cert.X509Certificate;
import java.util.List;


/**
 * <h2>{@link LogoutProtocolResponseContext}<br> <sub>[in short] (TODO).</sub></h2>
 *
 * <p> <i>08 17, 2010</i> </p>
 *
 * @author lhunath
 */
public class LogoutProtocolResponseContext extends ProtocolResponseContext {

    private final boolean success;

    /**
     * @param request          Logout Request this response is a response to.
     * @param id               Logout Response ID
     * @param success          Whether the logout response reports a successful and non-partial (complete) single logout.
     * @param certificateChain Optional certificate chain if protocol response was signed and contained the chain embedded in the
     *                         signature.
     */
    public LogoutProtocolResponseContext(LogoutProtocolRequestContext request, String id, boolean success,
                                         List<X509Certificate> certificateChain) {

        super( request, id, certificateChain );

        this.success = success;
    }

    @Override
    public LogoutProtocolRequestContext getRequest() {

        return (LogoutProtocolRequestContext) super.getRequest();
    }

    /**
     * @return Whether the logout response reports a successful and non-partial (complete) single logout.
     */
    public boolean isSuccess() {

        return success;
    }
}
