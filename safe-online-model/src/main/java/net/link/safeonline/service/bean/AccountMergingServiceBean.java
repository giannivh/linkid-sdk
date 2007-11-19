/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.service.bean;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

import net.link.safeonline.SafeOnlineConstants;
import net.link.safeonline.authentication.exception.AttributeTypeNotFoundException;
import net.link.safeonline.authentication.exception.SubjectNotFoundException;
import net.link.safeonline.authentication.service.AttributeDO;
import net.link.safeonline.common.SafeOnlineRoles;
import net.link.safeonline.dao.AttributeDAO;
import net.link.safeonline.dao.AttributeTypeDAO;
import net.link.safeonline.dao.SubscriptionDAO;
import net.link.safeonline.entity.ApplicationEntity;
import net.link.safeonline.entity.AttributeEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionEntity;
import net.link.safeonline.entity.AttributeTypeDescriptionPK;
import net.link.safeonline.entity.AttributeTypeEntity;
import net.link.safeonline.entity.CompoundedAttributeTypeMemberEntity;
import net.link.safeonline.entity.DatatypeType;
import net.link.safeonline.entity.SubjectEntity;
import net.link.safeonline.entity.SubscriptionEntity;
import net.link.safeonline.model.SubjectManager;
import net.link.safeonline.service.AccountMergingDO;
import net.link.safeonline.service.AccountMergingService;
import net.link.safeonline.service.SubjectService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.security.SecurityDomain;

@Stateless
@SecurityDomain(SafeOnlineConstants.SAFE_ONLINE_SECURITY_DOMAIN)
public class AccountMergingServiceBean implements AccountMergingService {

	private static final Log LOG = LogFactory
			.getLog(AccountMergingServiceBean.class);

	@EJB
	private SubjectService subjectService;

	@EJB
	private SubjectManager subjectManager;

	@EJB
	private SubscriptionDAO subscriptionDAO;

	@EJB
	private AttributeDAO attributeDAO;

	@EJB
	private AttributeTypeDAO attributeTypeDAO;

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public AccountMergingDO getAccountMergingDO(String sourceAccountName)
			throws SubjectNotFoundException, AttributeTypeNotFoundException {
		LOG.debug("merge account: " + sourceAccountName);
		SubjectEntity targetSubject = this.subjectManager.getCallerSubject();
		SubjectEntity sourceSubject = this.subjectService
				.getSubjectFromUserName(sourceAccountName);

		AccountMergingDO accountMergingDO = new AccountMergingDO(sourceSubject);

		List<SubscriptionEntity> targetSubscriptions = this.subscriptionDAO
				.listSubsciptions(targetSubject);
		List<SubscriptionEntity> sourceSubscriptions = this.subscriptionDAO
				.listSubsciptions(sourceSubject);
		mergeSubscriptions(accountMergingDO, targetSubscriptions,
				sourceSubscriptions);

		Map<AttributeTypeEntity, List<AttributeEntity>> targetAttributes = this.attributeDAO
				.listAttributes(targetSubject);
		Map<AttributeTypeEntity, List<AttributeEntity>> sourceAttributes = this.attributeDAO
				.listAttributes(sourceSubject);
		mergeAttributes(accountMergingDO, targetAttributes, sourceAttributes);

		return accountMergingDO;
	}

	@RolesAllowed(SafeOnlineRoles.USER_ROLE)
	public void mergeAccount(AccountMergingDO accountMergingDO) {
		LOG.debug("commit merge with account "
				+ accountMergingDO.getSourceSubject().getUserId());

	}

	/**
	 * Merge subscriptions :
	 * 
	 * When a subscription does already exist in the target: do not move
	 * 
	 * When a subscription does not yet exist: move if the source account holder
	 * has proven possession of a device from the allowed devices list of the
	 * application. When there is no allowed devices list: any device is
	 * sufficient.
	 * 
	 * @param targetSubscriptions
	 * @param sourceSubscriptions
	 */
	private void mergeSubscriptions(AccountMergingDO accountMergingDO,
			List<SubscriptionEntity> targetSubscriptions,
			List<SubscriptionEntity> sourceSubscriptions) {
		accountMergingDO.setPreservedSubscriptions(targetSubscriptions);
		for (SubscriptionEntity sourceSubscription : sourceSubscriptions) {
			SubscriptionEntity targetSubscription = getSubscription(
					targetSubscriptions, sourceSubscription.getApplication());
			if (null == targetSubscription) {
				accountMergingDO.addImportedSubscription(sourceSubscription);
			}
		}
	}

