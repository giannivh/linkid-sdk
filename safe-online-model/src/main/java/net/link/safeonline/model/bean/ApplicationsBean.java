package net.link.safeonline.model.bean;

import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.authentication.exception.ApplicationIdentityNotFoundException;
import net.link.safeonline.authentication.exception.ApplicationNotFoundException;
import net.link.safeonline.dao.ApplicationDAO;
import net.link.safeonline.dao.ApplicationIdentityDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.ApplicationIdentityAttributeEntity;
import net.link.safeonline.entity.ApplicationIdentityEntity;
import net.link.safeonline.model.Applications;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


@Stateless
public class ApplicationsBean implements Applications {

    private static final Log       LOG = LogFactory.getLog(ApplicationsBean.class);

    @EJB
    private ApplicationDAO         applicationDAO;

    @EJB
    private ApplicationIdentityDAO applicationIdentityDAO;


    public ApplicationEntity getApplication(String applicationName) throws ApplicationNotFoundException {

        return this.applicationDAO.getApplication(applicationName);
    }

    public List<ApplicationEntity> listApplications() {

        List<ApplicationEntity> applications = this.applicationDAO.listApplications();
        return applications;
    }

    public List<ApplicationEntity> listUserApplications() {

        List<ApplicationEntity> applications = this.applicationDAO.listUserApplications();
        return applications;
    }

    public Set<ApplicationIdentityAttributeEntity> getCurrentApplicationIdentity(ApplicationEntity application)
            throws ApplicationIdentityNotFoundException {

        LOG.debug("get current application identity: " + application.getName());

        long currentIdentityVersion = application.getCurrentApplicationIdentity();
        ApplicationIdentityEntity applicationIdentity = this.applicationIdentityDAO.getApplicationIdentity(application,
                currentIdentityVersion);
        Set<ApplicationIdentityAttributeEntity> attributes = applicationIdentity.getAttributes();
        for (ApplicationIdentityAttributeEntity attribute : attributes) {
            LOG.debug("attribute: " + attribute);
        }
        return attributes;
    }
}
