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
package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.activiti.TestAuthenticationInfoHolder;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.processes.WorkflowResult;
import com.evolveum.midpoint.wf.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.util.JaxbValueContainer;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.WfVariablesUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
//@DependsOn("workflowServiceImpl")
public class TestGeneralChangeProcessor extends AbstractInternalModelIntegrationTest {

    protected static final Trace LOGGER = TraceManager.getTrace(TestGeneralChangeProcessor.class);

    private static final String TEST_RESOURCE_DIR_NAME = "src/test/resources";
    private static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1 = new File(TEST_RESOURCE_DIR_NAME, "user-jack-modify-add-assignment-role1.xml");
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role2-change-gn.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-role3-change-gn2.xml";
    private static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-roles2-3-4.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-disable.xml";
    private static final String REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-activation-enable.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password.xml";
    private static final String REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2 = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-change-password-2.xml";

    private static final String DONT_CHECK = "dont-check";

    @Autowired(required = true)
	private Clockwork clockwork;

	@Autowired(required = true)
	private TaskManager taskManager;

    @Autowired(required = true)
    private WorkflowManagerImpl workflowServiceImpl;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private MiscDataUtil miscDataUtil;

    @Autowired
    private PrimaryChangeProcessor primaryChangeProcessor;

    @Autowired
    private GeneralChangeProcessor generalChangeProcessor;

    private ActivitiUtil activitiUtil = new ActivitiUtil();                 // this is not a spring bean

