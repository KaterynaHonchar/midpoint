/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.controller.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.web.bean.AppenderListItem;
import com.evolveum.midpoint.web.bean.AppenderType;
import com.evolveum.midpoint.web.bean.LoggerListItem;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("logging")
@Scope("session")
public class LoggingController implements Serializable {

	public static final String PAGE_NAVIGATION_LOGGING = "/config/logging?faces-redirect=true";
	private static final long serialVersionUID = -8739729766074013883L;
	@Autowired(required = true)
	private ModelPortType model;
	private List<LoggerListItem> loggers;
	private List<AppenderListItem> appenders;
	private boolean selectAllLoggers = false;
	private boolean selectAllAppenders = false;

	public List<SelectItem> getAppenderNames() {
		List<SelectItem> appenders = new ArrayList<SelectItem>();
		for (AppenderListItem item : getAppenders()) {
			appenders.add(new SelectItem(item.getName()));
		}

		Collections.sort(appenders, new SelectItemComparator());

		return appenders;
	}

	public List<SelectItem> getCategories() {
		List<SelectItem> categories = new ArrayList<SelectItem>();
		for (LoggingCategoryType type : LoggingCategoryType.values()) {
			categories.add(new SelectItem(type.value()));
		}

		Collections.sort(categories, new SelectItemComparator());

		return categories;
	}

	public List<SelectItem> getComponents() {
		List<SelectItem> components = new ArrayList<SelectItem>();
		for (LoggingComponentType type : LoggingComponentType.values()) {
			components.add(new SelectItem(type.value()));
		}

		Collections.sort(components, new SelectItemComparator());

		return components;
	}

	public List<SelectItem> getLevels() {
		List<SelectItem> levels = new ArrayList<SelectItem>();
		for (LoggingLevelType type : LoggingLevelType.values()) {
			levels.add(new SelectItem(type.value()));
		}

		Collections.sort(levels, new SelectItemComparator());

		return levels;
	}

	public List<SelectItem> getTypes() {
		List<SelectItem> types = new ArrayList<SelectItem>();
		for (AppenderType type : AppenderType.values()) {
			types.add(new SelectItem(type.getTitle(), FacesUtils.translateKey(type.getTitle())));
		}

		Collections.sort(types, new SelectItemComparator());

		return types;
	}

	public List<LoggerListItem> getLoggers() {
		if (loggers == null) {
			loggers = new ArrayList<LoggerListItem>();
		}
		return loggers;
	}

	public List<AppenderListItem> getAppenders() {
		if (appenders == null) {
			appenders = new ArrayList<AppenderListItem>();
		}
		return appenders;
	}

	public boolean isSelectAllAppenders() {
		return selectAllAppenders;
	}

	public boolean isSelectAllLoggers() {
		return selectAllLoggers;
	}

	public void setSelectAllAppenders(boolean selectAllAppenders) {
		this.selectAllAppenders = selectAllAppenders;
	}

	public void setSelectAllLoggers(boolean selectAllLoggers) {
		this.selectAllLoggers = selectAllLoggers;
	}

	public void selectLoggerPerformed(ValueChangeEvent evt) {
		this.selectAllLoggers = ControllerUtil.selectPerformed(evt, getLoggers());
	}

	public void selectAllLoggersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getLoggers());
	}

	public void selectAppenderPerformed(ValueChangeEvent evt) {
		this.selectAllAppenders = ControllerUtil.selectPerformed(evt, getAppenders());
	}

	public void selectAllAppendersPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getAppenders());
	}

	public void deleteLoggers() {
		List<LoggerListItem> items = new ArrayList<LoggerListItem>();
		for (LoggerListItem item : getLoggers()) {
			if (item.isSelected()) {
				items.add(item);
			}
		}

		getLoggers().removeAll(items);

		saveConfiguration();
	}

	public void deleteAppenders() {
		List<AppenderListItem> items = new ArrayList<AppenderListItem>();
		for (AppenderListItem item : getAppenders()) {
			if (item.isSelected()) {
				items.add(item);
			}
		}
		getAppenders().removeAll(items);

		saveConfiguration();
	}

	public String initController() {

		return PAGE_NAVIGATION_LOGGING;
	}

	void saveConfiguration() {
		// TODO: finish save operation
		LoggingConfigurationType configuration = createConfiguration(getLoggers(), getAppenders());
		try {
			SystemConfigurationType object = new SystemConfigurationType();
			object.setLogging(configuration);
			System.out.println(JAXBUtil.marshalWrap(object, new QName("configuration",
					"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd")));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// try {
		// ObjectModificationType change = null;
		// Holder<OperationResultType> holder = new
		// Holder<OperationResultType>(new OperationResultType());
		// model.modifyObject(change, holder);
		// } catch (FaultMessage ex) {
		//
		// }
	}

	private LoggingConfigurationType createConfiguration(List<LoggerListItem> loggers,
			List<AppenderListItem> appenders) {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		for (AppenderListItem item : appenders) {
			AppenderConfigurationType appender = createAppenderType(item);
			configuration.getAppender().add(appender);
		}
		for (LoggerListItem item : loggers) {
			LoggerConfigurationType logger = createLoggerType(item, configuration);
			configuration.getLogger().add(logger);
		}

		return configuration;
	}

	private AppenderConfigurationType createAppenderType(AppenderListItem item) {
		AppenderConfigurationType appender = null;
		switch (item.getType()) {
			case CONSOLE:
				appender = new AppenderConfigurationType();
				break;
			case FILE:
				FileAppenderConfigurationType fileAppender = new FileAppenderConfigurationType();
				fileAppender.setFilePath(item.getFilePath());
				fileAppender.setMaxFileSize(item.getMaxFileSize());

				appender = fileAppender;
				break;
		}

		appender.setName(item.getName());
		appender.setPattern(item.getPattern());

		return appender;
	}

	private LoggerConfigurationType createLoggerType(LoggerListItem item,
			LoggingConfigurationType configuration) {
		LoggerConfigurationType logger = new LoggerConfigurationType();
		for (String category : item.getCategories()) {
			logger.getCategory().add(LoggingCategoryType.fromValue(category));
		}
		for (String component : item.getComponents()) {
			logger.getComponent().add(LoggingComponentType.fromValue(component));
		}
		logger.getPackage().addAll(item.getPackages());
		logger.setLevel(item.getLevel());

		for (String appender : item.getAppenders()) {
			if (!containsAppender(appender, configuration.getAppender())) {
				continue;
			}
			logger.getAppender().add(appender);
		}

		return logger;
	}

	private boolean containsAppender(String name, List<AppenderConfigurationType> appenders) {
		for (AppenderConfigurationType appender : appenders) {
			if (appender.getName().equals(name)) {
				return true;
			}
		}

		return false;
	}
}