	private SubscriptionEntity getSubscription(
			List<SubscriptionEntity> subscriptions,
			ApplicationEntity application) {
		for (SubscriptionEntity subscription : subscriptions) {
			if (subscription.getApplication().equals(application))
				return subscription;
		}
		return null;
	}

	/**
	 * Merge attributes
	 * 
	 * no corresponding attribute exists: move to target
	 * 
	 * multivalued attributes: add values to target
	 * 
	 * single valued, user editable: user can choose
	 * 
	 * single valued, non user editable: use target attributes.
	 * 
	 * @param targetAttributes
	 * @param sourceAttributes
	 * @throws AttributeTypeNotFoundException
	 */
	private void mergeAttributes(AccountMergingDO accountMergingDO,
			Map<AttributeTypeEntity, List<AttributeEntity>> targetAttributes,
			Map<AttributeTypeEntity, List<AttributeEntity>> sourceAttributes)
			throws AttributeTypeNotFoundException {

		Map<AttributeTypeEntity, List<AttributeEntity>> preservedAttributeMap = new HashMap<AttributeTypeEntity, List<AttributeEntity>>(
				targetAttributes);
		List<AttributeEntity> importedAttributesList = new LinkedList<AttributeEntity>();
		List<AttributeEntity> mergedAttributesList = new LinkedList<AttributeEntity>();
		Map<List<AttributeEntity>, List<AttributeEntity>> choosableAttributesMap = new HashMap<List<AttributeEntity>, List<AttributeEntity>>();

		for (Entry<AttributeTypeEntity, List<AttributeEntity>> sourceAttribute : sourceAttributes
				.entrySet()) {
			AttributeTypeEntity sourceAttributeType = sourceAttribute.getKey();
			LOG.debug("handling attribute: " + sourceAttributeType.getName()
					+ " ( " + sourceAttributeType.getType() + " ) ");
			Entry<AttributeTypeEntity, List<AttributeEntity>> targetAttribute = getAttribute(
					targetAttributes, sourceAttributeType);
			if (null == targetAttribute) {
				importedAttributesList.addAll(sourceAttribute.getValue());
			} else if (sourceAttributeType.isMultivalued()) {
				mergeAttribute(targetAttribute, sourceAttribute);
				preservedAttributeMap.remove(sourceAttributeType);
				mergedAttributesList.addAll(targetAttribute.getValue());
			} else if (sourceAttributeType.isUserEditable()) {
				preservedAttributeMap.remove(sourceAttributeType);
				choosableAttributesMap.put(targetAttribute.getValue(),
						sourceAttribute.getValue());
			}
		}
		for (Entry<AttributeTypeEntity, List<AttributeEntity>> preservedAttribute : preservedAttributeMap
				.entrySet()) {
			List<AttributeEntity> preservedAttributes = preservedAttribute
					.getValue();
			accountMergingDO.addPreservedAttributes(mapAttributes(
					preservedAttributes, null));
		}
		accountMergingDO.setImportedAttributes(mapAttributes(
				importedAttributesList, null));
		accountMergingDO.setMergedAttributes(mapAttributes(
				mergedAttributesList, null));
		for (Entry<List<AttributeEntity>, List<AttributeEntity>> choosableAttribute : choosableAttributesMap
				.entrySet()) {
			accountMergingDO.addChoosableAttributes(mapAttributes(
					choosableAttribute.getKey(), null), mapAttributes(
					choosableAttribute.getValue(), null));
		}
	}

	/**
	 * Merges two multivalued attributes, removing doubles
	 * 
	 * @param sourceAttribute
	 * @param targetAttribute
	 */
	private void mergeAttribute(
			Entry<AttributeTypeEntity, List<AttributeEntity>> targetAttribute,
			Entry<AttributeTypeEntity, List<AttributeEntity>> sourceAttribute) {
		for (AttributeEntity attributeValue : sourceAttribute.getValue()) {
			if (!getAttributeValue(targetAttribute.getValue(), attributeValue)) {
				attributeValue.setAttributeIndex(targetAttribute.getValue()
						.size());
				targetAttribute.getValue().add(attributeValue);
			}
		}
	}

