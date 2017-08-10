package pingis.services;

import org.springframework.stereotype.Service;
import pingis.utils.EditorTabData;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import pingis.entities.Challenge;
import pingis.entities.ImplementationType;
import pingis.entities.TaskInstance;
import pingis.utils.JavaClassGenerator;

@Service
public class EditorService {
    
    @Autowired
    TaskImplementationService taskImplementationService;
    
    public EditorService() {
    }

    public Map<String, EditorTabData> generateEditorContents(TaskInstance taskInstance) {
        Map<String, EditorTabData> tabData;
        Challenge currentChallenge = taskInstance.getTask().getChallenge();
        if (taskInstance.getTask().getType().equals(ImplementationType.TEST)) {
            tabData = this.generateTestTaskTabs(taskInstance, currentChallenge);
        } else {
            tabData = this.generateImplTaskTabs(taskInstance, currentChallenge);
        }
        return tabData;
    }

    private Map<String, EditorTabData> generateTestTaskTabs(TaskInstance taskInstance,
            Challenge currentChallenge) {
        Map<String, EditorTabData> tabData = new LinkedHashMap();
        TaskInstance implTaskInstance
                = taskImplementationService.getCorrespondingImplTaskInstance(taskInstance);
        EditorTabData tab1 = new EditorTabData(
                JavaClassGenerator.generateTestClassFilename(currentChallenge),
                taskInstance.getTask().getCodeStub());
        EditorTabData tab2 = new EditorTabData(
                JavaClassGenerator.generateImplClassFilename(currentChallenge),
                implTaskInstance.getTask().getCodeStub());
        tabData.put("editor1", tab1);
        tabData.put("editor2", tab2);
        return tabData;
    }

    private Map<String, EditorTabData> generateImplTaskTabs(TaskInstance taskInstance,
            Challenge currentChallenge) {
        Map<String, EditorTabData> tabData = new LinkedHashMap();
        TaskInstance testTaskInstance
                = taskImplementationService.getCorrespondingTestTaskInstance(
                taskInstance);
        EditorTabData tab2 = new EditorTabData(
                "Implement code here",
                taskInstance.getTask().getCodeStub());
        EditorTabData tab1 = new EditorTabData("Test to fulfill",
                testTaskInstance
                        .getCode());
        tabData.put("editor2", tab1);
        tabData.put("editor1", tab2);
        return tabData;
    }

}
