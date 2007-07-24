/*
 * SafeOnline project.
 * 
 * Copyright 2006-2007 Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

package net.link.safeonline.taglib;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import net.link.safeonline.authentication.service.AttributeDO;
import net.link.safeonline.entity.DatatypeType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JSF validator for {@link AttributeDO}.
 * 
 * @author fcorneli
 * 
 */
public class AttributeValidator implements Validator {

	private static final Log LOG = LogFactory.getLog(AttributeValidator.class);

	public static final String VALIDATOR_ID = "net.link.validator.attribute";

	public void validate(FacesContext context, UIComponent component,
			Object value) throws ValidatorException {
		UIInput inputComponent = (UIInput) component;
		boolean required = inputComponent.isRequired();
		if (false == required) {
			return;
		}
		AttributeDO attribute = (AttributeDO) value;
		if (false == attribute.isRequired()) {
			/*
			 * In case of compounded member attributes the attribute can be
			 * optional.
			 */
			return;
		}
		DatatypeType type = attribute.getType();
		TypeValidator typeValidator = typeValidators.get(type);
		if (null == typeValidator) {
			FacesMessage facesMessage = new FacesMessage("unsupported type: "
					+ type);
			LOG.error("unsupported type: " + type);
			throw new ValidatorException(facesMessage);
		}
		typeValidator.validate(context, attribute);
	}

	private interface TypeValidator {

		void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException;
	}

	@SupportedType(DatatypeType.STRING)
	public static class StringTypeValidator implements TypeValidator {

		public void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException {
			String value = attribute.getStringValue();
			if (null == value) {
				FacesMessage facesMessage = new FacesMessage(
						"string value is null");
				throw new ValidatorException(facesMessage);
			}
			if (0 == value.length()) {
				FacesMessage facesMessage = new FacesMessage(
						"string value is empty");
				throw new ValidatorException(facesMessage);
			}
		}
	}

	@SupportedType(DatatypeType.BOOLEAN)
	public static class BooleanTypeValidator implements TypeValidator {

		public void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException {
			Boolean value = attribute.getBooleanValue();
			if (null == value) {
				FacesMessage facesMessage = new FacesMessage(
						"boolean value is null");
				throw new ValidatorException(facesMessage);
			}
		}
	}

	@SupportedType(DatatypeType.INTEGER)
	public static class IntegerTypeValidator implements TypeValidator {

		public void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException {
			Integer value = attribute.getIntegerValue();
			if (null == value) {
				String msg = "integer value is null";
				LOG.debug(msg);
				FacesMessage facesMessage = new FacesMessage(msg);
				throw new ValidatorException(facesMessage);
			}
		}
	}

	@SupportedType(DatatypeType.DOUBLE)
	public static class DoubleTypeValidator implements TypeValidator {

		public void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException {
			Double value = attribute.getDoubleValue();
			if (null == value) {
				String msg = "double value is null";
				LOG.debug(msg);
				FacesMessage facesMessage = new FacesMessage(msg);
				throw new ValidatorException(facesMessage);
			}
		}
	}

	@SupportedType(DatatypeType.DATE)
	public static class DateTypeValidator implements TypeValidator {

		public void validate(FacesContext context, AttributeDO attribute)
				throws ValidatorException {
			Date value = attribute.getDateValue();
			if (null == value) {
				String msg = "date value is null";
				LOG.debug(msg);
				FacesMessage facesMessage = new FacesMessage(msg);
				throw new ValidatorException(facesMessage);
			}
		}
	}

	private static final Map<DatatypeType, TypeValidator> typeValidators = new HashMap<DatatypeType, TypeValidator>();

	static {
		registerTypeValidator(StringTypeValidator.class);
		registerTypeValidator(BooleanTypeValidator.class);
		registerTypeValidator(IntegerTypeValidator.class);
		registerTypeValidator(DoubleTypeValidator.class);
		registerTypeValidator(DateTypeValidator.class);
	}

	private static void registerTypeValidator(
			Class<? extends TypeValidator> clazz) {
		SupportedType supportedTypeAnnotation = clazz
				.getAnnotation(SupportedType.class);
		if (null == supportedTypeAnnotation) {
			throw new RuntimeException(
					"@SupportedType annotation required on class: "
							+ clazz.getName());
		}
		DatatypeType type = supportedTypeAnnotation.value();
		if (typeValidators.containsKey(type)) {
			throw new RuntimeException(
					"already registered an validator for type: " + type);
		}

		TypeValidator typeValidator;
		try {
			typeValidator = clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("error instantiating: "
					+ clazz.getName());
		}
		typeValidators.put(type, typeValidator);
	}
}
