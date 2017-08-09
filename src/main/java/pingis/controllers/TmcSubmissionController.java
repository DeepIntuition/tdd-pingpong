package pingis.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pingis.entities.tmc.TmcSubmission;
import pingis.entities.tmc.TmcSubmissionStatus;
import pingis.repositories.TmcSubmissionRepository;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import pingis.entities.tmc.TestOutput;

@Controller
public class TmcSubmissionController {
    @Autowired
    private TmcSubmissionRepository submissionRepository;
    @Autowired
    private SimpMessagingTemplate template;

    // These request parameters are specified separately because there doesn't seem to
    // be a simple way to rename fields when doing data binding.
    @PostMapping("/submission-result")
    public ResponseEntity submissionResult(
            @RequestParam("test_output") String testOutput,
            @RequestParam String stdout,
            @RequestParam String stderr,
            @RequestParam String validations,
            @RequestParam("vm_log") String vmLog,
            @RequestParam String token,
            @RequestParam String status,
            @RequestParam("exit_code") String exitCode) throws IOException {
        UUID submissionId = UUID.fromString(token);
        TmcSubmission submission = submissionRepository.findOne(submissionId);

        if (submission == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        if (submission.getStatus() != TmcSubmissionStatus.PENDING) {
            // Result is being submitted twice.
            // TODO: decide on a better response code for this...
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        int exitCodeValue = Integer.parseInt(exitCode.trim());
        ObjectMapper mapper = new ObjectMapper();
        
        submission.setExitCode(exitCodeValue);
        submission.setStatus(status);
        submission.setStderr(stderr);
        submission.setStdout(stdout);
        submission.setTestOutput(mapper.readValue(testOutput, TestOutput.class));
        submission.setValidations(validations);
        submission.setVmLog(vmLog);
        submissionRepository.save(submission);
        
        //Broadcasts the submission to /topic/results
        this.template.convertAndSend("/topic/results", submission.getTestOutput());
        return new ResponseEntity(HttpStatus.OK);
    }
}
