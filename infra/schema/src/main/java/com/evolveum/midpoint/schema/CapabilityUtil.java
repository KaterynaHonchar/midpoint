/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationEnableDisableCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.PasswordCapabilityType;

/**
 * @author semancik
 *
 */
public class CapabilityUtil {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> T getCapability(Collection<Object> capabilities, Class<T> capabilityClass) {
		if (capabilities == null) {
			return null;
		}
		for (Object cap : capabilities) {
			if (cap instanceof JAXBElement) {
				JAXBElement jaxe = (JAXBElement) cap;
				if (capabilityClass.isAssignableFrom(jaxe.getDeclaredType())) {
					return (T) jaxe.getValue();
				}
			} else if (capabilityClass.isAssignableFrom(cap.getClass())) {
				return (T) cap;
			}
		}
		return null;
	}

	public static boolean isCapabilityEnabled(Object capability) throws SchemaException {
		if (capability == null) {
			return false;
		}
		if (capability instanceof JAXBElement<?>) {
			capability = ((JAXBElement<?>)capability).getValue();
		}
		
		if (capability instanceof CapabilityType) {
			return CapabilityUtil.isCapabilityEnabled((CapabilityType)capability);
		} else if (capability instanceof Element) {
			return CapabilityUtil.isCapabilityEnabled((Element)capability);
		} else {
			throw new IllegalArgumentException("Unexpected capability type "+capability.getClass());
		}
	}

	public static boolean isCapabilityEnabled(Element capability) throws SchemaException {
		if (capability == null) {
			return false;
		}
		ObjectFactory capabilitiesObjectFactory = new ObjectFactory();
		QName enabledElementName = capabilitiesObjectFactory.createEnabled(true).getName();
		Element enabledElement = DOMUtil.getChildElement(capability, enabledElementName);
		if (enabledElement == null) {
			return true;
		}
		Boolean enabled = XmlTypeConverter.convertValueElementAsScalar(enabledElement, Boolean.class);
		if (enabled == null) {
			return true;
		}
		return enabled;
	}

	public static <T extends CapabilityType> boolean isCapabilityEnabled(T capability) {
		if (capability == null) {
			return false;
		}
		if (capability.isEnabled() == null) {
			return true;
		}
		return capability.isEnabled();
	}

	public static boolean containsCapabilityWithSameElementName(List<Object> capabilities, Object capability) {
		if (capabilities == null) {
			return false;
		}
		QName capabilityElementName = JAXBUtil.getElementQName(capability);
		for (Object cap: capabilities) {
			QName capElementName = JAXBUtil.getElementQName(cap);
			if (capabilityElementName.equals(capElementName)) {
				return true;
			}
		}
		return false;
	}

	public static String getCapabilityDisplayName(Object capability) {
		// TODO: look for schema annotation
		String className = null;
		if (capability instanceof JAXBElement) {
			className = ((JAXBElement) capability).getDeclaredType().getSimpleName();
		} else {
			className = capability.getClass().getSimpleName();
		}
		if (className.endsWith("CapabilityType")) {
			return className.substring(0, className.length() - "CapabilityType".length());
		}
		return className;
	}
	
	public static boolean isPasswordReturnedByDefault(CredentialsCapabilityType capability) {
		if (capability == null) {
			return false;
		}
		PasswordCapabilityType password = capability.getPassword();
		if (password == null) {
			return false;
		}
		if (password.isReturnedByDefault() == null) {
			return true;
		}
		return password.isReturnedByDefault();
	}
	
	public static boolean isEnabledReturnedByDefault(ActivationCapabilityType capability) {
		if (capability == null) {
			return false;
		}
		ActivationEnableDisableCapabilityType enaCap = capability.getEnableDisable();
		if (enaCap == null) {
			return false;
		}
		if (enaCap.isReturnedByDefault() == null) {
			return true;
		}
		return enaCap.isReturnedByDefault();
	}
}