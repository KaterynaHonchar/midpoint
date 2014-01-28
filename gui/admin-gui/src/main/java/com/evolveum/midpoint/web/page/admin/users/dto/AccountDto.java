/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.dto;

import java.io.Serializable;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.orgStruct.NodeDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author mserbak
 */
public class AccountDto<T extends ObjectType> implements Serializable {
	private PrismObject<T> prismAccount;
	private SynchronizationPolicyDecision syncPolicy;

	public AccountDto(PrismObject<T> account, SynchronizationPolicyDecision syncPolicy) {
		this.prismAccount = account;
		this.syncPolicy = syncPolicy;
	}

	public PrismObject<T> getPrismAccount() {
		return prismAccount;
	}

	public SynchronizationPolicyDecision getSyncPolicy() {
		return syncPolicy;
	}
}