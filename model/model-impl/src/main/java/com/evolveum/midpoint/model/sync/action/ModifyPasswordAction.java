/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import java.util.List;

/**
 * @author lazyman
 */
public class ModifyPasswordAction extends BaseAction {

    private static final Trace trace = TraceManager.getTrace(ModifyPasswordAction.class);

    @Override
    public String executeChanges(String userOid, ResourceObjectShadowChangeDescription change,
            SynchronizationSituationType situation,
            OperationResult result) throws SynchronizationException {
        ResourceObjectShadowType shadowAfterChange = change.getCurrentShadow();
        OperationResult subResult = result.createSubresult(ACTION_MODIFY_PASSWORD);

        UserType userType = getUser(userOid, subResult);
        if (userType == null) {
            throw new SynchronizationException("Can't find user with oid '" + userOid + "'.");
        }

//        if (!(change.getObjectChange() instanceof ObjectChangeModificationType)) {
//            throw new SynchronizationException("Object change is not instacne of "
//                    + ObjectChangeModificationType.class.getName());
//        }

        try {
            //TODO: create sync context and modify password
        } finally {
            subResult.computeStatus();
        }

        return userOid;
    }

    private PropertyModificationType getPasswordFromModification(ObjectChangeModificationType objectChange) {
        List<PropertyModificationType> list = objectChange.getObjectModification().getPropertyModification();
        for (PropertyModificationType propModification : list) {
            XPathHolder path = new XPathHolder(propModification.getPath());
            List<XPathSegment> segments = path.toSegments();
            if (segments.size() == 0 || !segments.get(0).getQName().equals(SchemaConstants.I_CREDENTIALS)) {
                continue;
            }

            PropertyModificationType.Value value = propModification.getValue();
            if (value == null) {
                continue;
            }
            List<Object> elements = value.getAny();
            for (Object element : elements) {
                if (SchemaConstants.I_PASSWORD.equals(JAXBUtil.getElementQName(element))) {
                    return propModification;
                }
            }
        }

        return null;
    }
}
