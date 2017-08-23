package pingis.controllers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import pingis.config.OAuthProperties;
import pingis.config.SecurityConfig;
import pingis.entities.Challenge;
import pingis.entities.Task;
import pingis.entities.TaskInstance;
import pingis.entities.TaskType;
import pingis.entities.User;
import pingis.entities.sandbox.Submission;
import pingis.services.ChallengeService;
import pingis.services.EditorService;
import pingis.services.TaskInstanceService;
import pingis.services.TaskService;
import pingis.services.UserService;
import pingis.services.sandbox.SandboxService;
import pingis.utils.EditorTabData;

@RunWith(SpringRunner.class)
@WebMvcTest(TaskController.class)
@ContextConfiguration(classes = {TaskController.class, SecurityConfig.class, OAuthProperties.class})
@WebAppConfiguration
public class TaskControllerTest {

  @Autowired
  WebApplicationContext context;

  private MockMvc mvc;

  @MockBean
  private SandboxService sandboxServiceMock;

  @MockBean
  private ChallengeService challengeServiceMock;

  @MockBean
  private TaskInstanceService taskInstanceServiceMock;

  @MockBean
  private TaskService taskServiceMock;

  @MockBean
  private EditorService editorServiceMock;

  @MockBean
  private UserService userServiceMock;

  @Captor
  private ArgumentCaptor<Map<String, byte[]>> packagingArgCaptor;

  private Challenge challenge;
  private Task testTask;
  private Task implementationTask;
  private TaskInstance testTaskInstance;
  private TaskInstance implTaskInstance;
  private User testUser;
  private Submission submission;

  @Before
  public void setUp() {
    testUser = new User(1, "TESTUSER", 1);
    challenge = new Challenge("Calculator", testUser,
        "Simple calculator");
    testTask = new Task(1,
        TaskType.TEST, testUser, "CalculatorAddition",
        "Implement addition", "return 1+1;", 1, 1);
    implementationTask = new Task(2,
        TaskType.IMPLEMENTATION, testUser, "implement addition",
        "implement addition", "public test", 1, 1);
    testTaskInstance
        = new TaskInstance(testUser, "", testTask);
    implTaskInstance = new TaskInstance(testUser, "",
        implementationTask);
    testTask.setChallenge(challenge);
    challenge.addTask(implementationTask);
    implementationTask.setChallenge(challenge);
    submission = new Submission();
    submission.setId(UUID.randomUUID());
    MockitoAnnotations.initMocks(this);

    this.mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .build();
  }


  @Test
  public void givenTaskWhenGetTestTask() throws Exception {
    when(taskInstanceServiceMock.findOne(testTaskInstance.getId()))
        .thenReturn(testTaskInstance);
    Map<String, EditorTabData> tabData = this
        .generateTabData(implTaskInstance, testTaskInstance);
    when(editorServiceMock.generateEditorContents(testTaskInstance)).thenReturn(tabData);
    String uri = "/task/" + testTaskInstance.getId();
    performSimpleGetRequestAndFindContent(uri, "task", testTask.getCodeStub());
    verify(taskInstanceServiceMock, times(1)).findOne(testTaskInstance.getId());
    verify(editorServiceMock, times(1))
        .generateEditorContents(testTaskInstance);
    verifyNoMoreInteractions(taskInstanceServiceMock);
    verifyNoMoreInteractions(editorServiceMock);
  }

  @Test
  public void givenTaskWhenGetImplementationTask() throws Exception {
    User testUser = new User(1, "TESTUSER", 1);

    when(taskInstanceServiceMock.findOne(implTaskInstance.getId()))
        .thenReturn(implTaskInstance);
    Map<String, EditorTabData> tabData = generateTabData(implTaskInstance, testTaskInstance);
    when(editorServiceMock.generateEditorContents(implTaskInstance)).thenReturn(tabData);
    String uri = "/task/" + implTaskInstance.getId();
    performSimpleGetRequestAndFindContent(uri, "task", implementationTask.getCodeStub());
    verify(taskInstanceServiceMock, times(1)).findOne(implTaskInstance.getId());
    verify(editorServiceMock, times(1))
        .generateEditorContents(implTaskInstance);
    verifyNoMoreInteractions(taskInstanceServiceMock);
    verifyNoMoreInteractions(editorServiceMock);
  }

  private Map<String, EditorTabData> generateTabData(TaskInstance implTaskInstance,
      TaskInstance testTaskInstance) {
    Map<String, EditorTabData> tabData = new LinkedHashMap<String, EditorTabData>();
    EditorTabData tab1 = new EditorTabData("Implement code here",
        implTaskInstance.getTask().getCodeStub());
    EditorTabData tab2 = new EditorTabData("Test to fulfill",
        testTaskInstance.getTask().getCodeStub());
    tabData.put("editor1", tab1);
    tabData.put("editor2", tab2);
    return tabData;
  }