	private boolean getAttributeValue(List<AttributeEntity> attributeValues,
			AttributeEntity searchAttributeValue) {
		for (AttributeEntity attributeValue : attributeValues) {
			if (attributeValue.getValue().equals(
					searchAttributeValue.getValue()))
				return true;
		}
		return false;
	}

	private Entry<AttributeTypeEntity, List<AttributeEntity>> getAttribute(
			Map<AttributeTypeEntity, List<AttributeEntity>> attributes,
			AttributeTypeEntity attributeType) {
		for (Entry<AttributeTypeEntity, List<AttributeEntity>> attribute : attributes
				.entrySet()) {
			if (attribute.getKey().equals(attributeType))
				return attribute;
		}
		return null;
	}

	private List<AttributeDO> mapAttributes(List<AttributeEntity> attributes,
			String language) throws AttributeTypeNotFoundException {
		/*
		 * At the top of the result list we put the attributes that are not a
		 * member of a compounded attribute.
		 */
		List<AttributeDO> attributesView = new LinkedList<AttributeDO>();
		for (AttributeEntity attribute : attributes) {
			AttributeTypeEntity attributeType = attribute.getAttributeType();

			if (attributeType.isCompoundMember()) {
				continue;
			}
			if (attributeType.isCompounded()) {
				/*
				 * The database also contains attribute entities for the
				 * compounded attribute type itself. This attribute stores the
				 * UUID of the corresponding compounded attribute record. This
				 * UUID is used for identification of the compounded attribute
				 * record by the data web service. This UUID serves no purpose
				 * in the communication between SafeOnline core and the
				 * SafeOnline User Web Application. So we can simply skip this
				 * compounded attribute entry.
				 */
				continue;
			}

			LOG.debug("attribute pk type: "
					+ attribute.getPk().getAttributeType());
			LOG.debug("attribute type: " + attributeType.getName());
			String name = attributeType.getName();
			String stringValue = attribute.getStringValue();
			Boolean booleanValue = attribute.getBooleanValue();
			boolean editable = attributeType.isUserEditable();
			DatatypeType datatype = attributeType.getType();
			boolean multivalued = attributeType.isMultivalued();
			long index = attribute.getAttributeIndex();

			String humanReadableName = null;
			String description = null;
			if (null != language) {
				LOG.debug("trying language: " + language);
				AttributeTypeDescriptionEntity attributeTypeDescription = this.attributeTypeDAO
						.findDescription(new AttributeTypeDescriptionPK(name,
								language));
				if (null != attributeTypeDescription) {
					LOG.debug("found description");
					humanReadableName = attributeTypeDescription.getName();
					description = attributeTypeDescription.getDescription();
				}
			}

			AttributeDO attributeView = new AttributeDO(name, datatype,
					multivalued, index, humanReadableName, description,
					editable, true, stringValue, booleanValue);
			attributeView.setValue(attribute);
			attributesView.add(attributeView);
		}

		/*
		 * Next we put the compounded member attribute in their compounded
		 * attribute context. First we need to determine which compounded
		 * attribute groups we need to visualize.
		 */

		/*
		 * compoundedAttributes: First map is keyed per compounded attribute
		 * type. Second map is keyed via the compounded member attribute type.
		 * Third map is keyed via attribute index.
		 */
		Map<AttributeTypeEntity, Map<AttributeTypeEntity, Map<Long, AttributeEntity>>> compoundedAttributes = new HashMap<AttributeTypeEntity, Map<AttributeTypeEntity, Map<Long, AttributeEntity>>>();
		/*
		 * numberOfRecordsPerCompounded: per compounded attribute type we keep
		 * track of the number of data records that we're required to visualize.
		 */
		Map<AttributeTypeEntity, Long> numberOfRecordsPerCompounded = new HashMap<AttributeTypeEntity, Long>();
		for (AttributeEntity attribute : attributes) {
			AttributeTypeEntity attributeType = attribute.getAttributeType();
			if (false == attributeType.isCompoundMember()) {
				continue;
			}
			AttributeTypeEntity compoundedAttributeType = this.attributeTypeDAO
					.getParent(attributeType);
			Map<AttributeTypeEntity, Map<Long, AttributeEntity>> members = compoundedAttributes
					.get(compoundedAttributeType);
			if (null == members) {
				members = new HashMap<AttributeTypeEntity, Map<Long, AttributeEntity>>();
				compoundedAttributes.put(compoundedAttributeType, members);
			}
			Map<Long, AttributeEntity> memberAttributes = members
					.get(attributeType);
			if (null == memberAttributes) {
				memberAttributes = new HashMap<Long, AttributeEntity>();
				members.put(attributeType, memberAttributes);
			}
			long attributeIndex = attribute.getAttributeIndex();
			memberAttributes.put(attributeIndex, attribute);

			Long numberOfRecords = numberOfRecordsPerCompounded
					.get(compoundedAttributeType);
			if (null == numberOfRecords) {
				// long live auto-boxing NPEs... big step forward
				numberOfRecords = 1L;
			}
			if (numberOfRecords < attributeIndex + 1) {
				numberOfRecords = attributeIndex + 1;
			}
			numberOfRecordsPerCompounded.put(compoundedAttributeType,
					numberOfRecords);
		}

		for (Map.Entry<AttributeTypeEntity, Map<AttributeTypeEntity, Map<Long, AttributeEntity>>> compoundedAttributeEntry : compoundedAttributes
				.entrySet()) {
			AttributeTypeEntity compoundedAttributeType = compoundedAttributeEntry
					.getKey();
			Map<AttributeTypeEntity, Map<Long, AttributeEntity>> membersMap = compoundedAttributeEntry
					.getValue();

			String humanReadableName = null;
			String description = null;
			if (null != language) {
				AttributeTypeDescriptionEntity attributeTypeDescription = this.attributeTypeDAO
						.findDescription(new AttributeTypeDescriptionPK(
								compoundedAttributeType.getName(), language));
				if (null != attributeTypeDescription) {
					LOG.debug("found description");
					humanReadableName = attributeTypeDescription.getName();
					description = attributeTypeDescription.getDescription();
				}
			}

			long numberOfRecords = numberOfRecordsPerCompounded
					.get(compoundedAttributeType);
			for (long idx = 0; idx < numberOfRecords; idx++) {
				AttributeDO compoundedAttributeView = new AttributeDO(
						compoundedAttributeType.getName(),
						DatatypeType.COMPOUNDED, true, idx, humanReadableName,
						description, compoundedAttributeType.isUserEditable(),
						false, null, null);
				compoundedAttributeView.setCompounded(true);
				attributesView.add(compoundedAttributeView);

				List<CompoundedAttributeTypeMemberEntity> members = compoundedAttributeType
						.getMembers();
				/*
				 * Remember that the members are in-order.
				 */
				for (CompoundedAttributeTypeMemberEntity member : members) {
					AttributeTypeEntity memberAttributeType = member
							.getMember();

					String memberHumanReadableName = null;
					String memberDescription = null;
					if (null != language) {
						AttributeTypeDescriptionEntity attributeTypeDescription = this.attributeTypeDAO
								.findDescription(new AttributeTypeDescriptionPK(
										memberAttributeType.getName(), language));
						if (null != attributeTypeDescription) {
							LOG.debug("found description");
							memberHumanReadableName = attributeTypeDescription
									.getName();
							memberDescription = attributeTypeDescription
									.getDescription();
						}
					}

					AttributeDO attributeView = new AttributeDO(
							memberAttributeType.getName(), memberAttributeType
									.getType(), memberAttributeType
									.isMultivalued(), idx,
							memberHumanReadableName, memberDescription, false,
							false, null, null);
					/*
					 * We mark compounded attribute members as non-editable when
					 * queries via the listAttributes method to ease
					 * visualization.
					 */
					Map<Long, AttributeEntity> attributeValues = membersMap
							.get(memberAttributeType);
					attributeView.setMember(true);
					AttributeEntity attributeValue = null;
					if (null != attributeValues) {
						attributeValue = attributeValues.get(idx);
					}
					if (null != attributeValue) {
						attributeView.setValue(attributeValue);
					}
					attributesView.add(attributeView);
				}
			}
		}

		return attributesView;
	}

}
