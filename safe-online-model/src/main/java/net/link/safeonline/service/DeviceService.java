package net.link.safeonline.service;

import java.util.List;

import javax.ejb.Local;

import net.link.safeonline.authentication.exception.DeviceClassDescriptionNotFoundException;
import net.link.safeonline.authentication.exception.DeviceClassNotFoundException;
import net.link.safeonline.authentication.exception.DeviceDescriptionNotFoundException;
import net.link.safeonline.authentication.exception.DeviceNotFoundException;
import net.link.safeonline.authentication.exception.DevicePropertyNotFoundException;
import net.link.safeonline.authentication.exception.ExistingDeviceClassException;
import net.link.safeonline.authentication.exception.ExistingDeviceException;
import net.link.safeonline.entity.AllowedDeviceEntity;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.DeviceClassDescriptionEntity;
import net.link.safeonline.entity.DeviceClassEntity;
import net.link.safeonline.entity.DeviceDescriptionEntity;
import net.link.safeonline.entity.DeviceEntity;
import net.link.safeonline.entity.DevicePropertyEntity;
import net.link.safeonline.entity.RegisteredDeviceEntity;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.pkix.exception.CertificateEncodingException;

@Local
public interface DeviceService {

	List<DeviceEntity> listDevices();

	List<AllowedDeviceEntity> listAllowedDevices(ApplicationEntity application);

	void setAllowedDevices(ApplicationEntity application,
			List<AllowedDeviceEntity> allowedDeviceList);

	List<DeviceClassEntity> listDeviceClasses();

	List<RegisteredDeviceEntity> listRegisteredDevices(SubjectEntity subject);

	List<DeviceDescriptionEntity> listDeviceDescriptions(String deviceName)
			throws DeviceNotFoundException;

	List<DevicePropertyEntity> listDeviceProperties(String deviceName)
			throws DeviceNotFoundException;

	void addDeviceDescription(DeviceDescriptionEntity newDeviceDescription)
			throws DeviceNotFoundException;

	void removeDeviceDescription(DeviceDescriptionEntity description)
			throws DeviceDescriptionNotFoundException;

	void addDeviceProperty(DevicePropertyEntity newDeviceProperty)
			throws DeviceNotFoundException;

	void removeDeviceProperty(DevicePropertyEntity property)
			throws DevicePropertyNotFoundException;

	void addDevice(String name, String deviceClassName,
			String authenticationURL, String registrationURL,
			String removalURL, byte[] encodedCertificate)
			throws CertificateEncodingException, DeviceClassNotFoundException,
			ExistingDeviceException;

	List<DeviceClassDescriptionEntity> listDeviceClassDescriptions(
			String deviceClassName) throws DeviceClassNotFoundException;

	void addDeviceClassDescription(
			DeviceClassDescriptionEntity newDeviceClassDescription)
			throws DeviceClassNotFoundException;

	void removeDeviceClassDescription(DeviceClassDescriptionEntity description)
			throws DeviceClassDescriptionNotFoundException;

	void addDeviceClass(String name) throws ExistingDeviceClassException;

	void updateAuthenticationUrl(String deviceName, String authenticationURL)
			throws DeviceNotFoundException;

	void updateRegistrationUrl(String deviceName, String registrationURL)
			throws DeviceNotFoundException;

	void updateRemovalUrl(String deviceName, String removalURL)
			throws DeviceNotFoundException;

	void updateDeviceCertificate(String deviceName, byte[] encodedCertificate)
			throws DeviceNotFoundException, CertificateEncodingException;

	void saveDeviceDescription(DeviceDescriptionEntity description);

	void saveDeviceProperty(DevicePropertyEntity property);

	void saveDeviceClassDescription(DeviceClassDescriptionEntity description);

}