    public TestGeneralChangeProcessor() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
        importObjectFromFile(TestConstants.USERS_AND_ROLES_FILENAME, initResult);

	}

	@Test(enabled = false)
    public void test010AddRole1() throws Exception {
        TestUtil.displayTestTile(this, "test010UserModifyAddRole");
       	executeTest("test010UserModifyAddRole", USER_JACK_OID, 1, false, true, new ContextCreator() {
               @Override
               public LensContext createModelContext(OperationResult result) throws Exception {
                   LensContext<UserType> context = createUserAccountContext();
                   fillContextWithUser(context, USER_JACK_OID, result);
                   addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                   return context;
               }
           });
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
                return context;
            }

            @Override
            void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception {
                assertEquals("Unexpected state of the context", ModelState.PRIMARY, context.getState());
            }

            @Override
            void completeWorkItem(WorkItemType workItem, String taskId, OperationResult result) throws Exception {
                PrismObject<? extends ObjectType> workItemObject = workItem.getRequestSpecificData().asPrismObject();
                LOGGER.trace("workItemObject = " + workItemObject.debugDump());

                // change role1 -> role2
                final int N = 6;
                StringBuilder ctx = new StringBuilder();

                for (int ctxIndex = 0; ctxIndex < N; ctxIndex++) {
                    QName contextQName = new QName(SchemaConstants.NS_WFCF, "modelContextToBeEdited" + ctxIndex);
                    PrismProperty<String> contextProperty = workItemObject.findProperty(contextQName);
                    assertNotNull(contextQName + " not found among workItem specific properties", contextProperty);
                    ctx.append(contextProperty.getRealValue());
                }

                String newCtx = ctx.toString().replaceAll("00000001-d34d-b33f-f00d-000000000001", "00000001-d34d-b33f-f00d-000000000002");
                for (int ctxIndex = 0; ctxIndex < N; ctxIndex++) {
                    QName contextQName = new QName(SchemaConstants.NS_WFCF, "modelContextToBeEdited" + ctxIndex);
                    PrismProperty<String> contextProperty = workItemObject.findProperty(contextQName);
                    contextProperty.replace(new PrismPropertyValue<String>(JaxbValueContainer.getChunk(newCtx, ctxIndex)));
                }

                TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
                workflowServiceImpl.completeWorkItemWithDetails(taskId, workItemObject, "approve", result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                assertAssignedRole(USER_JACK_OID, TestConstants.ROLE_R2_OID, task, result);
                checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R1_OID, WorkflowResult.APPROVED));
                //checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R1BOSS_OID), result);
            }
        });
	}

    @Test(enabled = true)
    public void test020AddAccountRejected() throws Exception {
        TestUtil.displayTestTile(this, "test020AddAccountRejected");

        primaryChangeProcessor.setEnabled(false);
        generalChangeProcessor.setEnabled(true);
        generalChangeProcessor.disableScenario("primaryScenario");
        generalChangeProcessor.enableScenario("secondaryScenario");

        executeTest("test020AddAccountRejected", USER_JACK_OID, 1, false, true, new ContextCreator() {
            @Override
            public LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<UserType, ShadowType> context = createUserAccountContext();
                fillContextWithUser(context, USER_JACK_OID, result);
                addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILENAME);
                return context;
            }

            @Override
            void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception {
                assertEquals("Unexpected state of the context", ModelState.SECONDARY, context.getState());
            }

            @Override
            void completeWorkItem(WorkItemType workItem, String taskId, OperationResult result) throws Exception {
                PrismObject<? extends ObjectType> workItemObject = workItem.getRequestSpecificData().asPrismObject();
                display("workItemObject", workItemObject);

                WfProcessInstanceType instance = workflowServiceImpl.getProcessInstanceById(workItem.getProcessInstanceId(), false, true, result);
                JaxbValueContainer<ObjectDeltaType> deltaTypeWrapped = WfVariablesUtil.getVariable(instance, "greenDelta", JaxbValueContainer.class);
                activitiUtil.revive(deltaTypeWrapped);
                ObjectDeltaType deltaType = deltaTypeWrapped.getValue();
                display("greenDelta", DeltaConvertor.createObjectDelta(deltaType, prismContext));

                PrismPropertyDefinition ppd = new PrismPropertyDefinition(new QName(SchemaConstants.NS_WFCF, "[Button]rejectAll"),
                        new QName(SchemaConstants.NS_WFCF, "[Button]rejectAll"), DOMUtil.XSD_BOOLEAN, prismContext);
                PrismProperty<Boolean> rejectAll = ppd.instantiate();
                rejectAll.setRealValue(Boolean.TRUE);
                workItemObject.addReplaceExisting(rejectAll);

                TestAuthenticationInfoHolder.setUserType(getUser(USER_ADMINISTRATOR_OID).asObjectable());
                workflowServiceImpl.completeWorkItemWithDetails(taskId, workItemObject, "approve", result);
            }

            @Override
            void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception {
                PrismObject<UserType> jack = getUser(USER_JACK_OID);
                assertAssignedRole(USER_JACK_OID, TestConstants.ROLE_R2_OID, task, result);
                assertNoLinkedAccount(jack);
                //checkDummyTransportMessages("simpleUserNotifier", 1);
                //checkWorkItemAuditRecords(createResultMap(TestConstants.ROLE_R1_OID, WorkflowResult.APPROVED));
                //checkUserApprovers(USER_JACK_OID, Arrays.asList(TestConstants.R1BOSS_OID), result);
            }


        });
    }


    private abstract class ContextCreator {
        LensContext createModelContext(OperationResult result) throws Exception { return null; }
        void assertsAfterClockworkRun(ModelContext context, Task task, OperationResult result) throws Exception { }
        void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception { }
        void assertsRootTaskFinishes(Task task, OperationResult result) throws Exception { }
        String getObjectOid(Task task, OperationResult result) throws SchemaException { return null; };

        abstract void completeWorkItem(WorkItemType workItem, String taskId, OperationResult result) throws Exception;
    }

	private void executeTest(String testName, String oid, int subtaskCount, boolean immediate, boolean checkObjectOnSubtasks, ContextCreator contextCreator) throws Exception {

        int workflowSubtaskCount = immediate ? subtaskCount-1 : subtaskCount;

		// GIVEN
        prepareNotifications();
        dummyAuditService.clear();

        OperationResult result = new OperationResult("execution");

        Task modelTask = taskManager.createTaskInstance(TestGeneralChangeProcessor.class.getName() + "."+testName);
        display("Model task after creation", modelTask);

        LensContext<UserType> context = (LensContext<UserType>) contextCreator.createModelContext(result);
        modelTask.setOwner(repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, result));

        LensContext<UserType, ShadowType> context = (LensContext<UserType, ShadowType>) contextCreator.createModelContext(result);
        display("Input context", context);

        assertFocusModificationSanity(context);
        assertUserModificationSanity(context);

        // WHEN

       	HookOperationMode mode = clockwork.run(context, modelTask, result);

        // THEN

        contextCreator.assertsAfterClockworkRun(context, modelTask, result);
        assertEquals("Wrong mode after clockwork.run in " + context.getState(), HookOperationMode.BACKGROUND, mode);
        modelTask.refresh(result);
        display("Model task after clockwork runs", modelTask);

        Task rootTask = taskManager.getTask(wfTaskUtil.getRootTaskOid(modelTask), result);
        display("Workflow root task created by clockwork run", rootTask);

        assertTrue("Workflow root task is not persistent", rootTask.isPersistent());
        assertEquals("Invalid current handler", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI, rootTask.getHandlerUri());

        ModelContext taskModelContext = immediate ? null : wfTaskUtil.retrieveModelContext(rootTask, result);
        assertNotNull("Model context is not present in root task", taskModelContext);

        List<Task> subtasks = rootTask.listSubtasks(result);
        assertEquals("Incorrect number of subtasks", subtaskCount, subtasks.size());

        for (int subtaskIndex = 0; subtaskIndex < subtasks.size(); subtaskIndex++) {
            Task subtask = subtasks.get(subtaskIndex);

            // now check the workflow state
            String pid = wfTaskUtil.getProcessId(subtask);
            assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

            WfProcessInstanceType processInstance = workflowServiceImpl.getProcessInstanceById(pid, false, true, result);
            assertNotNull("Process instance information cannot be retrieved", processInstance);
            assertEquals("Incorrect number of work items", 1, processInstance.getWorkItems().size());

            String taskId = processInstance.getWorkItems().get(0).getWorkItemId();
            WorkItemType workItem = workflowServiceImpl.getWorkItemDetailsById(taskId, result);

            org.activiti.engine.task.Task t = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
            assertNotNull("activiti task not found", t);

            String executionId = t.getExecutionId();
            LOGGER.trace("Task id = " + taskId + ", execution id = " + executionId);

            contextCreator.completeWorkItem(workItem, taskId, result);
        }

        waitForTaskClose(rootTask, 60000);
        contextCreator.assertsRootTaskFinishes(rootTask, result);
    }

    protected void waitForTaskClose(final Task task, final int timeout) throws Exception {
        final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class+".waitForTaskClose");
        Checker checker = new Checker() {
            @Override
            public boolean check() throws Exception {
                task.refresh(waitResult);
                OperationResult result = task.getResult();
                if (verbose) display("Check result", result);
                return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
            }
            @Override
            public void timeout() {
                try {
                    task.refresh(waitResult);
                } catch (ObjectNotFoundException e) {
                    LOGGER.error("Exception during task refresh: {}", e,e);
                } catch (SchemaException e) {
                    LOGGER.error("Exception during task refresh: {}", e,e);
                }
                OperationResult result = task.getResult();
                LOGGER.debug("Result of timed-out task:\n{}", result.dump());
                assert false : "Timeout ("+timeout+") while waiting for "+task+" to finish. Last result "+result;
            }
        };
        IntegrationTestTools.waitFor("Waiting for " + task + " finish", checker, timeout, 1000);
    }

}