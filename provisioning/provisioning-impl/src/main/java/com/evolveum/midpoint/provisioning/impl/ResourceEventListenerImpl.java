/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
//import com.evolveum.midpoint.model.ModelWebService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.impl.ShadowCacheFactory.Mode;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ResourceEventListenerImpl implements ResourceEventListener {

	
	private static final Trace LOGGER = TraceManager.getTrace(ResourceEventListenerImpl.class);
	
	@Autowired(required = true)
	private ShadowCacheFactory shadowCacheFactory;
	
	@Autowired(required = true)
	private ProvisioningContextFactory provisioningContextFactory;
	
	@Autowired
	private ChangeNotificationDispatcher notificationManager;
	
	@PostConstruct
	public void registerForResourceObjectChangeNotifications() {
		notificationManager.registerNotificationListener(this);
	}

	@PreDestroy
	public void unregisterForResourceObjectChangeNotifications() {
		notificationManager.unregisterNotificationListener(this);
	}
	
	private ShadowCache getShadowCache(ShadowCacheFactory.Mode mode){
		return shadowCacheFactory.getShadowCache(mode);
	}
	
	
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult parentResult) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectNotFoundException, GenericConnectorException, ObjectAlreadyExistsException {
		
		Validate.notNull(eventDescription, "Event description must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null");
		
		LOGGER.trace("Received event notification with the description: {}", eventDescription.debugDump());
		
		if (eventDescription.getCurrentShadow() == null && eventDescription.getDelta() == null){
			throw new IllegalStateException("Neither current shadow, nor delta specified. It is required to have at least one of them specified.");
		}
		
		applyDefinitions(eventDescription, parentResult);
		
		PrismObject<ShadowType> shadow = null;
		
		if (eventDescription.getCurrentShadow() != null){
			shadow = eventDescription.getCurrentShadow();
		} else if (eventDescription.getOldShadow() != null){
			shadow = eventDescription.getOldShadow();
		} else if (eventDescription.getDelta() != null && eventDescription.getDelta().isAdd()){
			if (eventDescription.getDelta().getObjectToAdd() == null){
				throw new IllegalStateException("Found ADD delta, but no object to add was specified.");
			}
			shadow = eventDescription.getDelta().getObjectToAdd();
		} else{
			
			throw new IllegalStateException("Resource event description does not contain neither old shadow, nor current shadow, nor shadow in delta");
		}
		
	
		ShadowCache shadowCache = getShadowCache(Mode.STANDARD);
		
		ProvisioningContext ctx = provisioningContextFactory.create(shadow, task, parentResult);
		ctx.assertDefinition();
		
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
		
		Change<ShadowType> change = new Change<ShadowType>(identifiers, eventDescription.getCurrentShadow(), eventDescription.getOldShadow(), eventDescription.getDelta());
		ObjectClassComplexTypeDefinition objectClassDefinition = ShadowUtil.getObjectClassDefinition(shadow);
		change.setObjectClassDefinition(objectClassDefinition);
		
		ShadowType shadowType = shadow.asObjectable();
		
		LOGGER.trace("Start to precess change: {}", change.toString());
		shadowCache.processChange(ctx, change, parentResult);
		
		LOGGER.trace("Change after processing {} . Start synchronizing.", change.toString());
		shadowCache.processSynchronization(ctx, change, parentResult);
	
	}

	private void applyDefinitions(ResourceEventDescription eventDescription,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		ShadowCache shadowCache = getShadowCache(Mode.STANDARD);
		if (eventDescription.getCurrentShadow() != null){
			shadowCache.applyDefinition(eventDescription.getCurrentShadow(), parentResult);
		}
		
		if (eventDescription.getOldShadow() != null){
			shadowCache.applyDefinition(eventDescription.getOldShadow(), parentResult);
		}
		
		if (eventDescription.getDelta() != null){
			shadowCache.applyDefinition(eventDescription.getDelta(), null, parentResult);
		}
	}

}
