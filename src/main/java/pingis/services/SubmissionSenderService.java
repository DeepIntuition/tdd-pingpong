package pingis.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pingis.config.SubmissionProperties;
import pingis.entities.tmc.TmcSubmission;
import pingis.entities.tmc.TmcSubmissionStatus;
import pingis.repositories.TmcSubmissionRepository;

import java.util.UUID;

@Service
public class SubmissionSenderService {
    private static final String SUBMISSION_FILENAME = "submission.tar";

    private RestTemplate restTemplate;

    private final SubmissionProperties submissionProperties;

    private final TmcSubmissionRepository submissionRepository;

    @Autowired
    public SubmissionSenderService(RestTemplateBuilder restTemplateBuilder, SubmissionProperties submissionProperties,
                                   TmcSubmissionRepository submissionRepository) {
        this.submissionProperties = submissionProperties;
        this.submissionRepository = submissionRepository;

        restTemplate = restTemplateBuilder
                .rootUri(submissionProperties.getSandboxUrl())
                .build();
    }

    private HttpEntity<MultiValueMap<String, Object>> buildRequestEntity(byte[] packaged, String notifyToken,
                                                                         String notifyUrl) {
        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();

        HttpHeaders textHeaders = new HttpHeaders();
        textHeaders.setContentType(MediaType.TEXT_PLAIN);

        HttpHeaders binaryHeaders = new HttpHeaders();
        binaryHeaders.setContentDispositionFormData("file", SUBMISSION_FILENAME);

        map.add("file", new HttpEntity<>(new ByteArrayResource(packaged), binaryHeaders));
        map.add("token", new HttpEntity<>(notifyToken, textHeaders));
        map.add("notify", new HttpEntity<>(notifyUrl, textHeaders));

        return new HttpEntity<>(map);
    }

    public TmcSubmission sendSubmission(TmcSubmission submission, byte[] packaged) {
        submission.setId(UUID.randomUUID());
        submission.setStatus(TmcSubmissionStatus.PENDING);

        String notifyUrl = submissionProperties.getNotifyUrl();

        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildRequestEntity(packaged,
                submission.getId().toString(), notifyUrl);

        TmcSubmissionResponse response = restTemplate.postForObject("/tasks.json", requestEntity,
                TmcSubmissionResponse.class);

        if (!response.getStatus().equals(TmcSubmissionResponse.OK)) {
            return null;
        }

        submissionRepository.save(submission);

        return submission;
    }
}
