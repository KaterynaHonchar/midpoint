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

package com.evolveum.midpoint.repo.sql.data.a1;

import javax.persistence.Embeddable;
import javax.persistence.UniqueConstraint;
import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 8:59 PM
 * To change this template use File | Settings | File Templates.
 */
@Embeddable
public class StringValue extends Value {

    private String value;

    public StringValue() {
    }

    public StringValue(String value) {
        this(null, null, value);
    }

    public StringValue(QName name, QName type, String value) {
        setName(name);
        setType(type);
        setValue(value);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