  @Test
  public void submitTestTask() throws Exception {
    String submissionCode = "/* this is a test */";

    when(taskInstanceServiceMock.findOne(testTaskInstance.getId())).thenReturn(testTaskInstance);
    when(challengeServiceMock.findOne(challenge.getId())).thenReturn(challenge);
    when(taskServiceMock.findTaskInChallenge(challenge.getId(), implementationTask.getIndex()))
        .thenReturn(implementationTask);
    when(taskServiceMock.getCorrespondingTask(testTask)).thenReturn(implementationTask);
    when(sandboxServiceMock.submit(Mockito.any(), Mockito.any())).thenReturn(submission);
    mvc.perform(post("/task")
        .param("submissionCode", submissionCode)
        .param("taskInstanceId", Long.toString(implTaskInstance.getId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("/feedback*"));

    verify(taskInstanceServiceMock, times(1)).findOne(testTaskInstance.getId());
    verify(taskInstanceServiceMock).updateTaskInstanceCode(testTaskInstance.getId(),
        submissionCode);
    verify(taskServiceMock).getCorrespondingTask(testTask);
    verifyNoMoreInteractions(taskInstanceServiceMock);
    verifyNoMoreInteractions(challengeServiceMock);
    verifyNoMoreInteractions(taskServiceMock);
  }

  @Test
  public void submitImplementationTask() throws Exception {
    String submissionCode = "/* this is an implementation */";

    when(taskInstanceServiceMock.findOne(implTaskInstance.getId())).thenReturn(implTaskInstance);
    when(challengeServiceMock.findOne(challenge.getId())).thenReturn(challenge);
    when(taskServiceMock.findTaskInChallenge(challenge.getId(), testTask.getIndex()))
        .thenReturn(testTask);
    when(taskServiceMock.getCorrespondingTask(implementationTask)).thenReturn(testTask);
    when(sandboxServiceMock.submit(Mockito.any(), Mockito.any())).thenReturn(submission);
    mvc.perform(post("/task")
        .param("submissionCode", submissionCode)
        .param("taskInstanceId", Long.toString(implTaskInstance.getId())))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("/feedback*"));

    verify(taskInstanceServiceMock, times(1)).findOne(implTaskInstance.getId());
    verify(taskInstanceServiceMock).updateTaskInstanceCode(implTaskInstance.getId(),
        submissionCode);
    verify(taskServiceMock).getCorrespondingTask(implementationTask);
    verifyNoMoreInteractions(taskInstanceServiceMock);
    verifyNoMoreInteractions(challengeServiceMock);
    verifyNoMoreInteractions(taskServiceMock);
  }

  @Test
  public void givenErrorWhenGetNonExistentTask() throws Exception {
    when(taskInstanceServiceMock.findOne(123))
        .thenReturn(null);
    mvc.perform(get("/task/123"))
        .andExpect(status().isOk())
        .andExpect(view().name("error"));
    verify(taskInstanceServiceMock, times(1)).findOne(123);
    verifyNoMoreInteractions(taskInstanceServiceMock);
    verifyNoMoreInteractions(editorServiceMock);
  }

  private void performSimpleGetRequestAndFindContent(String uri,
      String viewName,
      String expectedContent) throws Exception {
    mvc.perform(get(uri))
        .andExpect(status().isOk())
        .andExpect(view().name(viewName))
        .andExpect(content().string(containsString(expectedContent)));
  }

  @Test
  public void nextTaskReturnsNextTaskView() throws Exception {
    Long challengeId = 678L;

    Challenge challenge = mock(Challenge.class);

    Iterator itr = mock(Iterator.class);
    when(itr.hasNext()).thenReturn(false);

    List testTasks = mock(List.class);
    when(testTasks.iterator()).thenReturn(itr);

    Set set = mock(Set.class);
    when(set.iterator()).thenReturn(itr);

    MultiValueMap implementationTaskInstances = mock(MultiValueMap.class);
    when(implementationTaskInstances.keySet()).thenReturn(set);

    when(challengeServiceMock.findOne(any())).thenReturn(mock(Challenge.class));
    when(taskServiceMock.filterTasksByUser(any(), any())).thenReturn(testTasks);
    when(taskServiceMock.getAvailableTestTaskInstances(any(), any()))
            .thenReturn(implementationTaskInstances);

    mvc.perform(get("/nextTask/" + challengeId))
            .andExpect(status().isOk())
            .andExpect(view().name("nexttask"))
            .andExpect(model().attributeExists("challenge"))
            .andExpect(model().attributeExists("testTasks"))
            .andExpect(model().attributeExists("implementationTaskInstances"));

    verify(challengeServiceMock).findOne(678L);
  }

  @Test
  public void newImplementationTaskInstanceRedirectsToTask() throws Exception {
    Long taskId = 123L;
    Long testTaskInstanceId = 555L;
    Long newTaskInstanceId = 743L;

    Task task = mock(Task.class);
    when(task.getType()).thenReturn(TaskType.IMPLEMENTATION);
    when(taskServiceMock.findOne(any())).thenReturn(task);

    TaskInstance newTaskInstance = mock(TaskInstance.class);
    when(newTaskInstance.getId()).thenReturn(newTaskInstanceId);
    when(taskInstanceServiceMock.createEmpty(any(), any())).thenReturn(newTaskInstance);

    TaskInstance testTaskInstance = mock(TaskInstance.class);
    when(taskInstanceServiceMock.findOne(anyLong())).thenReturn(testTaskInstance);

    mvc.perform(get("/newTaskInstance")
            .param("taskId", taskId.toString())
            .param("testTaskInstanceId", testTaskInstanceId.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/task/" + newTaskInstanceId))
            .andExpect(model().attribute("taskInstanceId", newTaskInstanceId.toString()));

    verify(taskServiceMock).findOne(taskId);
    verify(taskInstanceServiceMock).findOne(testTaskInstanceId);
    verify(taskInstanceServiceMock).save(newTaskInstance);
    verify(taskInstanceServiceMock).save(testTaskInstance);
  }

  @Test
  public void newTestTaskInstanceRedirectsToTask() throws Exception {
    Long taskId = 123L;
    Long testTaskInstanceId = 234L;
    Long newTaskInstanceId = 345L;

    Task task = mock(Task.class);
    when(task.getType()).thenReturn(TaskType.TEST);
    when(taskServiceMock.findOne(any())).thenReturn(task);

    TaskInstance newTaskInstance = mock(TaskInstance.class);
    when(newTaskInstance.getId()).thenReturn(newTaskInstanceId);
    when(taskInstanceServiceMock.createEmpty(any(), any())).thenReturn(newTaskInstance);

    mvc.perform(get("/newTaskInstance")
            .param("taskId", taskId.toString())
            .param("testTaskInstanceId", testTaskInstanceId.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/task/" + newTaskInstanceId))
            .andExpect(model().attribute("taskInstanceId", newTaskInstanceId.toString()));

    verify(taskServiceMock).findOne(taskId);
    verify(taskInstanceServiceMock, never()).save(any());
  }

  @Test
  public void randomTaskRedirectsToRandomTask() throws Exception {
    Long challengeId = 333L;

    Challenge challenge = mock(Challenge.class);
    when(challenge.getId()).thenReturn(challengeId);
    when(challengeServiceMock.getRandomChallenge()).thenReturn(challenge);

    mvc.perform(get("/randomTask"))
            .andExpect(status().is3xxRedirection())
            .andExpect(model().attribute("challengeId", challengeId.toString()))
            .andExpect(redirectedUrlPattern("/randomTask/?*"));
  }

  @Test
  public void randomTaskWithoutAvailableTaskRedirectsToUser() throws Exception {
    when(taskServiceMock.noNextTaskAvailable(any(), any())).thenReturn(true);

    mvc.perform(get("/randomTask/0"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user"));
  }

  @Test
  public void randomTaskWithTestTaskRedirectsToNewTaskInstance() throws Exception {
    Long challengeId = 999L;
    Long taskId = 237L;

    Task task = mock(Task.class);
    when(task.getId()).thenReturn(taskId);

    when(challengeServiceMock.findOne(any())).thenReturn(mock(Challenge.class));
    when(userServiceMock.getCurrentUser()).thenReturn(mock(User.class));
    when(taskServiceMock.noNextTaskAvailable(any(), any())).thenReturn(false);

    when(taskServiceMock.getRandomTaskType()).thenReturn(TaskType.TEST);
    when(taskServiceMock.hasNextTestTaskAvailable(any(), any())).thenReturn(true);

    when(taskServiceMock.getRandomTestTask(any(), any())).thenReturn(task);

    mvc.perform(get("/randomTask/0")
            .param("challengeId", challengeId.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/newTaskInstance?*"))
            .andExpect(model().attribute("taskId", taskId.toString()))
            .andExpect(model().attribute("testTaskInstanceId", "0"));

    verify(taskServiceMock).getRandomTestTask(any(), any());
  }

  @Test
  public void randomTaskWithImplementationTaskRedirectsToNewTaskInstance() throws Exception {
    Long challengeId = 998L;
    Long taskId = 337L;
    Long taskInstanceId = 222L;

    Task task = mock(Task.class);
    when(task.getId()).thenReturn(taskId);

    TaskInstance taskInstance = mock(TaskInstance.class);
    when(taskInstance.getId()).thenReturn(taskInstanceId);

    when(challengeServiceMock.findOne(any())).thenReturn(mock(Challenge.class));
    when(userServiceMock.getCurrentUser()).thenReturn(mock(User.class));
    when(taskServiceMock.noNextTaskAvailable(any(), any())).thenReturn(false);
    when(taskServiceMock.hasNextImplTaskAvailable(any(), any())).thenReturn(true);

    when(taskServiceMock.getRandomTaskType()).thenReturn(TaskType.IMPLEMENTATION);

    when(taskServiceMock.getRandomImplTask(any(), any())).thenReturn(task);
    when(taskServiceMock.getRandomTaskInstance(any(), any(), any())).thenReturn(taskInstance);

    mvc.perform(get("/randomTask/0")
            .param("challengeId", challengeId.toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/newTaskInstance?*"))
            .andExpect(model().attribute("taskId", taskId.toString()))
            .andExpect(model().attribute("testTaskInstanceId", taskInstanceId.toString()));

    verify(taskServiceMock).getRandomImplTask(any(), any());

  }
}
